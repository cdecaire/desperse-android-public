package app.desperse.ui.components.media

import android.util.Log
import android.util.LruCache
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.desperse.ui.components.FaIcon
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.theme.DesperseSpacing
import com.google.android.filament.Skybox
import com.google.android.filament.utils.ModelViewer as FilamentModelViewer
import com.google.android.filament.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.desperse.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.net.URLEncoder
import java.nio.ByteBuffer

private const val TAG = "ModelViewer"

/** Ensure Filament native libraries are loaded exactly once. */
private object FilamentInitializer {
    var initialized = false
        private set

    @Synchronized
    fun ensureInitialized() {
        if (!initialized) {
            Utils.init()
            initialized = true
        }
    }
}

/**
 * In-memory LRU cache for downloaded GLB data.
 * Avoids re-downloading the same model when navigating back to detail view.
 * Max 50MB total cached data.
 */
private object GlbCache {
    private val cache = object : LruCache<String, ByteArray>(50 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    fun get(url: String): ByteArray? = cache.get(url)

    fun put(url: String, data: ByteArray) {
        cache.put(url, data)
    }

    fun remove(url: String) {
        cache.remove(url)
    }
}

/**
 * Interactive 3D model viewer using Google Filament.
 * Supports GLB files with orbit, zoom, and pan gestures.
 */
@Composable
fun ModelViewer(
    modelSource: ModelSource,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var filamentViewer by remember { mutableStateOf<FilamentModelViewer?>(null) }
    var choreographerCallback by remember { mutableStateOf<Choreographer.FrameCallback?>(null) }
    // Flag to stop the render loop — checked every frame before rendering
    val renderActive = remember { mutableStateOf(true) }

    // Pre-load native libs before composition triggers the AndroidView factory.
    // This is cheap after the first call (just a boolean check).
    FilamentInitializer.ensureInitialized()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).also { surfaceView ->
                    surfaceView.setZOrderMediaOverlay(true)
                    val viewer = FilamentModelViewer(surfaceView)

                    viewer.scene.skybox = Skybox.Builder()
                        .color(0.35f, 0.35f, 0.38f, 1.0f)
                        .build(viewer.engine)

                    setupDefaultLighting(viewer)
                    filamentViewer = viewer

                    // Start render loop — stops when renderActive is set to false
                    val choreographer = Choreographer.getInstance()
                    val frameCallback = object : Choreographer.FrameCallback {
                        override fun doFrame(frameTimeNanos: Long) {
                            if (!renderActive.value) return
                            choreographer.postFrameCallback(this)
                            viewer.render(frameTimeNanos)
                        }
                    }
                    choreographerCallback = frameCallback
                    choreographer.postFrameCallback(frameCallback)

                    surfaceView.setOnTouchListener { view, event ->
                        // Prevent parent LazyColumn from intercepting orbit/zoom gestures
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                view.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        viewer.onTouchEvent(event)
                        true
                    }

                    // Load model: fetch bytes on IO thread, feed to Filament on main
                    scope.launch {
                        try {
                            val buffer = withContext(Dispatchers.IO) {
                                when (modelSource) {
                                    is ModelSource.Url -> downloadModelCached(modelSource.url)
                                    is ModelSource.Stream -> readStream(modelSource.provider())
                                }
                            }

                            viewer.loadModelGlb(buffer)
                            if (viewer.asset != null) {
                                viewer.transformToUnitCube()
                                isLoading = false
                            } else {
                                loadError = "Unable to parse 3D model"
                                isLoading = false
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load 3D model", e)
                            loadError = "Failed to load 3D model"
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading indicator
        if (isLoading && loadError == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }

        // Error state
        if (loadError != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = loadError ?: "Failed to load model",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }

        // 3D badge (bottom-right — matches feed)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(DesperseSpacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                FaIcon(
                    icon = FaIcons.Cube,
                    size = 16.dp,
                    tint = Color.White,
                    style = FaIconStyle.Solid,
                    contentDescription = "3D model"
                )
            }
        }
    }

    // Clean up Filament resources safely
    DisposableEffect(Unit) {
        onDispose {
            // Stop render loop immediately — the flag is checked each frame
            renderActive.value = false
            choreographerCallback?.let { Choreographer.getInstance().removeFrameCallback(it) }
            choreographerCallback = null
            filamentViewer = null
        }
    }
}

/**
 * Source for 3D model data
 */
sealed class ModelSource {
    /** Load from a remote URL */
    data class Url(val url: String) : ModelSource()
    /** Load from an InputStream (e.g. content:// URI) */
    data class Stream(val provider: () -> InputStream) : ModelSource()
}

/**
 * Studio-style lighting: bright ambient IBL via spherical harmonics +
 * 3-point light rig (key, fill, rim) for well-lit PBR models.
 */
private fun setupDefaultLighting(viewer: FilamentModelViewer) {
    val engine = viewer.engine
    val em = com.google.android.filament.EntityManager.get()

    // Bright, uniform ambient — all 9 SH bands filled high
    val bands = floatArrayOf(
        2.0f, 1.9f, 1.85f,    // L00 — strong base ambient
        0.3f, 0.3f, 0.35f,    // L1-1 — bottom fill
        0.4f, 0.4f, 0.4f,     // L10 — front bias
        0.2f, 0.2f, 0.2f,     // L11 — side fill
        0.1f, 0.1f, 0.1f,     // L2-2
        0.05f, 0.05f, 0.05f,  // L2-1
        -0.1f, -0.1f, -0.1f,  // L20 — subtle vertical contrast
        0.05f, 0.05f, 0.05f,  // L21
        0.05f, 0.05f, 0.05f,  // L22
    )

    viewer.scene.indirectLight = com.google.android.filament.IndirectLight.Builder()
        .irradiance(3, bands)
        .intensity(75_000f)
        .build(engine)

    // Key light (upper-right front, bright)
    val keyLight = em.create()
    com.google.android.filament.LightManager.Builder(com.google.android.filament.LightManager.Type.DIRECTIONAL)
        .color(1.0f, 0.98f, 0.95f)
        .intensity(250_000f)
        .direction(-0.5f, -1.0f, -0.7f)
        .castShadows(true)
        .build(engine, keyLight)
    viewer.scene.addEntity(keyLight)

    // Fill light (strong, opposite side)
    val fillLight = em.create()
    com.google.android.filament.LightManager.Builder(com.google.android.filament.LightManager.Type.DIRECTIONAL)
        .color(0.95f, 0.95f, 1.0f)
        .intensity(150_000f)
        .direction(0.6f, -0.3f, -0.5f)
        .castShadows(false)
        .build(engine, fillLight)
    viewer.scene.addEntity(fillLight)

    // Rim/back light (edge highlights)
    val rimLight = em.create()
    com.google.android.filament.LightManager.Builder(com.google.android.filament.LightManager.Type.DIRECTIONAL)
        .color(1.0f, 1.0f, 1.0f)
        .intensity(100_000f)
        .direction(0.0f, -0.5f, 0.8f)
        .castShadows(false)
        .build(engine, rimLight)
    viewer.scene.addEntity(rimLight)

    // Bottom fill (prevents underside from going pure black)
    val bottomFill = em.create()
    com.google.android.filament.LightManager.Builder(com.google.android.filament.LightManager.Type.DIRECTIONAL)
        .color(0.9f, 0.9f, 0.95f)
        .intensity(60_000f)
        .direction(0.0f, 1.0f, -0.3f)
        .castShadows(false)
        .build(engine, bottomFill)
    viewer.scene.addEntity(bottomFill)
}

/** GLB magic bytes: "glTF" */
private fun isValidGlb(bytes: ByteArray): Boolean {
    return bytes.size >= 4 &&
        bytes[0] == 0x67.toByte() && // g
        bytes[1] == 0x6C.toByte() && // l
        bytes[2] == 0x54.toByte() && // T
        bytes[3] == 0x46.toByte()    // F
}

private val httpClient = OkHttpClient()

/** Build the proxy URL for a Vercel Blob asset.
 *  Vercel bot protection blocks direct programmatic access to blob URLs.
 *  Route through the server's proxy endpoint which authenticates server-side. */
private fun proxyUrl(blobUrl: String): String {
    val baseUrl = if (BuildConfig.DEBUG && BuildConfig.DEV_API_BASE_URL.isNotEmpty()) {
        BuildConfig.DEV_API_BASE_URL
    } else {
        BuildConfig.API_BASE_URL
    }
    val encoded = URLEncoder.encode(blobUrl, "UTF-8")
    return "${baseUrl.trimEnd('/')}/api/v1/media/proxy?url=$encoded"
}

/** Download a GLB file with in-memory caching via proxy endpoint. */
private fun downloadModelCached(url: String): ByteBuffer {
    GlbCache.get(url)?.let { cached ->
        if (isValidGlb(cached)) {
            return ByteBuffer.allocateDirect(cached.size).apply {
                put(cached)
                rewind()
            }
        } else {
            GlbCache.remove(url)
        }
    }

    // Route through server proxy to bypass Vercel bot protection on blob URLs
    val fetchUrl = if (url.contains("blob.vercel-storage.com")) proxyUrl(url) else url
    val request = Request.Builder().url(fetchUrl).build()
    val bytes = httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
        response.body!!.bytes()
    }

    if (isValidGlb(bytes)) {
        GlbCache.put(url, bytes)
    }

    return ByteBuffer.allocateDirect(bytes.size).apply {
        put(bytes)
        rewind()
    }
}

/** Read an InputStream into a ByteBuffer */
private fun readStream(input: InputStream): ByteBuffer {
    val bytes = input.use { it.readBytes() }
    return ByteBuffer.allocateDirect(bytes.size).apply {
        put(bytes)
        rewind()
    }
}

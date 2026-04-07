package app.desperse.ui.screens.create

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import app.desperse.ui.components.ButtonVariant
import app.desperse.ui.components.DesperseFaIconButton
import app.desperse.ui.components.DesperseTextButton
import app.desperse.ui.components.FaIconStyle
import app.desperse.ui.components.FaIcons
import app.desperse.ui.screens.create.components.AudioPickerSheet
import app.desperse.ui.screens.create.components.GalleryGrid
import app.desperse.ui.screens.create.components.MediaPreview
import app.desperse.ui.screens.create.components.MediaTypeSelector
import app.desperse.ui.theme.DesperseSpacing

/**
 * Step 1 of the create flow: media selection with tab-based media type picker.
 * Top half shows a preview of the selected media.
 * Bottom half shows a device gallery grid (Image/Video/Audio tabs) or file picker (3D tab).
 * A floating pill-shaped segment control at the bottom switches between media types.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSelectionScreen(
    viewModel: CreatePostViewModel,
    onClose: () -> Unit,
    onNext: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission launcher
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.onPermissionResult(allGranted)
    }

    // Audio picker sheet state
    var showAudioPicker by remember { mutableStateOf(false) }

    // 3D file picker — chooser dialog with cancel
    // We must copy the file to app cache immediately because content:// URI
    // permissions from ACTION_GET_CONTENT are temporary and the stream may
    // return garbage (e.g. HTML from cloud providers) if read later.
    val threeDFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            val fileName = resolveFileName(context, uri)
            val cachedUri = copyToCache(context, uri, fileName)
            if (cachedUri != null) {
                viewModel.addThreeDFile(cachedUri, fileName)
            }
        }
    }

    // Check/request permissions on launch
    LaunchedEffect(Unit) {
        if (!uiState.galleryPermissionGranted) {
            permissionLauncher.launch(permissions)
        }
    }

    // Next button enabled based on tab
    val nextEnabled = when (uiState.mediaTab) {
        MediaTab.Image -> uiState.selectedMediaItems.isNotEmpty()
        MediaTab.Video -> uiState.selectedMediaItems.size == 1
        MediaTab.Audio -> uiState.audioFile != null
        MediaTab.ThreeD -> uiState.threeDFile != null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    DesperseFaIconButton(
                        icon = FaIcons.Xmark,
                        onClick = onClose,
                        contentDescription = "Close",
                        style = FaIconStyle.Solid
                    )
                },
                title = {
                    Text(
                        text = "New post",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    DesperseTextButton(
                        text = "Next",
                        onClick = {
                            viewModel.onNextFromMediaSelect()
                            onNext()
                        },
                        variant = ButtonVariant.Ghost,
                        enabled = nextEnabled
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.galleryPermissionDenied -> {
                    PermissionDeniedState(
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        onRetry = { permissionLauncher.launch(permissions) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.galleryPermissionGranted -> {
                    // Preview area (~45%)
                    MediaPreview(
                        mediaTab = uiState.mediaTab,
                        selectedItems = uiState.selectedMediaItems,
                        audioFile = uiState.audioFile,
                        threeDFile = uiState.threeDFile,
                        onAddAudio = { showAudioPicker = true },
                        onRemoveAudio = { viewModel.removeAudioFile() },
                        onAddThreeD = {
                            threeDFilePicker.launch(
                                createFileChooser("Select 3D file", "application/octet-stream")
                            )
                        },
                        onRemoveThreeD = { viewModel.removeThreeDFile() },
                        modifier = Modifier.weight(0.45f)
                    )

                    // Gallery grid (~55%) with floating tab selector
                    Box(modifier = Modifier.weight(0.55f)) {
                        GalleryGrid(
                            items = uiState.galleryItems,
                            selectedItems = uiState.selectedMediaItems,
                            mediaTab = uiState.mediaTab,
                            onItemClick = { viewModel.toggleMediaSelection(it) },
                            onLoadMore = { viewModel.loadGalleryPage() },
                            modifier = Modifier.fillMaxSize()
                        )

                        MediaTypeSelector(
                            selectedTab = uiState.mediaTab,
                            onTabSelected = { viewModel.switchMediaTab(it) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = DesperseSpacing.lg)
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Requesting access to photos...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Audio picker sheet
    AudioPickerSheet(
        isOpen = showAudioPicker,
        onDismiss = { showAudioPicker = false },
        audioFiles = uiState.deviceAudioFiles,
        isLoading = uiState.isLoadingAudioFiles,
        onSelect = { viewModel.selectAudioFile(it) }
    )
}

@Composable
private fun PermissionDeniedState(
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(DesperseSpacing.xxl)
        ) {
            Text(
                text = "Photo access required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Allow access to your photos and videos to create posts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DesperseSpacing.sm, bottom = DesperseSpacing.lg)
            )

            DesperseTextButton(
                text = "Open Settings",
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            DesperseTextButton(
                text = "Try Again",
                onClick = onRetry,
                variant = ButtonVariant.Ghost,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(top = DesperseSpacing.sm)
            )
        }
    }
}

/** Create a chooser-wrapped intent for file picking. Always shows a cancel-able dialog. */
private fun createFileChooser(title: String, mimeType: String): Intent {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = mimeType
        addCategory(Intent.CATEGORY_OPENABLE)
        putExtra(Intent.EXTRA_LOCAL_ONLY, true)
    }
    return Intent.createChooser(intent, title)
}

/** Resolve display name from a content URI. */
private fun resolveFileName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) return cursor.getString(nameIndex)
            }
        }
    return uri.lastPathSegment ?: "file"
}

/**
 * Copy a content:// URI to app cache immediately.
 * Content provider permissions from ACTION_GET_CONTENT are temporary,
 * so we must read the bytes while the permission is still valid.
 */
private fun copyToCache(context: Context, uri: Uri, fileName: String): Uri? {
    return try {
        val cacheFile = java.io.File(context.cacheDir, "3d_${System.currentTimeMillis()}_$fileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(cacheFile)
    } catch (e: Exception) {
        android.util.Log.e("MediaSelection", "Failed to copy 3D file to cache", e)
        null
    }
}

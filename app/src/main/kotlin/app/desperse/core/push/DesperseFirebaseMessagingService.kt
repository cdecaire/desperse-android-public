package app.desperse.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import app.desperse.MainActivity
import app.desperse.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class DesperseFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushTokenManager: PushTokenManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "DesperseFirebaseMsgSvc"
        const val CHANNEL_ID = "desperse_notifications"
        const val EXTRA_FROM_NOTIFICATION = "from_notification"
        private var notificationId = 0
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: ${token.take(10)}...")
        // Save token for registration after auth completes.
        // Don't call registerToken() here - auth may not be ready yet.
        serviceScope.launch {
            pushTokenManager.savePendingToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received: data=${message.data}, notification=${message.notification?.title}")

        val data = message.data
        val notification = message.notification

        // Support both data-only (backend) and notification (Firebase campaigns) payloads
        val title = data["title"] ?: notification?.title ?: return
        val body = data["body"] ?: notification?.body ?: ""
        val deepLink = data["deepLink"] ?: notification?.link?.toString()
        val type = data["type"] ?: "notification"
        val avatarUrl = data["actorAvatarUrl"]
        val imageUrl = data["imageUrl"]

        serviceScope.launch {
            showNotification(title, body, deepLink, type, avatarUrl, imageUrl, message)
        }
    }

    private suspend fun showNotification(
        title: String,
        body: String,
        deepLink: String?,
        type: String,
        avatarUrl: String?,
        imageUrl: String?,
        originalMessage: RemoteMessage? = null
    ) {
        ensureNotificationChannel()

        // Download images on IO thread
        val avatarBitmap = avatarUrl?.let { downloadAndCircleCrop(it, 128) }
        val imageBitmap = imageUrl?.let { downloadBitmap(it) }

        val intent = if (deepLink != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), this, MainActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra(EXTRA_FROM_NOTIFICATION, true)

        // Copy FCM analytics extras so Firebase can auto-track notification opens.
        // Campaign notifications include google.c.a.* keys that Firebase Analytics reads.
        originalMessage?.toIntent()?.extras?.let { fcmExtras ->
            for (key in fcmExtras.keySet()) {
                if (key.startsWith("google.") || key.startsWith("gcm.") ||
                    key == "from" || key == "collapse_key"
                ) {
                    fcmExtras.getString(key)?.let { intent.putExtra(key, it) }
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Avatar as large icon (circular, like Instagram)
        if (avatarBitmap != null) {
            builder.setLargeIcon(avatarBitmap)
        }

        // Post image in expanded notification
        if (imageBitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(imageBitmap)
                    .bigLargeIcon(null as Bitmap?) // Hide large icon when expanded
            )
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId++, builder.build())
    }

    private suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            URL(url).openStream().use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download image: $url", e)
            null
        }
    }

    private suspend fun downloadAndCircleCrop(url: String, sizePx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val raw = URL(url).openStream().use { BitmapFactory.decodeStream(it) }
                    ?: return@withContext null
                val scaled = Bitmap.createScaledBitmap(raw, sizePx, sizePx, true)
                if (scaled !== raw) raw.recycle()

                val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val radius = sizePx / 2f
                canvas.drawCircle(radius, radius, radius, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(scaled, 0f, 0f, paint)
                scaled.recycle()
                output
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download avatar: $url", e)
                null
            }
        }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Desperse",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Desperse notifications"
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

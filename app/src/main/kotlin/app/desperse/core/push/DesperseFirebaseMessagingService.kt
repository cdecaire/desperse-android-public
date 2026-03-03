package app.desperse.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
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

        showNotification(title, body, deepLink, type, message)
    }

    private fun showNotification(
        title: String,
        body: String,
        deepLink: String?,
        type: String,
        originalMessage: RemoteMessage? = null
    ) {
        ensureNotificationChannel()

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

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId++, notification)
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

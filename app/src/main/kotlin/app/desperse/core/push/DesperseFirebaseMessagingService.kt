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
        Log.d(TAG, "FCM message received: ${message.data}")

        val data = message.data
        val title = data["title"] ?: return
        val body = data["body"] ?: ""
        val deepLink = data["deepLink"]
        val type = data["type"] ?: "notification"

        showNotification(title, body, deepLink, type)
    }

    private fun showNotification(title: String, body: String, deepLink: String?, type: String) {
        ensureNotificationChannel()

        val intent = if (deepLink != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), this, MainActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

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

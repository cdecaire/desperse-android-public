package app.desperse

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DesperseApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Sentry if DSN is configured
        if (BuildConfig.SENTRY_DSN.isNotBlank()) {
            try {
                io.sentry.android.core.SentryAndroid.init(this) { options ->
                    options.dsn = BuildConfig.SENTRY_DSN
                    options.isEnableAutoSessionTracking = true
                    options.environment = if (BuildConfig.DEBUG) "development" else "production"
                    options.tracesSampleRate = 0.1
                }
            } catch (e: Exception) {
                // Sentry init failed, continue without it
            }
        }

        // Create notification channel for push notifications
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "desperse_notifications",
                "Desperse",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Desperse notifications"
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

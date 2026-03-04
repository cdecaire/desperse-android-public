package app.desperse.core.di

import app.desperse.core.arweave.TurboPaymentApi
import app.desperse.core.arweave.TurboUploadApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ArweaveModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /** Unauthenticated OkHttp client for Turbo APIs (no AuthInterceptor) */
    @Provides
    @Singleton
    @Named("turbo")
    fun provideTurboOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (app.desperse.BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideTurboPaymentApi(
        @Named("turbo") okHttpClient: OkHttpClient
    ): TurboPaymentApi {
        return Retrofit.Builder()
            .baseUrl("https://payment.ardrive.io/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TurboPaymentApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTurboUploadApi(
        @Named("turbo") okHttpClient: OkHttpClient
    ): TurboUploadApi {
        return Retrofit.Builder()
            .baseUrl("https://upload.ardrive.io/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TurboUploadApi::class.java)
    }
}

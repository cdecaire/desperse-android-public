package app.desperse.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Auth module for Hilt.
 *
 * Note: PrivyAuthManager, TokenStorage, and AuthInterceptor use @Inject constructor,
 * so they're automatically provided by Hilt without explicit @Provides methods.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    // All auth-related classes use constructor injection (@Inject constructor)
    // so no explicit providers are needed here.
    // This module is kept for future bindings if needed.
}

package app.desperse.core.di

import app.desperse.core.wallet.MwaWalletSigner
import app.desperse.core.wallet.PrivyWalletSigner
import app.desperse.core.wallet.WalletSigner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WalletModule {
    @Provides
    @Singleton
    @Named("embedded")
    fun provideEmbeddedWalletSigner(signer: PrivyWalletSigner): WalletSigner = signer

    @Provides
    @Singleton
    @Named("external")
    fun provideExternalWalletSigner(signer: MwaWalletSigner): WalletSigner = signer
}

package app.desperse.ui.screens.wallet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.response.NFTAsset
import app.desperse.data.dto.response.TokenBalance
import app.desperse.data.dto.response.WalletActivityItem
import app.desperse.data.dto.response.WalletBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WalletViewModel"

data class WalletUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalUsd: Double = 0.0,
    val solPriceUsd: Double = 0.0,
    val solChangePct24h: Double = 0.0,
    val wallets: List<WalletBalance> = emptyList(),
    val tokens: List<TokenBalance> = emptyList(),
    val activity: List<WalletActivityItem> = emptyList(),
    val nfts: List<NFTAsset> = emptyList()
) {
    // Computed property for total change in USD (based on SOL holdings)
    val totalChangeUsd: Double
        get() {
            val solWallet = wallets.find { it.walletClientType == "privy" } ?: wallets.firstOrNull()
            val solValue = (solWallet?.sol ?: 0.0) * solPriceUsd
            return solValue * (solChangePct24h / 100.0)
        }

    val isPositiveChange: Boolean
        get() = totalChangeUsd >= 0
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val api: DesperseApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    fun loadWalletData() {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                when (val result = safeApiCall { api.getWalletOverview() }) {
                    is ApiResult.Success -> {
                        val data = result.data
                        Log.d(TAG, "Wallet data loaded: totalUsd=${data.totalUsd}, tokens=${data.tokens.size}, nfts=${data.nfts.size}, activity=${data.activity.size}")
                        data.tokens.forEach { t ->
                            Log.d(TAG, "  token: mint=${t.mint}, symbol=${t.symbol}, name=${t.name}, balance=${t.balance}, isAppToken=${t.isAppToken}")
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                totalUsd = data.totalUsd,
                                solPriceUsd = data.solPriceUsd,
                                solChangePct24h = data.solChangePct24h,
                                wallets = data.wallets,
                                tokens = data.tokens,
                                activity = data.activity,
                                nfts = data.nfts
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        Log.e(TAG, "Failed to load wallet: ${result.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading wallet", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load wallet"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadWalletData()
    }
}

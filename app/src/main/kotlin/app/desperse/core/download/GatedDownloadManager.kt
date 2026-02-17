package app.desperse.core.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import app.desperse.BuildConfig
import app.desperse.core.auth.PrivyAuthManager
import app.desperse.core.network.ApiResult
import app.desperse.core.network.DesperseApi
import app.desperse.core.network.safeApiCall
import app.desperse.data.dto.request.DownloadNonceRequest
import app.desperse.data.dto.request.DownloadVerifyRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages gated asset downloads with Privy wallet signature verification.
 *
 * Flow:
 * 1. Request nonce from server (POST /api/v1/downloads/nonce)
 * 2. Sign message with Privy embedded wallet
 * 3. Verify signature + ownership on server (POST /api/v1/downloads/verify)
 * 4. Open download URL with short-lived token
 */
@Singleton
class GatedDownloadManager @Inject constructor(
    private val api: DesperseApi,
    private val privyAuthManager: PrivyAuthManager,
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private const val TAG = "GatedDownloadManager"
    }

    sealed class DownloadResult {
        data object Success : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }

    /**
     * Download a gated asset using the signature verification flow.
     * Shows toast messages for progress/errors and opens the download URL on success.
     */
    suspend fun downloadGatedAsset(context: Context, assetId: String): DownloadResult {
        return withContext(Dispatchers.Main) {
            try {
                Toast.makeText(context, "Verifying ownership\u2026", Toast.LENGTH_SHORT).show()

                // Step 1: Get nonce from server
                val nonceResult = withContext(Dispatchers.IO) {
                    safeApiCall { api.getDownloadNonce(DownloadNonceRequest(assetId)) }
                }

                when (nonceResult) {
                    is ApiResult.Error -> {
                        val msg = nonceResult.message
                        Log.e(TAG, "Failed to get nonce: $msg")
                        Toast.makeText(context, "Download failed: $msg", Toast.LENGTH_SHORT).show()
                        return@withContext DownloadResult.Error(msg)
                    }
                    is ApiResult.Success -> { /* continue */ }
                }

                val nonceData = nonceResult.data
                val message = nonceData.message

                // Step 2: Sign message with Privy embedded wallet
                val signResult = withContext(Dispatchers.IO) {
                    privyAuthManager.signMessage(message)
                }

                val signature = signResult.getOrElse { error ->
                    val msg = error.message ?: "Failed to sign message"
                    Log.e(TAG, "Sign failed: $msg", error)
                    Toast.makeText(context, "Download failed: $msg", Toast.LENGTH_SHORT).show()
                    return@withContext DownloadResult.Error(msg)
                }

                // Step 3: Verify signature + ownership on server
                val verifyResult = withContext(Dispatchers.IO) {
                    safeApiCall {
                        api.verifyDownload(
                            DownloadVerifyRequest(
                                assetId = assetId,
                                signature = signature,
                                message = message
                            )
                        )
                    }
                }

                when (verifyResult) {
                    is ApiResult.Error -> {
                        val msg = verifyResult.message
                        Log.e(TAG, "Verify failed: $msg")
                        Toast.makeText(context, "Download failed: $msg", Toast.LENGTH_SHORT).show()
                        return@withContext DownloadResult.Error(msg)
                    }
                    is ApiResult.Success -> { /* continue */ }
                }

                val downloadToken = verifyResult.data.token

                // Step 4: Open download URL with token
                val downloadUrl = "${BuildConfig.API_BASE_URL}/api/assets/$assetId?token=$downloadToken"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                DownloadResult.Success
            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                val msg = e.message ?: "Download failed"
                Toast.makeText(context, "Download failed: $msg", Toast.LENGTH_SHORT).show()
                DownloadResult.Error(msg)
            }
        }
    }

    /**
     * Download a non-gated asset by opening the asset URL directly.
     */
    fun downloadFreeAsset(context: Context, assetId: String) {
        val downloadUrl = "${BuildConfig.API_BASE_URL}/api/assets/$assetId"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

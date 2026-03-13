package com.diversify.solana.rpc

import com.diversify.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class SolanaRpcClient @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val primaryRpcUrl: String = BuildConfig.SOLANA_RPC_URL

    private val rpcFallbackUrls = listOf(
        primaryRpcUrl,
        "https://api.mainnet-beta.solana.com",
        "https://solana-rpc.publicnode.com"
    ).distinct()

    private fun buildRequest(url: String, json: String): Request {
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())
        return Request.Builder()
            .url(url)
            .post(body)
            .build()
    }

    private suspend fun rpcCall(
        method: String,
        params: JSONArray = JSONArray()
    ): Result<Any> {
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", method)
            .put("params", params)
            .toString()

        val errors = mutableListOf<String>()

        rpcFallbackUrls.forEach { endpoint ->
            var attempt = 0
            while (attempt < MAX_ATTEMPTS_PER_ENDPOINT) {
                attempt++
                val callResult = runCatching { executeRpc(endpoint, payload) }
                if (callResult.isSuccess) {
                    return Result.success(callResult.getOrThrow())
                }

                val message = callResult.exceptionOrNull()?.message ?: "Unknown RPC failure."
                errors += "$endpoint attempt $attempt: $message"

                if (!isRetriable(message) || attempt >= MAX_ATTEMPTS_PER_ENDPOINT) {
                    break
                }
                delay(RETRY_DELAY_MS)
            }
        }

        val summary = errors.lastOrNull() ?: "Unknown error."
        return Result.failure(IOException("Mainnet RPC unavailable across fallback endpoints. $summary"))
    }

    private fun executeRpc(endpoint: String, payload: String): Any {
        val req = buildRequest(endpoint, payload)
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("RPC HTTP ${resp.code}")
            }

            val body = resp.body?.string().orEmpty()
            if (body.isBlank()) {
                throw IOException("Empty RPC response.")
            }

            val json = JSONObject(body)
            if (json.has("error")) {
                throw IOException(json.get("error").toString())
            }
            if (!json.has("result")) {
                throw IOException("RPC response missing result.")
            }

            return json.get("result")
        }
    }

    private fun isRetriable(message: String): Boolean {
        val m = message.lowercase()
        return m.contains("timeout") ||
            m.contains("timed out") ||
            m.contains("failed to connect") ||
            m.contains("connection reset") ||
            m.contains("http 429") ||
            m.contains("http 500") ||
            m.contains("http 502") ||
            m.contains("http 503") ||
            m.contains("http 504") ||
            m.contains("-32005")
    }

    private fun requireObject(result: Any, method: String): JSONObject {
        return result as? JSONObject
            ?: throw IOException("Unexpected RPC result for $method: object expected.")
    }

    suspend fun getBalance(publicKey: String): Result<Long> {
        val params = JSONArray()
            .put(publicKey)
            .put(JSONObject().put("commitment", "confirmed"))
        return rpcCall("getBalance", params).mapCatching { result ->
            val obj = requireObject(result, "getBalance")
            val value = obj.optLong("value", -1L)
            if (value < 0L) {
                throw IOException("Invalid SOL balance response.")
            }
            value
        }
    }

    suspend fun sendTransaction(transaction: ByteArray): Result<String> {
        val encoded = android.util.Base64.encodeToString(transaction, android.util.Base64.NO_WRAP)
        val params = JSONArray()
            .put(encoded)
            .put(
                JSONObject()
                    .put("encoding", "base64")
                    .put("preflightCommitment", "confirmed")
                    .put("maxRetries", 3)
            )
        return rpcCall("sendTransaction", params).mapCatching { result ->
            result as? String ?: throw IOException("Invalid sendTransaction response.")
        }
    }

    suspend fun getTransactionStatus(signature: String): Result<TransactionStatus> {
        val params = JSONArray()
            .put(JSONArray().put(signature))
            .put(JSONObject().put("searchTransactionHistory", true))
        return rpcCall("getSignatureStatuses", params).mapCatching { result ->
            val obj = requireObject(result, "getSignatureStatuses")
                val array = obj.getJSONArray("value")
                if (array.isNull(0)) {
                    TransactionStatus.NOT_FOUND
                } else {
                    val status = array.getJSONObject(0)
                    val confirmation = status.optString("confirmationStatus", "")
                    when {
                        status.has("err") && !status.isNull("err") -> TransactionStatus.FAILED
                        confirmation == "confirmed" || confirmation == "finalized" -> TransactionStatus.CONFIRMED
                        else -> TransactionStatus.PENDING
                    }
                }
            }
    }

    suspend fun getRecentBlockhash(): Result<String> {
        val params = JSONArray().put(JSONObject().put("commitment", "confirmed"))
        return rpcCall("getLatestBlockhash", params).mapCatching { result ->
            val obj = requireObject(result, "getLatestBlockhash")
            obj.getJSONObject("value").getString("blockhash")
        }
    }

    suspend fun getTokenBalanceBaseUnits(ownerPublicKey: String, mint: String): Result<Long> {
        val params = JSONArray()
            .put(ownerPublicKey)
            .put(JSONObject().put("mint", mint))
            .put(
                JSONObject()
                    .put("encoding", "jsonParsed")
                    .put("commitment", "confirmed")
            )
        return rpcCall("getTokenAccountsByOwner", params).mapCatching { result ->
            val obj = requireObject(result, "getTokenAccountsByOwner")
            val value = obj.getJSONArray("value")
            var total = 0L
            for (i in 0 until value.length()) {
                val amount = value.getJSONObject(i)
                    .getJSONObject("account")
                    .getJSONObject("data")
                    .getJSONObject("parsed")
                    .getJSONObject("info")
                    .getJSONObject("tokenAmount")
                    .optString("amount", "0")
                total += amount.toLongOrNull() ?: 0L
            }
            total
        }
    }

    enum class TransactionStatus {
        PENDING,
        CONFIRMED,
        FAILED,
        NOT_FOUND
    }

    companion object {
        private const val MAX_ATTEMPTS_PER_ENDPOINT = 2
        private const val RETRY_DELAY_MS = 300L
    }
}

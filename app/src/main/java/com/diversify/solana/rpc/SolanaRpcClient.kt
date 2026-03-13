package com.diversify.solana.rpc

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SolanaRpcClient @Inject constructor() {
    private val client = okhttp3.OkHttpClient()
    private val rpcUrl: String = com.diversify.app.BuildConfig.SOLANA_RPC_URL

    private fun buildRequest(json: String): okhttp3.Request {
        val body = okhttp3.RequestBody.create(
            "application/json".toMediaTypeOrNull(), json
        )
        return okhttp3.Request.Builder()
            .url(rpcUrl)
            .post(body)
            .build()
    }

    private suspend fun rpcCall(
        method: String,
        params: JSONArray = JSONArray()
    ): Result<JSONObject> {
        return try {
            val payload = JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", 1)
                .put("method", method)
                .put("params", params)
                .toString()
            val req = buildRequest(payload)
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                return Result.failure(Exception("RPC error ${resp.code}"))
            }
            val body = resp.body?.string() ?: ""
            val resultObj = JSONObject(body)
            if (resultObj.has("error")) {
                val err = resultObj.getJSONObject("error")
                return Result.failure(Exception(err.toString()))
            }
            Result.success(resultObj.getJSONObject("result"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBalance(publicKey: String): Result<Long> {
        return rpcCall("getBalance", JSONArray().put(publicKey))
            .mapCatching { it.getLong("value") }
    }

    suspend fun sendTransaction(transaction: ByteArray): Result<String> {
        val encoded = android.util.Base64.encodeToString(transaction, android.util.Base64.NO_WRAP)
        return rpcCall("sendTransaction", JSONArray().put(encoded))
            .mapCatching { it.getString("result") }
    }

    suspend fun getTransactionStatus(signature: String): Result<TransactionStatus> {
        return rpcCall("getSignatureStatuses", JSONArray().put(JSONArray().put(signature)))
            .mapCatching { obj ->
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
        return rpcCall("getLatestBlockhash")
            .mapCatching { it.getJSONObject("value").getString("blockhash") }
    }

    suspend fun getTokenBalanceBaseUnits(ownerPublicKey: String, mint: String): Result<Long> {
        val params = JSONArray()
            .put(ownerPublicKey)
            .put(JSONObject().put("mint", mint))
            .put(JSONObject().put("encoding", "jsonParsed"))
        return rpcCall("getTokenAccountsByOwner", params).mapCatching { result ->
            val value = result.getJSONArray("value")
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
}

package com.diversify.solana.rpc

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JupiterClient @Inject constructor() {
    private val client = OkHttpClient()
    private val url = "https://quote-api.jup.ag/v4/swap"

    suspend fun getSwapTransaction(
        inputMint: String,
        outputMint: String,
        amountLamports: Long,
        userPubkey: String
    ): Result<ByteArray> {
        return try {
            val bodyJson = JSONObject().apply {
                put("inputMint", inputMint)
                put("outputMint", outputMint)
                put("amount", amountLamports)
                put("slippage", 1.0) // 1% slippage default
                put("userPublicKey", userPubkey)
            }
            val body = RequestBody.create("application/json".toMediaTypeOrNull(), bodyJson.toString())
            val req = Request.Builder().url(url).post(body).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                return Result.failure(IOException("Jupiter API error ${resp.code}"))
            }
            val respBody = resp.body?.string() ?: ""
            val obj = JSONObject(respBody)
            val swapTx = obj.getString("swapTransaction")
            val bytes = android.util.Base64.decode(swapTx, android.util.Base64.DEFAULT)
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

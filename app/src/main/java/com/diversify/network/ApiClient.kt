package com.diversify.network

import okhttp3.*
import com.diversify.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object ApiClient {
    private val client = OkHttpClient()
    private val baseUrl = BuildConfig.BACKEND_URL // defined in build.gradle or manifest

    data class Invoice(val invoiceId: String, val merchantAddress: String, val amountSol: Double)
    data class LeaderboardEntry(val userId: String, val completions: Int)

    fun createInvoice(userId: String, callback: (Result<Invoice>) -> Unit) {
        val json = JSONObject().put("userId", userId).toString()
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json)
        val req = Request.Builder()
            .url("$baseUrl/invoice")
            .post(body)
            .build()
        client.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(Result.failure(IOException("Unexpected code $it")))
                        return
                    }
                    val obj = JSONObject(it.body!!.string())
                    val inv = Invoice(
                        invoiceId = obj.getString("invoiceId"),
                        merchantAddress = obj.getString("merchantAddress"),
                        amountSol = obj.getDouble("amountSol")
                    )
                    callback(Result.success(inv))
                }
            }
        })
    }

    fun fetchSession(sessionId: String, callback: (Result<JSONObject>) -> Unit) {
        val req = Request.Builder()
            .url("$baseUrl/sessions/$sessionId")
            .get()
            .build()
        client.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(Result.failure(IOException("Unexpected code $it")))
                        return
                    }
                    val obj = JSONObject(it.body!!.string())
                    callback(Result.success(obj))
                }
            }
        })
    }

    fun fetchLeaderboard(callback: (Result<List<LeaderboardEntry>>) -> Unit) {
        val req = Request.Builder()
            .url("$baseUrl/leaderboard")
            .get()
            .build()
        client.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(Result.failure(IOException("Unexpected code $it")))
                        return
                    }
                    val arr = JSONArray(it.body!!.string())
                    val list = mutableListOf<LeaderboardEntry>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            LeaderboardEntry(
                                userId = obj.getString("userId"),
                                completions = obj.getInt("completions")
                            )
                        )
                    }
                    callback(Result.success(list))
                }
            }
        })
    }
}

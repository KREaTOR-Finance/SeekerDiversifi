package com.diversify.data.repository

import android.content.SharedPreferences
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    
    companion object {
        private const val PREF_TRANSACTION_COUNT = "pref_transaction_count"
        private const val PREF_BUNDLER_RATIO = "pref_bundler_ratio"
        private const val PREF_CURATED_TOKENS = "pref_curated_tokens"
        private const val PREF_FUNDING_ASSET = "pref_funding_asset" // "SOL" or "SKR"
    }
    
    suspend fun getTransactionCount(): Int = withContext(Dispatchers.IO) {
        sharedPreferences.getInt(PREF_TRANSACTION_COUNT, 0)
    }
    
    suspend fun setTransactionCount(count: Int) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putInt(PREF_TRANSACTION_COUNT, count).apply()
    }
    
    suspend fun getBundlerRatio(): Float = withContext(Dispatchers.IO) {
        sharedPreferences.getFloat(PREF_BUNDLER_RATIO, 1.0f)
    }
    
    suspend fun setBundlerRatio(ratio: Float) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putFloat(PREF_BUNDLER_RATIO, ratio).apply()
    }
    
    suspend fun getCuratedTokens(): List<String> = withContext(Dispatchers.IO) {
        val tokensString = sharedPreferences.getString(PREF_CURATED_TOKENS, "")
        if (tokensString.isNullOrEmpty()) {
            emptyList()
        } else {
            tokensString.split(",").map { it.trim() }
        }
    }
    
    suspend fun setCuratedTokens(tokens: List<String>) = withContext(Dispatchers.IO) {
        val tokensString = tokens.joinToString(",")
        sharedPreferences.edit().putString(PREF_CURATED_TOKENS, tokensString).apply()
    }

    suspend fun getFundingAsset(): String = withContext(Dispatchers.IO) {
        sharedPreferences.getString(PREF_FUNDING_ASSET, "SOL") ?: "SOL"
    }

    suspend fun setFundingAsset(asset: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString(PREF_FUNDING_ASSET, asset).apply()
    }
    
    suspend fun clearAllSettings() = withContext(Dispatchers.IO) {
        sharedPreferences.edit().clear().apply()
    }
}

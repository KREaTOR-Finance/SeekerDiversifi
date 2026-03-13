package com.diversify.core.util

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugHelper @Inject constructor() {
    
    companion object {
        private const val TAG = "DiversifyDebug"
        var isDebugMode = BuildConfig.DEBUG
    }
    
    fun logTransaction(transactionId: String, status: String) {
        if (isDebugMode) {
            Log.d(TAG, "Transaction $transactionId: $status")
        }
    }
    
    fun logSession(sessionId: String, event: String) {
        if (isDebugMode) {
            Log.d(TAG, "Session $sessionId: $event")
        }
    }
    
    fun logError(error: Throwable) {
        if (isDebugMode) {
            Log.e(TAG, "Error", error)
        }
    }
    
    fun getSessionSummary(sessionId: String): String {
        return "Debug summary for session $sessionId"
    }
}

package com.diversify.solana.wallet

import kotlinx.coroutines.*
import javax.inject.Inject

class ReturnScheduler @Inject constructor(
    private val walletManager: CyclerWalletManager
) {

    fun scheduleReturns(sessionId: String, amounts: Map<Int, Double>) {
        amounts.forEach { (walletIndex, amount) ->
            scheduleReturn(sessionId, walletIndex, amount)
        }
    }

    /**
     * Schedule a single return as an internal app task.
     */
    fun scheduleReturn(sessionId: String, walletIndex: Int, amount: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            delay((3000..5000).random().toLong())
            try {
                val wallet = walletManager.getOrCreateWallet(walletIndex)
                val payload = walletManager.createReturnTransaction(
                    fromWallet = wallet,
                    toAddress = "session:$sessionId",
                    amount = (amount * 1_000_000_000L).toLong().coerceAtLeast(1L)
                )
                val signature = walletManager.signReturnPayload(walletIndex, payload)
                println("[ReturnScheduler] queued return from ${wallet.publicKey} for $sessionId sig=${signature.size}B")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelReturns(sessionId: String, walletIndices: List<Int>) {
        walletIndices.forEach { _ ->
            // placeholder
        }
    }
}

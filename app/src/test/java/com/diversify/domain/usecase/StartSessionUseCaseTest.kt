package com.diversify.domain.usecase

import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartSessionUseCaseTest {

    @Test
    fun createsInMemorySessionWithGeneratedTransactions() = runBlocking {
        val generator = GenerateTransactionBatchUseCase(Random(11))
        val useCase = StartSessionUseCase(generator)

        val (session, transactions) = useCase(
            totalTransactions = 12,
            sessionAmount = 0.6,
            allowlistTokens = listOf("USDC", "JUP", "BONK"),
            fundingAsset = "SOL",
            userId = "wallet_pubkey"
        )

        assertEquals("wallet_pubkey", session.userId)
        assertEquals(12, session.totalTransactions)
        assertEquals(12, transactions.size)
        assertTrue(transactions.all { it.sessionId == session.id })
    }
}

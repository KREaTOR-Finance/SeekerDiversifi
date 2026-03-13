package com.diversify.domain.usecase

import com.diversify.domain.model.TransactionType
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateTransactionBatchUseCaseTest {

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOddTransactionCount() {
        runBlocking {
            val useCase = GenerateTransactionBatchUseCase(Random(1))
            useCase(
                totalTransactions = 3,
                sessionAmount = 0.1,
                allowlistTokens = listOf("USDC", "JUP", "BONK"),
                fundingAsset = "SOL"
            )
        }
    }

    @Test
    fun plannerMaintainsBalancedInventoryAndCounts() = runBlocking {
        val useCase = GenerateTransactionBatchUseCase(Random(8))
        val batch = useCase(
            totalTransactions = 20,
            sessionAmount = 0.2,
            allowlistTokens = listOf("USDC", "JUP", "BONK"),
            fundingAsset = "SOL"
        )

        assertEquals(20, batch.size)
        assertEquals(TransactionType.BUY, batch.first().type)

        val holdings = mutableListOf<String>()
        var buyCount = 0
        var sellCount = 0

        batch.forEach { tx ->
            when (tx.type) {
                TransactionType.BUY -> {
                    buyCount++
                    holdings.add(tx.token)
                }

                TransactionType.SELL -> {
                    sellCount++
                    assertTrue("Sell emitted before any holdings", holdings.isNotEmpty())
                    val idx = holdings.indexOf(tx.token)
                    assertTrue("Sell token not present in holdings", idx >= 0)
                    holdings.removeAt(idx)
                }
            }
        }

        assertEquals(10, buyCount)
        assertEquals(10, sellCount)
        assertTrue("Planner should end with no holdings", holdings.isEmpty())
    }

    @Test
    fun avoidsImmediateSellOfLastBoughtTokenWhenAlternativesExist() = runBlocking {
        var foundAlternativeCase = false

        for (seed in 1..100) {
            val useCase = GenerateTransactionBatchUseCase(Random(seed))
            val batch = useCase(
                totalTransactions = 40,
                sessionAmount = 0.4,
                allowlistTokens = listOf("USDC", "JUP", "BONK"),
                fundingAsset = "SOL"
            )

            val holdings = mutableListOf<String>()
            var lastBought: String? = null

            batch.forEach { tx ->
                when (tx.type) {
                    TransactionType.BUY -> {
                        holdings.add(tx.token)
                        lastBought = tx.token
                    }

                    TransactionType.SELL -> {
                        val distinctHeld = holdings.distinct()
                        if (distinctHeld.size > 1 && lastBought != null) {
                            foundAlternativeCase = true
                            assertNotEquals(
                                "Sell should avoid immediate last-bought token when alternatives exist",
                                lastBought,
                                tx.token
                            )
                        }

                        val idx = holdings.indexOf(tx.token)
                        if (idx >= 0) {
                            holdings.removeAt(idx)
                        }
                    }
                }
            }
        }

        assertTrue("Expected at least one alternative-holdings sell case", foundAlternativeCase)
    }
}

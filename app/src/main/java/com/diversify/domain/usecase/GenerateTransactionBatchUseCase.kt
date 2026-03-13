package com.diversify.domain.usecase

import com.diversify.domain.model.MathPuzzle
import com.diversify.domain.model.PuzzleDifficulty
import com.diversify.domain.model.PuzzleType
import com.diversify.domain.model.Transaction
import com.diversify.domain.model.TransactionType
import com.diversify.solana.wallet.CyclerWalletManager
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

class GenerateTransactionBatchUseCase @Inject constructor(
    private val cyclerWalletManager: CyclerWalletManager
) {

    private val random = Random(System.currentTimeMillis())

    suspend operator fun invoke(
        count: Int,
        bundlerRatio: Float,
        curatedTokens: List<String>,
        fundingAsset: String
    ): List<Transaction> {

        if (count <= 0) return emptyList()

        val transactions = mutableListOf<Transaction>()
        val tradeAmount = 0.01

        repeat(count) { cycle ->
            val token = curatedTokens.shuffled().firstOrNull() ?: "SOL"

            transactions.add(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    sessionId = "",
                    type = TransactionType.CYCLER_TRADE_BUY,
                    amount = tradeAmount,
                    destinationWallet = null,
                    token = token,
                    puzzle = generateMathPuzzle(PuzzleDifficulty.MEDIUM),
                    rawData = buildSwapTransaction("SOL", token, tradeAmount),
                    status = com.diversify.domain.model.TransactionStatus.PENDING,
                    signature = null,
                    puzzleSolved = false,
                    puzzleTimeMs = 0L,
                    createdAt = System.currentTimeMillis(),
                    confirmedAt = null
                )
            )

            val dummyIndex = cycle % 50
            val dummyWallet = cyclerWalletManager.getOrCreateWallet(dummyIndex)
            transactions.add(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    sessionId = "",
                    type = TransactionType.BUNDLER_TRANSFER,
                    amount = tradeAmount,
                    destinationWallet = dummyWallet.publicKey,
                    token = if (fundingAsset == "SKR") "SKR" else null,
                    puzzle = generateMathPuzzle(PuzzleDifficulty.MEDIUM),
                    rawData = buildTransferTransaction(dummyWallet.publicKey, tradeAmount),
                    status = com.diversify.domain.model.TransactionStatus.PENDING,
                    signature = null,
                    puzzleSolved = false,
                    puzzleTimeMs = 0L,
                    createdAt = System.currentTimeMillis(),
                    confirmedAt = null,
                    metadata = mapOf("walletIndex" to dummyIndex.toString())
                )
            )

            transactions.add(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    sessionId = "",
                    type = TransactionType.CYCLER_TRADE_SELL,
                    amount = tradeAmount,
                    destinationWallet = null,
                    token = token,
                    puzzle = generateMathPuzzle(PuzzleDifficulty.MEDIUM),
                    rawData = buildSwapTransaction(token, "SKR", tradeAmount),
                    status = com.diversify.domain.model.TransactionStatus.PENDING,
                    signature = null,
                    puzzleSolved = false,
                    puzzleTimeMs = 0L,
                    createdAt = System.currentTimeMillis(),
                    confirmedAt = null
                )
            )
        }

        return transactions
    }

    private fun generateMathPuzzle(difficulty: PuzzleDifficulty): MathPuzzle {
        val a = random.nextInt(1, 20)
        val b = random.nextInt(1, 20)

        return when (random.nextInt(3)) {
            0 -> MathPuzzle(
                id = UUID.randomUUID().toString(),
                question = "$a + $b = ?",
                answer = (a + b).toString(),
                type = PuzzleType.ADDITION,
                difficulty = difficulty
            )

            1 -> {
                val (larger, smaller) = if (a > b) a to b else b to a
                MathPuzzle(
                    id = UUID.randomUUID().toString(),
                    question = "$larger - $smaller = ?",
                    answer = (larger - smaller).toString(),
                    type = PuzzleType.SUBTRACTION,
                    difficulty = difficulty
                )
            }

            else -> {
                val x = random.nextInt(2, 6)
                val y = random.nextInt(2, 6)
                MathPuzzle(
                    id = UUID.randomUUID().toString(),
                    question = "$x x $y = ?",
                    answer = (x * y).toString(),
                    type = PuzzleType.MULTIPLICATION,
                    difficulty = difficulty
                )
            }
        }
    }

    private fun buildTransferTransaction(to: String, amount: Double): ByteArray {
        return "transfer_${to}_${amount}".toByteArray()
    }

    private fun buildSwapTransaction(fromToken: String, toToken: String, amount: Double): ByteArray {
        return "swap_${fromToken}_${toToken}_${amount}".toByteArray()
    }
}

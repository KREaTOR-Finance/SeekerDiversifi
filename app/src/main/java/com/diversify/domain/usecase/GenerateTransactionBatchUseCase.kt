package com.diversify.domain.usecase

import com.diversify.domain.model.MathPuzzle
import com.diversify.domain.model.PuzzleDifficulty
import com.diversify.domain.model.PuzzleType
import com.diversify.domain.model.Transaction
import com.diversify.domain.model.TransactionType
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

class GenerateTransactionBatchUseCase @Inject constructor() {

    private var random = Random(System.currentTimeMillis())

    internal constructor(random: Random) : this() {
        this.random = random
    }

    suspend operator fun invoke(
        totalTransactions: Int,
        sessionAmount: Double,
        allowlistTokens: List<String>,
        fundingAsset: String
    ): List<Transaction> {
        val sanitized = allowlistTokens
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() && it != fundingAsset.uppercase() }
            .distinct()

        require(totalTransactions >= 2) { "Total transactions must be at least 2." }
        require(totalTransactions % 2 == 0) { "Total transactions must be even." }
        require(sessionAmount > 0.0) { "Session amount must be greater than zero." }
        require(sanitized.isNotEmpty()) { "Allowlist cannot be empty after filtering funding asset." }

        val totalBuys = totalTransactions / 2
        val amountPerBuy = sessionAmount / totalBuys.toDouble()
        val shuffledTokens = sanitized.shuffled(random)

        val transactions = ArrayList<Transaction>(totalTransactions)
        val heldLots = mutableListOf<String>()
        var buysRemaining = totalBuys
        var sellsRemaining = totalBuys
        var tokenCursor = 0
        var lastBoughtToken: String? = null

        repeat(totalTransactions) {
            val nextType = when {
                heldLots.isEmpty() -> TransactionType.BUY
                buysRemaining == 0 -> TransactionType.SELL
                sellsRemaining == 0 -> TransactionType.BUY
                random.nextBoolean() -> TransactionType.BUY
                else -> TransactionType.SELL
            }

            if (nextType == TransactionType.BUY) {
                val token = shuffledTokens[tokenCursor % shuffledTokens.size]
                tokenCursor++
                buysRemaining--
                heldLots.add(token)
                lastBoughtToken = token

                transactions.add(
                    Transaction(
                        id = UUID.randomUUID().toString(),
                        sessionId = "",
                        type = TransactionType.BUY,
                        amount = amountPerBuy,
                        token = token,
                        puzzle = generateMathPuzzle(PuzzleDifficulty.MEDIUM),
                        rawData = byteArrayOf(),
                        status = com.diversify.domain.model.TransactionStatus.PENDING,
                        signature = null,
                        createdAt = System.currentTimeMillis(),
                        confirmedAt = null
                    )
                )
            } else {
                val distinctHeld = heldLots.distinct()
                val sellCandidates =
                    if (distinctHeld.size > 1 && lastBoughtToken != null) distinctHeld.filter { it != lastBoughtToken }
                    else distinctHeld
                val sellToken = sellCandidates[random.nextInt(sellCandidates.size)]
                val lotIndex = heldLots.indexOfFirst { it == sellToken }
                if (lotIndex >= 0) {
                    heldLots.removeAt(lotIndex)
                }
                sellsRemaining--

                transactions.add(
                    Transaction(
                        id = UUID.randomUUID().toString(),
                        sessionId = "",
                        type = TransactionType.SELL,
                        amount = 0.0,
                        token = sellToken,
                        puzzle = generateMathPuzzle(PuzzleDifficulty.MEDIUM),
                        rawData = byteArrayOf(),
                        status = com.diversify.domain.model.TransactionStatus.PENDING,
                        signature = null,
                        createdAt = System.currentTimeMillis(),
                        confirmedAt = null
                    )
                )
            }
        }

        check(buysRemaining == 0) { "Planner ended with buys remaining." }
        check(sellsRemaining == 0) { "Planner ended with sells remaining." }
        check(heldLots.isEmpty()) { "Planner ended with non-zero holdings." }

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
}

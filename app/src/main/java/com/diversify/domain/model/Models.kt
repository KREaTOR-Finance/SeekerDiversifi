package com.diversify.domain.model

import java.util.*

data class Session(
    val id: String,
    val userId: String,
    val transactionCount: Int,
    val bundlerRatio: Float,
    val curatedTokens: List<String>,
    val fundingAsset: String,
    val startedAt: Long,
    val completedAt: Long?,
    val status: SessionStatus,
    val feePaid: Double,
    val skrRewardEarned: Double?,
    val governanceTokensEarned: Int
)

enum class SessionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class Transaction(
    val id: String,
    val sessionId: String,
    val type: TransactionType,
    val amount: Double,
    val token: String?,
    val destinationWallet: String?,
    val puzzle: MathPuzzle,
    val rawData: ByteArray,
    val status: TransactionStatus,
    val signature: String?,
    val puzzleSolved: Boolean,
    val puzzleTimeMs: Long,
    val createdAt: Long,
    val confirmedAt: Long?,
    var metadata: Map<String, String>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Transaction
        
        if (id != other.id) return false
        if (sessionId != other.sessionId) return false
        if (type != other.type) return false
        if (amount != other.amount) return false
        if (token != other.token) return false
        if (destinationWallet != other.destinationWallet) return false
        if (puzzle != other.puzzle) return false
        if (!rawData.contentEquals(other.rawData)) return false
        if (status != other.status) return false
        if (signature != other.signature) return false
        if (puzzleSolved != other.puzzleSolved) return false
        if (puzzleTimeMs != other.puzzleTimeMs) return false
        if (createdAt != other.createdAt) return false
        if (confirmedAt != other.confirmedAt) return false
        if (metadata != other.metadata) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + (token?.hashCode() ?: 0)
        result = 31 * result + (destinationWallet?.hashCode() ?: 0)
        result = 31 * result + puzzle.hashCode()
        result = 31 * result + rawData.contentHashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (signature?.hashCode() ?: 0)
        result = 31 * result + puzzleSolved.hashCode()
        result = 31 * result + puzzleTimeMs.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (confirmedAt?.hashCode() ?: 0)
        result = 31 * result + (metadata?.hashCode() ?: 0)
        return result
    }
}

enum class TransactionType {
    BUNDLER_TRANSFER,
    CYCLER_BUY,
    CYCLER_SELL,
    CYCLER_TRADE_BUY,
    CYCLER_TRADE_SELL
}

enum class TransactionStatus {
    PENDING,
    SIGNED,
    SUBMITTED,
    CONFIRMED,
    FAILED,
    SKIPPED
}

data class MathPuzzle(
    val id: String,
    val question: String,
    val answer: String,
    val type: PuzzleType,
    val difficulty: PuzzleDifficulty
)

enum class PuzzleType {
    ADDITION,
    SUBTRACTION,
    MULTIPLICATION,
    DIVISION
}

enum class PuzzleDifficulty {
    EASY,
    MEDIUM,
    HARD
}

data class CyclerWallet(
    val index: Int,
    val publicKey: String,
    val balance: Long,
    val isActive: Boolean,
    val privateSeed: String? = null
)

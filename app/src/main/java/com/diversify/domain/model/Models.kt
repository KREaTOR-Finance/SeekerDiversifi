package com.diversify.domain.model

data class Session(
    val id: String,
    val userId: String,
    val totalTransactions: Int,
    val sessionAmount: Double,
    val fundingAsset: String,
    val allowlistTokens: List<String>,
    val startedAt: Long,
    val completedAt: Long?,
    val status: SessionStatus
)

enum class SessionStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

data class Transaction(
    val id: String,
    val sessionId: String,
    val type: TransactionType,
    val amount: Double,
    val token: String,
    val puzzle: MathPuzzle,
    val rawData: ByteArray,
    val status: TransactionStatus,
    val signature: String?,
    val createdAt: Long,
    val confirmedAt: Long?
)

enum class TransactionType {
    BUY,
    SELL
}

enum class TransactionStatus {
    PENDING,
    SIGNED,
    CONFIRMED,
    FAILED
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

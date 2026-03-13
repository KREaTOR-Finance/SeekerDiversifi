package com.diversify.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val transactionCount: Int,
    val bundlerRatio: Float,
    val curatedTokens: String, // JSON string
    val fundingAsset: String,
    val startedAt: Long,
    val completedAt: Long?,
    val status: String,
    val feePaid: Double,
    val skrRewardEarned: Double?,
    val governanceTokensEarned: Int
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val type: String,
    val amount: Double,
    val token: String?,
    val destinationWallet: String?,
    val rawData: ByteArray?,
    val walletIndex: Int?,
    val status: String,
    val signature: String?,
    // puzzle fields persisted to allow resuming
    val puzzleId: String?,
    val puzzleQuestion: String?,
    val puzzleAnswer: String?,
    val puzzleType: String?,
    val puzzleDifficulty: String?,
    val puzzleSolved: Boolean,
    val puzzleTimeMs: Long,
    val createdAt: Long,
    val confirmedAt: Long?
)

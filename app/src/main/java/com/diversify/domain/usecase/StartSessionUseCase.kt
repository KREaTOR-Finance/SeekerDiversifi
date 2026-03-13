package com.diversify.domain.usecase

import com.diversify.domain.model.Session
import com.diversify.domain.model.SessionStatus
import java.util.UUID
import javax.inject.Inject

class StartSessionUseCase @Inject constructor(
    private val generateTransactionBatch: GenerateTransactionBatchUseCase
) {

    suspend operator fun invoke(
        totalTransactions: Int,
        sessionAmount: Double,
        allowlistTokens: List<String>,
        fundingAsset: String,
        userId: String
    ): Pair<Session, List<com.diversify.domain.model.Transaction>> {
        val session = Session(
            id = UUID.randomUUID().toString(),
            userId = userId,
            totalTransactions = totalTransactions,
            sessionAmount = sessionAmount,
            fundingAsset = fundingAsset,
            allowlistTokens = allowlistTokens,
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            status = SessionStatus.IN_PROGRESS
        )

        val transactions = generateTransactionBatch(
            totalTransactions = totalTransactions,
            sessionAmount = sessionAmount,
            allowlistTokens = allowlistTokens,
            fundingAsset = fundingAsset
        ).map { it.copy(sessionId = session.id) }

        return Pair(session, transactions)
    }
}

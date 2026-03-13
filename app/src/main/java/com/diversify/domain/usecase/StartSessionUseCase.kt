package com.diversify.domain.usecase

import com.diversify.data.repository.SessionRepository
import com.diversify.domain.model.Session
import com.diversify.domain.model.SessionStatus
import java.util.UUID
import javax.inject.Inject

class StartSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val generateTransactionBatch: GenerateTransactionBatchUseCase
) {
    
    /**
     * Starts a new session, persists it and its generated transactions.
     * Returns the session plus the list of generated transactions.
     */
    suspend operator fun invoke(
        transactionCount: Int,
        bundlerRatio: Float,
        curatedTokens: List<String>,
        fundingAsset: String,
        userId: String
    ): Pair<Session, List<com.diversify.domain.model.Transaction>> {
        val session = Session(
            id = UUID.randomUUID().toString(),
            userId = userId,
            transactionCount = transactionCount,
            bundlerRatio = bundlerRatio,
            curatedTokens = curatedTokens,
            fundingAsset = fundingAsset,
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            status = SessionStatus.IN_PROGRESS,
            feePaid = 3.0, // $3 fee
            skrRewardEarned = null,
            governanceTokensEarned = 0
        )
        
        sessionRepository.insertSession(
            com.diversify.data.local.entity.SessionEntity(
                id = session.id,
                userId = session.userId,
                transactionCount = session.transactionCount,
                bundlerRatio = session.bundlerRatio,
                curatedTokens = session.curatedTokens.joinToString(","),
                fundingAsset = fundingAsset,
                startedAt = session.startedAt,
                completedAt = session.completedAt,
                status = session.status.name,
                feePaid = session.feePaid,
                skrRewardEarned = session.skrRewardEarned,
                governanceTokensEarned = session.governanceTokensEarned
            )
        )
        
        // generate and persist transactions
        val transactions = generateTransactionBatch(
            count = transactionCount,
            bundlerRatio = bundlerRatio,
            curatedTokens = curatedTokens,
            fundingAsset = fundingAsset
        ).map { it.copy(sessionId = session.id) }
        
        val entities = transactions.map { tx ->
            com.diversify.data.local.entity.TransactionEntity(
                id = tx.id,
                sessionId = session.id,
                type = tx.type.name,
                amount = tx.amount,
                token = tx.token,
                destinationWallet = tx.destinationWallet,
                rawData = tx.rawData,
                walletIndex = tx.metadata?.get("walletIndex")?.toString()?.toIntOrNull(),
                status = tx.status.name,
                signature = tx.signature,
                // include puzzle details
                puzzleId = tx.puzzle.id,
                puzzleQuestion = tx.puzzle.question,
                puzzleAnswer = tx.puzzle.answer,
                puzzleType = tx.puzzle.type.name,
                puzzleDifficulty = tx.puzzle.difficulty.name,
                puzzleSolved = tx.puzzleSolved,
                puzzleTimeMs = tx.puzzleTimeMs,
                createdAt = tx.createdAt,
                confirmedAt = tx.confirmedAt
            )
        }
        sessionRepository.insertTransactions(entities)
        
        return Pair(session, transactions)
    }
}

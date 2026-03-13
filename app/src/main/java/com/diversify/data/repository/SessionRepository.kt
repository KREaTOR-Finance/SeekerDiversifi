package com.diversify.data.repository

import com.diversify.data.local.dao.SessionDao
import com.diversify.data.local.dao.TransactionDao
import com.diversify.data.local.entity.SessionEntity
import com.diversify.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val transactionDao: TransactionDao
) {
    
    fun getAllSessions(): Flow<List<SessionEntity>> {
        return sessionDao.getAllSessions()
    }
    
    suspend fun getSessionById(sessionId: String): SessionEntity? {
        return sessionDao.getSessionById(sessionId)
    }
    
    suspend fun insertSession(session: SessionEntity) {
        sessionDao.insertSession(session)
    }
    
    suspend fun updateSession(session: SessionEntity) {
        sessionDao.updateSession(session)
    }
    
    fun getTransactionsForSession(sessionId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForSession(sessionId)
    }
    
    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }
    
    suspend fun insertTransactions(transactions: List<TransactionEntity>) {
        transactionDao.insertTransactions(transactions)
    }
    
    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }
    
    suspend fun getActiveSession(): SessionEntity? {
        return sessionDao.getActiveSession()
    }
    
    suspend fun getCompletedSessionsCount(): Int {
        return sessionDao.getCompletedSessionsCount()
    }

    suspend fun getCompletedTransactionsCount(sessionId: String): Int {
        return transactionDao.getCompletedTransactionsCount(sessionId)
    }
}

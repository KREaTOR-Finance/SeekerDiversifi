package com.diversify.data.local.dao

import androidx.room.*
import com.diversify.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions WHERE sessionId = :sessionId ORDER BY createdAt")
    fun getTransactionsForSession(sessionId: String): Flow<List<TransactionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions WHERE sessionId = :sessionId")
    suspend fun deleteTransactionsForSession(sessionId: String)
    
    @Query("SELECT COUNT(*) FROM transactions WHERE sessionId = :sessionId AND status = 'CONFIRMED'")
    suspend fun getConfirmedTransactionsCount(sessionId: String): Int

    // count anything that's no longer pending so progress resumes correctly
    @Query("SELECT COUNT(*) FROM transactions WHERE sessionId = :sessionId AND status <> 'PENDING'")
    suspend fun getCompletedTransactionsCount(sessionId: String): Int
}

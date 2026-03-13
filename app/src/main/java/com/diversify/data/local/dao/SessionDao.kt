package com.diversify.data.local.dao

import androidx.room.*
import com.diversify.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): SessionEntity?
    
    @Query("SELECT * FROM sessions WHERE status = 'IN_PROGRESS' LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)
    
    @Update
    suspend fun updateSession(session: SessionEntity)
    
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)
    
    @Query("SELECT COUNT(*) FROM sessions WHERE status = 'COMPLETED'")
    suspend fun getCompletedSessionsCount(): Int
}

package com.example.pillmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pillmate.data.local.entity.ChatMessageEntity
import com.example.pillmate.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessions(sessions: List<ChatSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(messages: List<ChatMessageEntity>)

    @Query("SELECT * FROM chat_sessions WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun getSessionsForProfile(profileId: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_messages WHERE profileId = :profileId AND sessionId = :sessionId ORDER BY createdAt ASC")
    fun getMessagesForSession(profileId: String, sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_sessions WHERE profileId = :profileId")
    suspend fun getSessionCount(profileId: String): Int

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions WHERE profileId = :profileId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestSession(profileId: String): ChatSessionEntity?

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun getMessageCountForSession(sessionId: String): Int

    @Query(
        "SELECT * FROM chat_sessions " +
            "WHERE profileId = :profileId " +
            "AND id != :excludeSessionId " +
            "AND NOT EXISTS (SELECT 1 FROM chat_messages m WHERE m.sessionId = chat_sessions.id) " +
            "ORDER BY updatedAt DESC LIMIT 1"
    )
    suspend fun getAnotherEmptySession(profileId: String, excludeSessionId: String): ChatSessionEntity?

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)
}

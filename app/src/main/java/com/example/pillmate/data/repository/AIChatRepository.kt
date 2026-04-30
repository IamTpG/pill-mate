package com.example.pillmate.data.repository

import com.example.pillmate.data.local.dao.ChatDao
import com.example.pillmate.data.local.entity.ChatMessageEntity
import com.example.pillmate.data.local.entity.ChatSessionEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AIChatRepository(
    private val chatDao: ChatDao,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {
    fun getSessions(profileId: String): Flow<List<ChatSessionEntity>> {
        return chatDao.getSessionsForProfile(profileId)
    }

    fun getMessages(profileId: String, sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(profileId, sessionId)
    }

    suspend fun ensureSession(profileId: String): String {
        chatDao.getLatestSession(profileId)?.let { return it.id }
        if (chatDao.getSessionCount(profileId) > 0) {
            return chatDao.getLatestSession(profileId)?.id ?: createSession(profileId, "New Chat")
        }
        return createSession(profileId, "New Chat")
    }

    suspend fun getMessageCount(sessionId: String): Int {
        return chatDao.getMessageCountForSession(sessionId)
    }

    suspend fun findAnotherEmptySession(profileId: String, excludeSessionId: String): ChatSessionEntity? {
        return chatDao.getAnotherEmptySession(profileId, excludeSessionId)
    }

    suspend fun createSession(profileId: String, title: String): String {
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        chatDao.upsertSession(
            ChatSessionEntity(
                id = sessionId,
                profileId = profileId,
                title = title,
                createdAt = now,
                updatedAt = now
            )
        )

        db.collection("profiles").document(profileId)
            .collection("aiChats").document(sessionId)
            .set(
                mapOf(
                    "title" to title,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )
            .await()

        return sessionId
    }

    suspend fun refreshFromRemote(profileId: String) {
        val sessionsSnapshot = db.collection("profiles").document(profileId)
            .collection("aiChats")
            .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        val sessionEntities = sessionsSnapshot.documents.map { doc ->
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            val updatedAt = doc.getLong("updatedAt") ?: createdAt
            ChatSessionEntity(
                id = doc.id,
                profileId = profileId,
                title = doc.getString("title").orEmpty().ifBlank { "New Chat" },
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
        if (sessionEntities.isNotEmpty()) {
            chatDao.upsertSessions(sessionEntities)
        }

        sessionsSnapshot.documents.forEach { sessionDoc ->
            val messagesSnapshot = sessionDoc.reference.collection("messages")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()

            val messageEntities = messagesSnapshot.documents.map { msgDoc ->
                ChatMessageEntity(
                    id = msgDoc.id,
                    sessionId = sessionDoc.id,
                    profileId = profileId,
                    text = msgDoc.getString("text").orEmpty(),
                    isBot = msgDoc.getBoolean("isBot") ?: false,
                    createdAt = msgDoc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
            if (messageEntities.isNotEmpty()) {
                chatDao.upsertMessages(messageEntities)
            }
        }
    }

    suspend fun addMessage(
        profileId: String,
        sessionId: String,
        text: String,
        isBot: Boolean,
        createdAt: Long = System.currentTimeMillis()
    ): ChatMessageEntity {
        val messageId = UUID.randomUUID().toString()
        val message = ChatMessageEntity(
            id = messageId,
            sessionId = sessionId,
            profileId = profileId,
            text = text,
            isBot = isBot,
            createdAt = createdAt
        )
        chatDao.upsertMessage(message)

        val sessionRef = db.collection("profiles").document(profileId)
            .collection("aiChats").document(sessionId)
        sessionRef.collection("messages")
            .document(messageId)
            .set(
                mapOf(
                    "text" to text,
                    "isBot" to isBot,
                    "createdAt" to createdAt
                )
            )
            .await()
        sessionRef.update("updatedAt", createdAt).await()
        return message
    }

    suspend fun updateSessionTitle(profileId: String, sessionId: String, title: String) {
        val existing = chatDao.getSessionById(sessionId)
        val now = System.currentTimeMillis()
        val session = ChatSessionEntity(
            id = sessionId,
            profileId = profileId,
            title = title,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        chatDao.upsertSession(session)
        db.collection("profiles").document(profileId)
            .collection("aiChats").document(sessionId)
            .update(
                mapOf(
                    "title" to title,
                    "updatedAt" to now
                )
            )
            .await()
    }

    suspend fun deleteSession(profileId: String, sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSessionById(sessionId)
        val sessionRef = db.collection("profiles").document(profileId)
            .collection("aiChats").document(sessionId)
        val messages = sessionRef.collection("messages").get().await()
        if (messages.documents.isNotEmpty()) {
            val batch = db.batch()
            messages.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        sessionRef.delete().await()
    }

    suspend fun askAssistant(userText: String): String {
        val payload = hashMapOf("message" to userText)
        val result = functions.getHttpsCallable("askMediCabinet").call(payload).await()
        val responseMap = result.data as? Map<*, *>
        return responseMap?.get("reply") as? String ?: "No response"
    }
}

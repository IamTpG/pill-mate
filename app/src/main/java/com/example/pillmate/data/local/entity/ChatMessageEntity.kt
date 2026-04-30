package com.example.pillmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val profileId: String,
    val text: String,
    val isBot: Boolean,
    val createdAt: Long
)

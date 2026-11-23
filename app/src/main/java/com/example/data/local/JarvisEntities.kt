package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encrypted_memories")
data class EncryptedMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val encryptedTitle: String,
    val encryptedContent: String,
    val encryptedTags: String,
    val importance: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRecalledAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "encrypted_chat_messages")
data class EncryptedChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val encryptedText: String,
    val encryptedActionJson: String = "",
    val encryptedMemoriesJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "encrypted_automations")
data class EncryptedAutomationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encryptedName: String,
    val encryptedTrigger: String,
    val actionType: String,
    val encryptedTarget: String,
    val encryptedPayload: String,
    val isEnabled: Boolean = true,
    val runCount: Int = 0,
    val lastRunAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "encrypted_contacts")
data class EncryptedContactShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encryptedAlias: String,
    val encryptedDisplayName: String,
    val encryptedPhoneNumber: String,
    val encryptedEmail: String,
    val encryptedNotes: String,
    val createdAt: Long = System.currentTimeMillis()
)

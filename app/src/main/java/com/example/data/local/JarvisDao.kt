package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
    // Memories
    @Query("SELECT * FROM encrypted_memories ORDER BY importance DESC, lastRecalledAt DESC")
    fun observeMemories(): Flow<List<EncryptedMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(entity: EncryptedMemoryEntity): Long

    @Update
    suspend fun updateMemory(entity: EncryptedMemoryEntity)

    @Query("DELETE FROM encrypted_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("UPDATE encrypted_memories SET lastRecalledAt = :timestamp WHERE id IN (:ids)")
    suspend fun markMemoriesRecalled(ids: List<Long>, timestamp: Long)

    @Query("DELETE FROM encrypted_memories")
    suspend fun clearAllMemories()

    // Chat Messages
    @Query("SELECT * FROM encrypted_chat_messages ORDER BY timestamp ASC, id ASC")
    fun observeMessages(): Flow<List<EncryptedChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(entity: EncryptedChatMessageEntity): Long

    @Query("UPDATE encrypted_chat_messages SET encryptedActionJson = :encryptedActionJson WHERE id = :id")
    suspend fun updateMessageAction(id: Long, encryptedActionJson: String)

    @Query("DELETE FROM encrypted_chat_messages")
    suspend fun clearAllMessages()

    // Automations
    @Query("SELECT * FROM encrypted_automations ORDER BY isEnabled DESC, createdAt DESC")
    fun observeAutomations(): Flow<List<EncryptedAutomationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(entity: EncryptedAutomationEntity): Long

    @Update
    suspend fun updateAutomation(entity: EncryptedAutomationEntity)

    @Query("DELETE FROM encrypted_automations WHERE id = :id")
    suspend fun deleteAutomationById(id: Long)

    @Query("DELETE FROM encrypted_automations")
    suspend fun clearAllAutomations()

    // Contacts
    @Query("SELECT * FROM encrypted_contacts ORDER BY createdAt DESC")
    fun observeContacts(): Flow<List<EncryptedContactShortcutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(entity: EncryptedContactShortcutEntity): Long

    @Query("DELETE FROM encrypted_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Query("DELETE FROM encrypted_contacts")
    suspend fun clearAllContacts()
}

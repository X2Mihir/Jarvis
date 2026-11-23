package com.example.data.repository

import com.example.data.local.EncryptedAutomationEntity
import com.example.data.local.EncryptedChatMessageEntity
import com.example.data.local.EncryptedContactShortcutEntity
import com.example.data.local.EncryptedMemoryEntity
import com.example.data.local.JarvisDao
import com.example.domain.AutomationRule
import com.example.domain.ChatMessage
import com.example.domain.ChatRole
import com.example.domain.ContactShortcut
import com.example.domain.MemoryCategory
import com.example.domain.MemoryItem
import com.example.domain.PhoneActionPlan
import com.example.domain.PhoneActionType
import com.example.security.CryptoVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class JarvisVaultRepository(private val dao: JarvisDao) {

    val memories: Flow<List<MemoryItem>> = dao.observeMemories().map { list ->
        withContext(Dispatchers.IO) {
            list.map { it.toDomain() }
        }
    }

    val messages: Flow<List<ChatMessage>> = dao.observeMessages().map { list ->
        withContext(Dispatchers.IO) {
            list.map { it.toDomain() }
        }
    }

    val automations: Flow<List<AutomationRule>> = dao.observeAutomations().map { list ->
        withContext(Dispatchers.IO) {
            list.map { it.toDomain() }
        }
    }

    val contacts: Flow<List<ContactShortcut>> = dao.observeContacts().map { list ->
        withContext(Dispatchers.IO) {
            list.map { it.toDomain() }
        }
    }

    suspend fun saveMemory(item: MemoryItem): Long = withContext(Dispatchers.IO) {
        val tagsJson = JSONArray(item.tags).toString()
        val entity = EncryptedMemoryEntity(
            id = item.id,
            category = item.category.name,
            encryptedTitle = CryptoVault.encrypt(item.title),
            encryptedContent = CryptoVault.encrypt(item.content),
            encryptedTags = CryptoVault.encrypt(tagsJson),
            importance = item.importance.coerceIn(1, 5),
            createdAt = item.createdAt,
            lastRecalledAt = item.lastRecalledAt
        )
        if (item.id == 0L) {
            dao.insertMemory(entity)
        } else {
            dao.updateMemory(entity)
            item.id
        }
    }

    suspend fun deleteMemory(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteMemoryById(id)
    }

    suspend fun markMemoriesRecalled(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) {
            dao.markMemoriesRecalled(ids, System.currentTimeMillis())
        }
    }

    suspend fun appendMessage(message: ChatMessage): Long = withContext(Dispatchers.IO) {
        val actionJson = message.actionPlan?.let { serializeActionPlan(it) }.orEmpty()
        val memoryTitlesJson = if (message.savedMemoryTitles.isNotEmpty()) {
            JSONArray(message.savedMemoryTitles).toString()
        } else {
            ""
        }
        val entity = EncryptedChatMessageEntity(
            id = message.id,
            role = message.role.name,
            encryptedText = CryptoVault.encrypt(message.text),
            encryptedActionJson = if (actionJson.isNotEmpty()) CryptoVault.encrypt(actionJson) else "",
            encryptedMemoriesJson = if (memoryTitlesJson.isNotEmpty()) CryptoVault.encrypt(memoryTitlesJson) else "",
            timestamp = message.timestamp
        )
        dao.insertMessage(entity)
    }

    suspend fun markMessageActionExecuted(messageId: Long, actionPlan: PhoneActionPlan) =
        withContext(Dispatchers.IO) {
            val updated = actionPlan.copy(executed = true)
            val encrypted = CryptoVault.encrypt(serializeActionPlan(updated))
            dao.updateMessageAction(messageId, encrypted)
        }

    suspend fun clearConversation() = withContext(Dispatchers.IO) {
        dao.clearAllMessages()
    }

    suspend fun saveAutomation(rule: AutomationRule): Long = withContext(Dispatchers.IO) {
        val entity = EncryptedAutomationEntity(
            id = rule.id,
            encryptedName = CryptoVault.encrypt(rule.name),
            encryptedTrigger = CryptoVault.encrypt(rule.triggerDescription),
            actionType = rule.actionType.name,
            encryptedTarget = CryptoVault.encrypt(rule.target),
            encryptedPayload = CryptoVault.encrypt(rule.payload),
            isEnabled = rule.isEnabled,
            runCount = rule.runCount,
            lastRunAt = rule.lastRunAt,
            createdAt = rule.createdAt
        )
        if (rule.id == 0L) {
            dao.insertAutomation(entity)
        } else {
            dao.updateAutomation(entity)
            rule.id
        }
    }

    suspend fun recordAutomationRun(rule: AutomationRule) = withContext(Dispatchers.IO) {
        saveAutomation(
            rule.copy(
                runCount = rule.runCount + 1,
                lastRunAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleAutomationEnabled(rule: AutomationRule) = withContext(Dispatchers.IO) {
        saveAutomation(rule.copy(isEnabled = !rule.isEnabled))
    }

    suspend fun deleteAutomation(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteAutomationById(id)
    }

    suspend fun saveContact(contact: ContactShortcut): Long = withContext(Dispatchers.IO) {
        val entity = EncryptedContactShortcutEntity(
            id = contact.id,
            encryptedAlias = CryptoVault.encrypt(contact.alias),
            encryptedDisplayName = CryptoVault.encrypt(contact.displayName),
            encryptedPhoneNumber = CryptoVault.encrypt(contact.phoneNumber),
            encryptedEmail = CryptoVault.encrypt(contact.email),
            encryptedNotes = CryptoVault.encrypt(contact.notes),
            createdAt = contact.createdAt
        )
        dao.insertContact(entity)
    }

    suspend fun deleteContact(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteContactById(id)
    }

    suspend fun wipeAllEncryptedData() = withContext(Dispatchers.IO) {
        dao.clearAllMessages()
        dao.clearAllMemories()
        dao.clearAllAutomations()
        dao.clearAllContacts()
    }

    private fun EncryptedMemoryEntity.toDomain(): MemoryItem {
        val decryptedTags = CryptoVault.decrypt(encryptedTags)
        val tags = try {
            if (decryptedTags.isBlank()) emptyList()
            else {
                val arr = JSONArray(decryptedTags)
                (0 until arr.length()).map { arr.getString(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
        return MemoryItem(
            id = id,
            category = MemoryCategory.fromString(category),
            title = CryptoVault.decrypt(encryptedTitle),
            content = CryptoVault.decrypt(encryptedContent),
            tags = tags,
            importance = importance,
            createdAt = createdAt,
            lastRecalledAt = lastRecalledAt
        )
    }

    private fun EncryptedChatMessageEntity.toDomain(): ChatMessage {
        val actionPlan = if (encryptedActionJson.isNotBlank()) {
            deserializeActionPlan(CryptoVault.decrypt(encryptedActionJson))
        } else null
        val savedTitles = if (encryptedMemoriesJson.isNotBlank()) {
            try {
                val arr = JSONArray(CryptoVault.decrypt(encryptedMemoriesJson))
                (0 until arr.length()).map { arr.getString(it) }
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        return ChatMessage(
            id = id,
            role = if (role == ChatRole.USER.name) ChatRole.USER else ChatRole.JARVIS,
            text = CryptoVault.decrypt(encryptedText),
            actionPlan = actionPlan,
            savedMemoryTitles = savedTitles,
            timestamp = timestamp
        )
    }

    private fun EncryptedAutomationEntity.toDomain(): AutomationRule {
        return AutomationRule(
            id = id,
            name = CryptoVault.decrypt(encryptedName),
            triggerDescription = CryptoVault.decrypt(encryptedTrigger),
            actionType = PhoneActionType.fromString(actionType),
            target = CryptoVault.decrypt(encryptedTarget),
            payload = CryptoVault.decrypt(encryptedPayload),
            isEnabled = isEnabled,
            runCount = runCount,
            lastRunAt = lastRunAt,
            createdAt = createdAt
        )
    }

    private fun EncryptedContactShortcutEntity.toDomain(): ContactShortcut {
        return ContactShortcut(
            id = id,
            alias = CryptoVault.decrypt(encryptedAlias),
            displayName = CryptoVault.decrypt(encryptedDisplayName),
            phoneNumber = CryptoVault.decrypt(encryptedPhoneNumber),
            email = CryptoVault.decrypt(encryptedEmail),
            notes = CryptoVault.decrypt(encryptedNotes),
            createdAt = createdAt
        )
    }

    private fun serializeActionPlan(plan: PhoneActionPlan): String {
        return JSONObject().apply {
            put("type", plan.type.name)
            put("target", plan.target)
            put("payload", plan.payload)
            put("reason", plan.reason)
            put("executed", plan.executed)
        }.toString()
    }

    private fun deserializeActionPlan(jsonStr: String): PhoneActionPlan? {
        return try {
            val obj = JSONObject(jsonStr)
            val type = PhoneActionType.fromString(obj.optString("type"))
            if (type == PhoneActionType.NONE) null
            else PhoneActionPlan(
                type = type,
                target = obj.optString("target"),
                payload = obj.optString("payload"),
                reason = obj.optString("reason"),
                executed = obj.optBoolean("executed", false)
            )
        } catch (_: Exception) {
            null
        }
    }
}

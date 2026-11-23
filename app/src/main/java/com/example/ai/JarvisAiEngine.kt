package com.example.ai

import android.util.Log
import com.example.domain.AutomationRule
import com.example.domain.ChatMessage
import com.example.domain.ChatRole
import com.example.domain.ContactShortcut
import com.example.domain.MemoryCategory
import com.example.domain.MemoryItem
import com.example.domain.PhoneActionPlan
import com.example.domain.PhoneActionType
import com.example.domain.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExtractedMemory(
    val title: String,
    val content: String,
    val category: MemoryCategory,
    val importance: Int,
    val tags: List<String>
)

data class JarvisInferenceResult(
    val reply: String,
    val actionPlan: PhoneActionPlan?,
    val newMemories: List<ExtractedMemory>,
    val newAutomation: AutomationRule?,
    val recalledMemoryIds: List<Long>
)

class JarvisAiEngine {
    companion object {
        private const val TAG = "JarvisAiEngine"
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val TIMEOUT_MS = 60_000L
    }

    private val jarvisResponseSchema = Schema.obj(
        properties = mapOf(
            "reply" to Schema.string(
                description = "Helpful, personalized response to the user referencing remembered facts naturally."
            ),
            "action" to Schema.obj(
                properties = mapOf(
                    "type" to Schema.enumeration(
                        values = PhoneActionType.entries.map { it.name },
                        description = "Phone automation action type, or NONE if purely conversational."
                    ),
                    "target" to Schema.string(
                        description = "Recipient name/phone/email, alarm HH:MM, timer seconds as integer string, or event title."
                    ),
                    "payload" to Schema.string(
                        description = "SMS body, email 'Subject | Body', alarm/timer label, or calendar description."
                    ),
                    "reason" to Schema.string(
                        description = "Short 1-sentence confirmation summary of what this phone action will do."
                    )
                )
            ),
            "newMemories" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Short memory title, 2-6 words"),
                        "content" to Schema.string(description = "Durable fact, preference, relationship, habit, or task detail to remember"),
                        "category" to Schema.enumeration(
                            values = MemoryCategory.entries.map { it.name },
                            description = "Memory category"
                        ),
                        "importance" to Schema.integer(description = "1 to 5 importance score"),
                        "tags" to Schema.array(items = Schema.string())
                    )
                ),
                description = "Any new personal facts, preferences, contacts, schedules, or instructions worth saving to encrypted local memory. Empty if nothing new."
            ),
            "saveAutomation" to Schema.obj(
                properties = mapOf(
                    "shouldSave" to Schema.boolean(description = "True ONLY when user explicitly asks to save a reusable workflow/automation shortcut."),
                    "name" to Schema.string(),
                    "triggerDescription" to Schema.string(),
                    "actionType" to Schema.enumeration(values = PhoneActionType.entries.map { it.name }),
                    "target" to Schema.string(),
                    "payload" to Schema.string()
                ),
                optionalProperties = listOf("name", "triggerDescription", "actionType", "target", "payload")
            ),
            "recalledMemoryIds" to Schema.array(
                items = Schema.integer(),
                description = "IDs of existing memories that informed this response."
            )
        ),
        optionalProperties = listOf("saveAutomation", "recalledMemoryIds")
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = jarvisResponseSchema
                temperature = 0.4f
            },
            systemInstruction = content {
                text(
                    """
                    You are Jarvis Core, a personal Android AI assistant with an encrypted local memory vault and direct phone workflow orchestration.
                    Capabilities you control on this phone:
                    - CALL: Dial or call a contact/number (set target = contact name or phone number).
                    - SMS: Draft and open SMS to a contact/number with personalized body in payload.
                    - EMAIL: Draft email to recipient in target, payload formatted as 'Subject | Body'.
                    - ALARM: Set an alarm (put 24-hour HH:MM in target, label in payload).
                    - TIMER: Start a countdown timer (put integer seconds in target, label in payload).
                    - CALENDAR: Draft a calendar event (event title in target, description in payload).
                    - SETTINGS_WIFI / SETTINGS_BLUETOOTH / SETTINGS_SOUND / SETTINGS_BATTERY: Open those system controls.
                    - NONE: Conversational answer, memory recall, planning, or analysis.

                    Memory Rules:
                    1. Always leverage the provided ENCRYPTED LOCAL MEMORY VAULT and SAVED CONTACTS so you personalize every message, draft, and recommendation.
                    2. When the user reveals personal facts, preferences, people, routines, work projects, or reminders (e.g., "My mom's name is Priya", "I prefer concise morning texts", "My standup is at 9:30 AM"), extract them cleanly into newMemories unless already stored in the vault.
                    3. Never fabricate phone numbers or contact details that are not in the vault/contacts; if a number is missing for a call/SMS, put the contact's name in target so the app can resolve it from Contacts or ask the user to pick them.
                    4. Only set saveAutomation.shouldSave = true when the user asks to create/save an automation rule or reusable shortcut.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun processUserCommand(
        userInput: String,
        recentMessages: List<ChatMessage>,
        memories: List<MemoryItem>,
        contacts: List<ContactShortcut>,
        automations: List<AutomationRule>,
        preferences: UserPreferences
    ): JarvisInferenceResult {
        val nowFormatted = SimpleDateFormat("EEE, MMM d yyyy HH:mm", Locale.getDefault())
            .format(Date())

        val memoryBlock = if (!preferences.injectMemoryContext || memories.isEmpty()) {
            "None stored or context injection paused."
        } else {
            memories.take(60).joinToString("\n") { m ->
                "- [id=${m.id}] (${m.category.label}, imp=${m.importance}) ${m.title}: ${m.content}"
            }
        }

        val contactsBlock = if (contacts.isEmpty()) {
            "No saved contact shortcuts."
        } else {
            contacts.joinToString("\n") { c ->
                "- ${c.displayName} (alias: ${c.alias}, phone: ${c.phoneNumber}, email: ${c.email})"
            }
        }

        val automationsBlock = if (automations.isEmpty()) {
            "None"
        } else {
            automations.joinToString("\n") { a ->
                "- ${a.name} [${a.actionType.name}] target=${a.target} enabled=${a.isEnabled}"
            }
        }

        val recentHistory = recentMessages.takeLast(8).joinToString("\n") { msg ->
            val speaker = if (msg.role == ChatRole.USER) "User" else "Jarvis"
            "$speaker: ${msg.text}"
        }

        val prompt = """
            Current Local Time: $nowFormatted
            User Name: ${preferences.userName.ifBlank { "Unknown" }}
            Preferred Tone: ${preferences.assistantTone.directive}
            Auto-Extract New Memories: ${preferences.autoRememberEnabled}

            ENCRYPTED LOCAL MEMORY VAULT:
            $memoryBlock

            SAVED CONTACT SHORTCUTS:
            $contactsBlock

            SAVED AUTOMATIONS:
            $automationsBlock

            RECENT CONVERSATION:
            $recentHistory

            NEW USER REQUEST:
            $userInput
        """.trimIndent()

        return try {
            val response = withTimeout(TIMEOUT_MS) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("Jarvis AI returned an empty response.")
            parseStructuredResponse(rawJson, preferences.autoRememberEnabled)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Jarvis inference timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Jarvis inference failed", e)
            throw e
        }
    }

    private fun parseStructuredResponse(
        jsonString: String,
        autoRemember: Boolean
    ): JarvisInferenceResult {
        val root = JSONObject(jsonString)
        val reply = root.optString("reply").ifBlank {
            "Command processed."
        }

        val actionObj = root.optJSONObject("action")
        val actionPlan = if (actionObj != null) {
            val type = PhoneActionType.fromString(actionObj.optString("type"))
            if (type == PhoneActionType.NONE) {
                null
            } else {
                PhoneActionPlan(
                    type = type,
                    target = actionObj.optString("target"),
                    payload = actionObj.optString("payload"),
                    reason = actionObj.optString("reason")
                )
            }
        } else null

        val extractedMemories = mutableListOf<ExtractedMemory>()
        if (autoRemember) {
            val memArr = root.optJSONArray("newMemories")
            if (memArr != null) {
                for (i in 0 until memArr.length()) {
                    val item = memArr.optJSONObject(i) ?: continue
                    val title = item.optString("title").trim()
                    val content = item.optString("content").trim()
                    if (title.isNotEmpty() && content.isNotEmpty()) {
                        val tagsArr = item.optJSONArray("tags")
                        val tags = mutableListOf<String>()
                        if (tagsArr != null) {
                            for (t in 0 until tagsArr.length()) {
                                val tag = tagsArr.optString(t).trim()
                                if (tag.isNotEmpty()) tags.add(tag)
                            }
                        }
                        extractedMemories.add(
                            ExtractedMemory(
                                title = title,
                                content = content,
                                category = MemoryCategory.fromString(item.optString("category")),
                                importance = item.optInt("importance", 3).coerceIn(1, 5),
                                tags = tags
                            )
                        )
                    }
                }
            }
        }

        val autoObj = root.optJSONObject("saveAutomation")
        val newAutomation = if (autoObj != null && autoObj.optBoolean("shouldSave", false)) {
            val name = autoObj.optString("name").trim()
            val actionType = PhoneActionType.fromString(autoObj.optString("actionType"))
            if (name.isNotEmpty() && actionType != PhoneActionType.NONE) {
                AutomationRule(
                    name = name,
                    triggerDescription = autoObj.optString("triggerDescription")
                        .ifBlank { "On command" },
                    actionType = actionType,
                    target = autoObj.optString("target"),
                    payload = autoObj.optString("payload")
                )
            } else null
        } else null

        val recalledIds = mutableListOf<Long>()
        val recalledArr = root.optJSONArray("recalledMemoryIds")
        if (recalledArr != null) {
            for (i in 0 until recalledArr.length()) {
                val id = recalledArr.optLong(i, -1L)
                if (id > 0L) recalledIds.add(id)
            }
        }

        return JarvisInferenceResult(
            reply = reply,
            actionPlan = actionPlan,
            newMemories = extractedMemories,
            newAutomation = newAutomation,
            recalledMemoryIds = recalledIds
        )
    }
}

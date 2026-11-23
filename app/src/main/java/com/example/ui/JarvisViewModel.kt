package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.JarvisAiEngine
import com.example.automation.PhoneAutomationEngine
import com.example.data.local.JarvisDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.JarvisVaultRepository
import com.example.domain.AssistantTone
import com.example.domain.AutomationRule
import com.example.domain.ChatMessage
import com.example.domain.ChatRole
import com.example.domain.ContactShortcut
import com.example.domain.MemoryCategory
import com.example.domain.MemoryItem
import com.example.domain.PhoneActionPlan
import com.example.domain.PhoneActionType
import com.example.domain.UserPreferences
import com.example.security.CryptoVault
import com.example.security.VaultSecurityInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

sealed interface InferenceUiState {
    data object Idle : InferenceUiState
    data object Thinking : InferenceUiState
    data class Error(val message: String) : InferenceUiState
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "JarvisViewModel"
    }

    private val database = JarvisDatabase.getInstance(application)
    private val vaultRepository = JarvisVaultRepository(database.jarvisDao())
    private val prefsRepository = UserPreferencesRepository(application)
    private val aiEngine = JarvisAiEngine()

    val memories: StateFlow<List<MemoryItem>> = vaultRepository.memories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<ChatMessage>> = vaultRepository.messages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automations: StateFlow<List<AutomationRule>> = vaultRepository.automations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactShortcut>> = vaultRepository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<UserPreferences> = prefsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _vaultSecurity = MutableStateFlow(CryptoVault.inspectSecurity())
    val vaultSecurity: StateFlow<VaultSecurityInfo> = _vaultSecurity.asStateFlow()

    private val _inferenceState = MutableStateFlow<InferenceUiState>(InferenceUiState.Idle)
    val inferenceState: StateFlow<InferenceUiState> = _inferenceState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    fun dismissStatusBanner() {
        _statusBanner.value = null
    }

    fun dismissInferenceError() {
        if (_inferenceState.value is InferenceUiState.Error) {
            _inferenceState.value = InferenceUiState.Idle
        }
    }

    fun sendCommand(rawInput: String) {
        val text = rawInput.trim()
        if (text.isEmpty() || _inferenceState.value == InferenceUiState.Thinking) return

        viewModelScope.launch {
            _inferenceState.value = InferenceUiState.Thinking
            try {
                // Persist encrypted user message immediately
                vaultRepository.appendMessage(
                    ChatMessage(
                        role = ChatRole.USER,
                        text = text
                    )
                )

                val currentMemories = memories.value
                val currentContacts = contacts.value
                val currentAutomations = automations.value
                val currentHistory = messages.value
                val currentPrefs = preferences.value

                val result = aiEngine.processUserCommand(
                    userInput = text,
                    recentMessages = currentHistory,
                    memories = currentMemories,
                    contacts = currentContacts,
                    automations = currentAutomations,
                    preferences = currentPrefs
                )

                // Update lastRecalledAt on referenced memories
                if (result.recalledMemoryIds.isNotEmpty()) {
                    vaultRepository.markMemoriesRecalled(result.recalledMemoryIds)
                }

                // Encrypt and persist any newly extracted memories
                val savedTitles = mutableListOf<String>()
                for (extracted in result.newMemories) {
                    val duplicate = currentMemories.any { existing ->
                        existing.title.equals(extracted.title, ignoreCase = true) &&
                            existing.content.equals(extracted.content, ignoreCase = true)
                    }
                    if (!duplicate) {
                        vaultRepository.saveMemory(
                            MemoryItem(
                                category = extracted.category,
                                title = extracted.title,
                                content = extracted.content,
                                tags = extracted.tags,
                                importance = extracted.importance
                            )
                        )
                        savedTitles.add(extracted.title)
                    }
                }

                // Persist any requested automation shortcut
                result.newAutomation?.let { rule ->
                    vaultRepository.saveAutomation(rule)
                }

                vaultRepository.appendMessage(
                    ChatMessage(
                        role = ChatRole.JARVIS,
                        text = result.reply,
                        actionPlan = result.actionPlan,
                        savedMemoryTitles = savedTitles
                    )
                )

                if (currentPrefs.voiceTtsEnabled && ttsReady) {
                    tts?.speak(result.reply, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
                }
                _inferenceState.value = InferenceUiState.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Jarvis AI request timed out", e)
                _inferenceState.value = InferenceUiState.Error(
                    "Jarvis timed out after 60 seconds. Check your connection and retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Jarvis command failed", e)
                _inferenceState.value = InferenceUiState.Error(
                    e.localizedMessage ?: "Failed to process Jarvis command."
                )
            }
        }
    }

    fun executePhoneAction(
        context: Context,
        messageId: Long?,
        plan: PhoneActionPlan
    ) {
        val result = PhoneAutomationEngine.executeAction(
            context = context,
            plan = plan,
            vaultContacts = contacts.value,
            directCallPreferred = preferences.value.directCallPreferred
        )
        if (!result.success) {
            _statusBanner.value = result.statusMessage
        }
        if (result.success && messageId != null) {
            viewModelScope.launch {
                vaultRepository.markMessageActionExecuted(messageId, plan)
            }
        }
    }

    fun savePlanAsAutomation(plan: PhoneActionPlan, customName: String? = null) {
        viewModelScope.launch {
            val ruleName = customName?.takeIf { it.isNotBlank() }
                ?: "${plan.type.label}: ${plan.target.ifBlank { "Quick Action" }}"
            vaultRepository.saveAutomation(
                AutomationRule(
                    name = ruleName,
                    triggerDescription = "One-tap or voice shortcut",
                    actionType = plan.type,
                    target = plan.target,
                    payload = plan.payload
                )
            )
        }
    }

    fun runAutomation(context: Context, rule: AutomationRule) {
        val plan = PhoneActionPlan(
            type = rule.actionType,
            target = rule.target,
            payload = rule.payload,
            reason = rule.name
        )
        val result = PhoneAutomationEngine.executeAction(
            context = context,
            plan = plan,
            vaultContacts = contacts.value,
            directCallPreferred = preferences.value.directCallPreferred
        )
        if (!result.success) {
            _statusBanner.value = result.statusMessage
        }
        if (result.success) {
            viewModelScope.launch {
                vaultRepository.recordAutomationRun(rule)
            }
        }
    }

    fun saveAutomation(rule: AutomationRule) {
        viewModelScope.launch {
            vaultRepository.saveAutomation(rule)
        }
    }

    fun toggleAutomation(rule: AutomationRule) {
        viewModelScope.launch {
            vaultRepository.toggleAutomationEnabled(rule)
        }
    }

    fun deleteAutomation(id: Long) {
        viewModelScope.launch {
            vaultRepository.deleteAutomation(id)
        }
    }

    fun saveMemory(
        id: Long = 0,
        title: String,
        content: String,
        category: MemoryCategory,
        importance: Int,
        tagsRaw: String
    ) {
        val cleanTitle = title.trim()
        val cleanContent = content.trim()
        if (cleanTitle.isEmpty() || cleanContent.isEmpty()) return

        val tags = tagsRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        viewModelScope.launch {
            vaultRepository.saveMemory(
                MemoryItem(
                    id = id,
                    category = category,
                    title = cleanTitle,
                    content = cleanContent,
                    tags = tags,
                    importance = importance.coerceIn(1, 5)
                )
            )
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            vaultRepository.deleteMemory(id)
        }
    }

    fun saveContactShortcut(
        alias: String,
        displayName: String,
        phoneNumber: String,
        email: String = "",
        notes: String = ""
    ) {
        if (displayName.isBlank()) return
        viewModelScope.launch {
            vaultRepository.saveContact(
                ContactShortcut(
                    alias = alias.trim().ifBlank { displayName.trim().substringBefore(" ") },
                    displayName = displayName.trim(),
                    phoneNumber = phoneNumber.trim(),
                    email = email.trim(),
                    notes = notes.trim()
                )
            )
        }
    }

    fun importPickedContact(context: Context, uri: Uri) {
        val picked = PhoneAutomationEngine.extractPickedPhoneContact(context, uri)
        if (picked != null) {
            viewModelScope.launch {
                vaultRepository.saveContact(picked)
            }
        } else {
            _statusBanner.value = "Could not read phone contact."
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            vaultRepository.deleteContact(id)
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            vaultRepository.clearConversation()
        }
    }

    fun wipeAllVaultData() {
        viewModelScope.launch {
            vaultRepository.wipeAllEncryptedData()
            _vaultSecurity.value = CryptoVault.inspectSecurity()
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch { prefsRepository.setUserName(name) }
    }

    fun setAssistantTone(tone: AssistantTone) {
        viewModelScope.launch { prefsRepository.setAssistantTone(tone) }
    }

    fun setVoiceTtsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setVoiceTtsEnabled(enabled)
            if (!enabled) tts?.stop()
        }
    }

    fun setAutoRememberEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setAutoRememberEnabled(enabled) }
    }

    fun setInjectMemoryContext(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setInjectMemoryContext(enabled) }
    }

    fun setDirectCallPreferred(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setDirectCallPreferred(enabled) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setDarkTheme(enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

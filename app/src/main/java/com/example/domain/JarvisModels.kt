package com.example.domain

enum class MemoryCategory(val label: String) {
    PEOPLE("People"),
    PREFERENCES("Preferences"),
    WORK("Work"),
    HABITS("Habits"),
    FACTS("Facts");

    companion object {
        fun fromString(raw: String): MemoryCategory {
            val normalized = raw.trim().uppercase()
            return entries.firstOrNull {
                it.name == normalized || it.label.uppercase() == normalized
            } ?: FACTS
        }
    }
}

enum class PhoneActionType(val label: String) {
    CALL("Phone Call"),
    SMS("Send Message"),
    EMAIL("Compose Email"),
    ALARM("Set Alarm"),
    TIMER("Start Timer"),
    CALENDAR("Schedule Event"),
    SETTINGS_WIFI("Wi-Fi Settings"),
    SETTINGS_BLUETOOTH("Bluetooth Settings"),
    SETTINGS_SOUND("Sound & DND"),
    SETTINGS_BATTERY("Battery Settings"),
    NONE("Information");

    companion object {
        fun fromString(raw: String): PhoneActionType {
            val normalized = raw.trim().uppercase()
            return entries.firstOrNull { it.name == normalized } ?: NONE
        }
    }
}

enum class AssistantTone(val label: String, val directive: String) {
    TACTICAL(
        label = "Tactical HUD",
        directive = "Speak like a crisp, loyal, hyper-capable Jarvis AI butler: concise, high-signal, proactive, and calm."
    ),
    WARM(
        label = "Warm Personal",
        directive = "Speak warmly and naturally like a thoughtful personal assistant who remembers personal details."
    ),
    EXECUTIVE(
        label = "Executive Brief",
        directive = "Use ultra-brief bulleted executive summaries with zero filler."
    );

    companion object {
        fun fromString(raw: String): AssistantTone =
            entries.firstOrNull { it.name == raw } ?: TACTICAL
    }
}

data class MemoryItem(
    val id: Long = 0,
    val category: MemoryCategory,
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val importance: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRecalledAt: Long = System.currentTimeMillis()
)

data class PhoneActionPlan(
    val type: PhoneActionType,
    val target: String = "",
    val payload: String = "",
    val reason: String = "",
    val executed: Boolean = false
)

enum class ChatRole {
    USER,
    JARVIS
}

data class ChatMessage(
    val id: Long = 0,
    val role: ChatRole,
    val text: String,
    val actionPlan: PhoneActionPlan? = null,
    val savedMemoryTitles: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class AutomationRule(
    val id: Long = 0,
    val name: String,
    val triggerDescription: String,
    val actionType: PhoneActionType,
    val target: String,
    val payload: String,
    val isEnabled: Boolean = true,
    val runCount: Int = 0,
    val lastRunAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ContactShortcut(
    val id: Long = 0,
    val alias: String,
    val displayName: String,
    val phoneNumber: String,
    val email: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class UserPreferences(
    val userName: String = "",
    val assistantTone: AssistantTone = AssistantTone.TACTICAL,
    val voiceTtsEnabled: Boolean = false,
    val autoRememberEnabled: Boolean = true,
    val injectMemoryContext: Boolean = true,
    val directCallPreferred: Boolean = true,
    val darkTheme: Boolean = true
)

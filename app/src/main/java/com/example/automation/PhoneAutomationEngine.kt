package com.example.automation

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.domain.ContactShortcut
import com.example.domain.PhoneActionPlan
import com.example.domain.PhoneActionType

data class ActionExecutionResult(
    val success: Boolean,
    val statusMessage: String,
    val resolvedTarget: String = ""
)

data class ResolvedContact(
    val displayName: String,
    val phoneNumber: String = "",
    val email: String = ""
)

object PhoneAutomationEngine {
    private const val TAG = "PhoneAutomationEngine"

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun resolveTarget(
        context: Context,
        rawTarget: String,
        vaultContacts: List<ContactShortcut>
    ): ResolvedContact {
        val query = rawTarget.trim()
        if (query.isEmpty()) return ResolvedContact("")

        // 1. Direct email or phone number
        if (query.contains("@") && query.contains(".")) {
            return ResolvedContact(displayName = query, email = query)
        }
        val digitsOnly = query.filter { it.isDigit() || it == '+' }
        if (digitsOnly.length >= 3 && query.all { it.isDigit() || it in "+-() ." }) {
            return ResolvedContact(displayName = query, phoneNumber = digitsOnly)
        }

        // 2. Match against Encrypted Local Contact Shortcuts
        val vaultMatch = vaultContacts.firstOrNull { c ->
            c.alias.equals(query, ignoreCase = true) ||
                c.displayName.equals(query, ignoreCase = true) ||
                c.alias.contains(query, ignoreCase = true) ||
                c.displayName.contains(query, ignoreCase = true)
        }
        if (vaultMatch != null) {
            return ResolvedContact(
                displayName = vaultMatch.displayName,
                phoneNumber = vaultMatch.phoneNumber,
                email = vaultMatch.email
            )
        }

        // 3. Query Android ContactsContract if READ_CONTACTS is granted
        if (hasPermission(context, Manifest.permission.READ_CONTACTS)) {
            querySystemContacts(context, query)?.let { return it }
        }

        return ResolvedContact(displayName = query)
    }

    private fun querySystemContacts(context: Context, nameQuery: String): ResolvedContact? {
        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val args = arrayOf("%$nameQuery%")
            context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val displayName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else nameQuery
                    val phone = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                    return ResolvedContact(displayName = displayName, phoneNumber = phone)
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query system contacts", e)
            null
        }
    }

    fun extractPickedPhoneContact(context: Context, contactUri: Uri): ContactShortcut? {
        return try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val displayName =
                        if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else "Contact"
                    val phone = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                    return ContactShortcut(
                        alias = displayName.substringBefore(" "),
                        displayName = displayName,
                        phoneNumber = phone
                    )
                }
            }
            null
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing contact read permission for picked URI", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed extracting picked contact", e)
            null
        }
    }

    fun executeAction(
        context: Context,
        plan: PhoneActionPlan,
        vaultContacts: List<ContactShortcut>,
        directCallPreferred: Boolean
    ): ActionExecutionResult {
        return try {
            when (plan.type) {
                PhoneActionType.CALL -> executeCall(context, plan, vaultContacts, directCallPreferred)
                PhoneActionType.SMS -> executeSms(context, plan, vaultContacts)
                PhoneActionType.EMAIL -> executeEmail(context, plan, vaultContacts)
                PhoneActionType.ALARM -> executeAlarm(context, plan)
                PhoneActionType.TIMER -> executeTimer(context, plan)
                PhoneActionType.CALENDAR -> executeCalendar(context, plan)
                PhoneActionType.SETTINGS_WIFI ->
                    launchIntent(context, Intent(Settings.ACTION_WIFI_SETTINGS), "Opened Wi-Fi settings")
                PhoneActionType.SETTINGS_BLUETOOTH ->
                    launchIntent(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Opened Bluetooth settings")
                PhoneActionType.SETTINGS_SOUND ->
                    launchIntent(context, Intent(Settings.ACTION_SOUND_SETTINGS), "Opened Sound & Do Not Disturb settings")
                PhoneActionType.SETTINGS_BATTERY ->
                    launchIntent(context, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS), "Opened Battery Saver settings")
                PhoneActionType.NONE ->
                    ActionExecutionResult(false, "No executable device action attached.")
            }
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No activity found to handle ${plan.type}", e)
            ActionExecutionResult(
                false,
                "No compatible system app found on this device for ${plan.type.label}."
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission rejected for ${plan.type}", e)
            ActionExecutionResult(
                false,
                "Permission required to complete ${plan.type.label}. Opening fallback or grant permission in Vault."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed executing action ${plan.type}", e)
            ActionExecutionResult(false, e.localizedMessage ?: "Failed to launch action.")
        }
    }

    private fun executeCall(
        context: Context,
        plan: PhoneActionPlan,
        vaultContacts: List<ContactShortcut>,
        directCallPreferred: Boolean
    ): ActionExecutionResult {
        val resolved = resolveTarget(context, plan.target, vaultContacts)
        val number = resolved.phoneNumber
        if (number.isBlank()) {
            // Open dialer without number or inform user to attach contact
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return ActionExecutionResult(
                success = true,
                statusMessage = "No phone number found for '${plan.target}'. Opened Dialer — pick or save the contact in Automate to call by name.",
                resolvedTarget = plan.target
            )
        }

        val canDirectCall = directCallPreferred && hasPermission(context, Manifest.permission.CALL_PHONE)
        val action = if (canDirectCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val intent = Intent(action, Uri.parse("tel:${Uri.encode(number)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        val verb = if (canDirectCall) "Calling" else "Dialing"
        return ActionExecutionResult(
            success = true,
            statusMessage = "$verb ${resolved.displayName} ($number).",
            resolvedTarget = number
        )
    }

    private fun executeSms(
        context: Context,
        plan: PhoneActionPlan,
        vaultContacts: List<ContactShortcut>
    ): ActionExecutionResult {
        val resolved = resolveTarget(context, plan.target, vaultContacts)
        val targetNumber = resolved.phoneNumber
        val smsUri = if (targetNumber.isNotBlank()) {
            Uri.parse("smsto:${Uri.encode(targetNumber)}")
        } else {
            Uri.parse("smsto:")
        }
        val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", plan.payload)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        val label = if (targetNumber.isNotBlank()) "${resolved.displayName} ($targetNumber)" else "recipient selection"
        return ActionExecutionResult(
            success = true,
            statusMessage = "Prepared SMS to $label.",
            resolvedTarget = targetNumber
        )
    }

    private fun executeEmail(
        context: Context,
        plan: PhoneActionPlan,
        vaultContacts: List<ContactShortcut>
    ): ActionExecutionResult {
        val resolved = resolveTarget(context, plan.target, vaultContacts)
        val emailAddress = resolved.email.ifBlank {
            if (plan.target.contains("@")) plan.target else ""
        }
        val uri = Uri.parse("mailto:${Uri.encode(emailAddress)}")
        val parts = plan.payload.split("|", limit = 2)
        val subject = if (parts.size == 2) parts[0].trim() else "Message from Jarvis"
        val body = if (parts.size == 2) parts[1].trim() else plan.payload
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionExecutionResult(
            success = true,
            statusMessage = "Opened email composer${if (emailAddress.isNotBlank()) " for $emailAddress" else ""}.",
            resolvedTarget = emailAddress
        )
    }

    private fun executeAlarm(context: Context, plan: PhoneActionPlan): ActionExecutionResult {
        // Expect target or payload like "07:30" or "7:00"
        val timeRegex = Regex("""(\d{1,2}):(\d{2})""")
        val match = timeRegex.find("${plan.target} ${plan.payload}")
        val hour = match?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val minute = match?.groupValues?.get(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val label = plan.payload.ifBlank { "Jarvis Alarm" }
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionExecutionResult(
            success = true,
            statusMessage = "Configured alarm for %02d:%02d (%s).".format(hour, minute, label),
            resolvedTarget = "%02d:%02d".format(hour, minute)
        )
    }

    private fun executeTimer(context: Context, plan: PhoneActionPlan): ActionExecutionResult {
        val seconds = plan.target.filter { it.isDigit() }.toIntOrNull()
            ?: plan.payload.filter { it.isDigit() }.toIntOrNull()?.times(60)
            ?: 300
        val label = plan.payload.ifBlank { "Jarvis Timer" }
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionExecutionResult(
            success = true,
            statusMessage = "Started ${seconds}s timer ($label).",
            resolvedTarget = "${seconds}s"
        )
    }

    private fun executeCalendar(context: Context, plan: PhoneActionPlan): ActionExecutionResult {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, plan.target.ifBlank { "Jarvis Scheduled Event" })
            putExtra(CalendarContract.Events.DESCRIPTION, plan.payload)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + 3_600_000L)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, System.currentTimeMillis() + 7_200_000L)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ActionExecutionResult(
            success = true,
            statusMessage = "Opened calendar event draft: ${plan.target.ifBlank { "Event" }}.",
            resolvedTarget = plan.target
        )
    }

    private fun launchIntent(context: Context, intent: Intent, successMsg: String): ActionExecutionResult {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ActionExecutionResult(true, successMsg)
    }
}

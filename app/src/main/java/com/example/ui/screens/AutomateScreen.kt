package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.AutomationRule
import com.example.domain.ContactShortcut
import com.example.domain.PhoneActionPlan
import com.example.domain.PhoneActionType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutomateScreen(
    automations: List<AutomationRule>,
    contacts: List<ContactShortcut>,
    onQuickAction: (PhoneActionPlan) -> Unit,
    onRunAutomation: (AutomationRule) -> Unit,
    onToggleAutomation: (AutomationRule) -> Unit,
    onSaveAutomation: (AutomationRule) -> Unit,
    onDeleteAutomation: (Long) -> Unit,
    onSaveContact: (alias: String, displayName: String, phone: String, email: String, notes: String) -> Unit,
    onImportContactUri: (Uri) -> Unit,
    onDeleteContact: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddRuleInline by remember { mutableStateOf(false) }
    var showAddContactInline by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> onImportContactUri(uri) }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Automate",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Phone bridges, encrypted workflows, and contact shortcuts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Compact 4-item Quick Bridges Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickBridgeChip(
                        icon = Icons.Default.Call,
                        label = "Dialer",
                        onClick = { onQuickAction(PhoneActionPlan(PhoneActionType.CALL)) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickBridgeChip(
                        icon = Icons.Default.Sms,
                        label = "SMS",
                        onClick = { onQuickAction(PhoneActionPlan(PhoneActionType.SMS)) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickBridgeChip(
                        icon = Icons.Default.Alarm,
                        label = "Alarm",
                        onClick = {
                            onQuickAction(PhoneActionPlan(PhoneActionType.ALARM, "07:00", "Alarm"))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickBridgeChip(
                        icon = Icons.Default.Wifi,
                        label = "Wi-Fi",
                        onClick = { onQuickAction(PhoneActionPlan(PhoneActionType.SETTINGS_WIFI)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Workflows Header + Inline Toggle
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "WORKFLOWS (${automations.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { showAddRuleInline = !showAddRuleInline },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_automation_button")
                    ) {
                        Icon(
                            imageVector = if (showAddRuleInline) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showAddRuleInline) "Close" else "New Rule")
                    }
                }

                AnimatedVisibility(visible = showAddRuleInline) {
                    InlineAutomationComposer(
                        onSave = { rule ->
                            onSaveAutomation(rule)
                            showAddRuleInline = false
                        }
                    )
                }
            }

            if (automations.isEmpty() && !showAddRuleInline) {
                item {
                    Text(
                        text = "No workflows yet. Tap 'New Rule' or ask Jarvis to save one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(automations, key = { "auto_${it.id}" }) { rule ->
                    AutomationRuleCard(
                        rule = rule,
                        onRun = { onRunAutomation(rule) },
                        onToggle = { onToggleAutomation(rule) },
                        onDelete = { onDeleteAutomation(rule.id) }
                    )
                }
            }

            // Contacts Header + Inline Toggle
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CONTACTS (${contacts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        TextButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_PICK,
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                )
                                contactPickerLauncher.launch(intent)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("import_contact_button")
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import")
                        }
                        TextButton(
                            onClick = { showAddContactInline = !showAddContactInline },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("add_contact_button")
                        ) {
                            Icon(
                                imageVector = if (showAddContactInline) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showAddContactInline) "Close" else "Add")
                        }
                    }
                }

                AnimatedVisibility(visible = showAddContactInline) {
                    InlineContactComposer(
                        onSave = { alias, name, phone, email ->
                            onSaveContact(alias, name, phone, email, "")
                            showAddContactInline = false
                        }
                    )
                }
            }

            if (contacts.isEmpty() && !showAddContactInline) {
                item {
                    Text(
                        text = "Add or import contacts so Jarvis can call or message them by name.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(contacts, key = { "contact_${it.id}" }) { contact ->
                    ContactShortcutCard(
                        contact = contact,
                        onCall = {
                            onQuickAction(
                                PhoneActionPlan(
                                    type = PhoneActionType.CALL,
                                    target = contact.phoneNumber.ifBlank { contact.displayName }
                                )
                            )
                        },
                        onSms = {
                            onQuickAction(
                                PhoneActionPlan(
                                    type = PhoneActionType.SMS,
                                    target = contact.phoneNumber.ifBlank { contact.displayName },
                                    payload = ""
                                )
                            )
                        },
                        onDelete = { onDeleteContact(contact.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickBridgeChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AutomationRuleCard(
    rule: AutomationRule,
    onRun: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = actionIconFor(rule.actionType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${rule.actionType.label} • ${rule.target.ifBlank { "Quick action" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("automation_switch_${rule.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Runs: ${rule.runCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete automation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onRun,
                        enabled = rule.isEnabled,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("run_automation_${rule.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactShortcutCard(
    contact: ContactShortcut,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (contact.alias.isNotBlank() && !contact.alias.equals(contact.displayName, true)) {
                        "${contact.displayName} (${contact.alias})"
                    } else {
                        contact.displayName
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = listOf(contact.phoneNumber, contact.email).filter { it.isNotBlank() }
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCall, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Default.Call, contentDescription = "Call ${contact.displayName}", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onSms, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Default.Sms, contentDescription = "Message ${contact.displayName}", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove contact", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineAutomationComposer(
    onSave: (AutomationRule) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(PhoneActionType.SMS) }
    var target by remember { mutableStateOf("") }
    var payload by remember { mutableStateOf("") }

    val availableTypes = listOf(
        PhoneActionType.CALL,
        PhoneActionType.SMS,
        PhoneActionType.EMAIL,
        PhoneActionType.ALARM,
        PhoneActionType.TIMER,
        PhoneActionType.SETTINGS_WIFI
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Workflow Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                availableTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.label) }
                    )
                }
            }
            OutlinedTextField(
                value = target,
                onValueChange = { target = it },
                label = { Text("Target (Name / Number / HH:MM)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it },
                label = { Text("Message or Label") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            AutomationRule(
                                name = name.trim(),
                                triggerDescription = "Manual / Voice",
                                actionType = selectedType,
                                target = target.trim(),
                                payload = payload.trim()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Workflow")
            }
        }
    }
}

@Composable
private fun InlineContactComposer(
    onSave: (alias: String, name: String, phone: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text("Alias (e.g. Mom, Boss)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSave(alias, name, phone, email) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Contact")
            }
        }
    }
}

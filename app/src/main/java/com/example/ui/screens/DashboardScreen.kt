package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.MemoryItem
import com.example.domain.PhoneActionPlan
import com.example.domain.PhoneActionType
import com.example.domain.UserPreferences
import com.example.security.VaultSecurityInfo
import com.example.ui.theme.JarvisCoreTheme
import com.example.ui.theme.LocalJarvisSpacing
import java.util.Calendar

@Composable
fun DashboardScreen(
    preferences: UserPreferences,
    memories: List<MemoryItem>,
    automationCount: Int,
    contactCount: Int,
    vaultSecurity: VaultSecurityInfo,
    onOpenAssistant: () -> Unit,
    onOpenAutomate: () -> Unit,
    onOpenMemoryVault: () -> Unit,
    onOpenVaultSettings: () -> Unit,
    onSubmitQuickCommand: (String) -> Unit,
    onQuickPhoneAction: (PhoneActionPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalJarvisSpacing.current
    var quickPrompt by rememberSaveable { mutableStateOf("") }

    val timeGreeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            // Minimal Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "JARVIS CORE • ${vaultSecurity.algorithm}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (preferences.userName.isNotBlank()) {
                            "$timeGreeting, ${preferences.userName}."
                        } else {
                            "$timeGreeting."
                        },
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Local memory encrypted. Ready for commands.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Command Input
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quickPrompt,
                        onValueChange = { quickPrompt = it },
                        placeholder = {
                            Text(
                                text = "Ask Jarvis to call, message, or remember…",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (quickPrompt.isNotBlank()) {
                                        onSubmitQuickCommand(quickPrompt)
                                        quickPrompt = ""
                                    }
                                },
                                enabled = quickPrompt.isNotBlank(),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_quick_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.cd_send_command),
                                    tint = if (quickPrompt.isNotBlank()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (quickPrompt.isNotBlank()) {
                                    onSubmitQuickCommand(quickPrompt)
                                    quickPrompt = ""
                                }
                            }
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_quick_prompt_input")
                    )

                    Button(
                        onClick = onOpenAssistant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("dashboard_open_assistant_button")
                    ) {
                        Text("Open Console")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Minimal Monochrome Stats Strip
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                    ) {
                        MinimalStatCell(
                            label = "MEMORIES",
                            value = "${memories.size}",
                            onClick = onOpenMemoryVault,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_memory_tile")
                        )
                        MinimalStatCell(
                            label = "WORKFLOWS",
                            value = "$automationCount",
                            onClick = onOpenAutomate,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_automate_tile")
                        )
                        MinimalStatCell(
                            label = "CONTACTS",
                            value = "$contactCount",
                            onClick = onOpenVaultSettings,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_vault_tile")
                        )
                    }
                }
            }

            // Quick Phone Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DEVICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MinimalActionButton(
                            icon = Icons.Default.Call,
                            label = "Call",
                            onClick = { onQuickPhoneAction(PhoneActionPlan(PhoneActionType.CALL)) },
                            modifier = Modifier.weight(1f)
                        )
                        MinimalActionButton(
                            icon = Icons.Default.Sms,
                            label = "Text",
                            onClick = { onQuickPhoneAction(PhoneActionPlan(PhoneActionType.SMS)) },
                            modifier = Modifier.weight(1f)
                        )
                        MinimalActionButton(
                            icon = Icons.Default.Alarm,
                            label = "Alarm",
                            onClick = {
                                onQuickPhoneAction(
                                    PhoneActionPlan(PhoneActionType.ALARM, "07:00", "Alarm")
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        MinimalActionButton(
                            icon = Icons.Default.Wifi,
                            label = "Wi-Fi",
                            onClick = {
                                onQuickPhoneAction(PhoneActionPlan(PhoneActionType.SETTINGS_WIFI))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Latest Recalled Context (Clean Flat Section)
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))
                val latestMemory = memories.firstOrNull()
                Text(
                    text = if (latestMemory != null) "RECALLED" else "BRIEFING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = latestMemory?.title ?: "No saved context yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = latestMemory?.content
                        ?: "Tell Jarvis about your schedule, contacts, or preferences to build your encrypted local memory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MinimalStatCell(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MinimalActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
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

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    JarvisCoreTheme(darkTheme = true) {
        DashboardScreen(
            preferences = UserPreferences(userName = "Alex"),
            memories = emptyList(),
            automationCount = 2,
            contactCount = 3,
            vaultSecurity = VaultSecurityInfo(
                keyAlias = "jarvis_memory_master_key_v1",
                algorithm = "AES-256-GCM",
                provider = "AndroidKeyStore",
                isHardwareBacked = true,
                isOperational = true
            ),
            onOpenAssistant = {},
            onOpenAutomate = {},
            onOpenMemoryVault = {},
            onOpenVaultSettings = {},
            onSubmitQuickCommand = {},
            onQuickPhoneAction = {}
        )
    }
}

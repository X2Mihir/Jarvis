package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.automation.PhoneAutomationEngine
import com.example.domain.AssistantTone
import com.example.domain.UserPreferences
import com.example.security.VaultSecurityInfo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VaultSettingsScreen(
    preferences: UserPreferences,
    vaultSecurity: VaultSecurityInfo,
    memoryCount: Int,
    automationCount: Int,
    contactCount: Int,
    onSetUserName: (String) -> Unit,
    onSetTone: (AssistantTone) -> Unit,
    onSetTts: (Boolean) -> Unit,
    onSetAutoRemember: (Boolean) -> Unit,
    onSetInjectContext: (Boolean) -> Unit,
    onSetDirectCall: (Boolean) -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onWipeAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nameDraft by remember(preferences.userName) { mutableStateOf(preferences.userName) }
    var permissionRefreshKey by remember { mutableIntStateOf(0) }
    var confirmWipeInline by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionRefreshKey++
    }

    val hasCallPerm = remember(permissionRefreshKey) {
        PhoneAutomationEngine.hasPermission(context, Manifest.permission.CALL_PHONE)
    }
    val hasContactsPerm = remember(permissionRefreshKey) {
        PhoneAutomationEngine.hasPermission(context, Manifest.permission.READ_CONTACTS)
    }
    val hasAudioPerm = remember(permissionRefreshKey) {
        PhoneAutomationEngine.hasPermission(context, Manifest.permission.RECORD_AUDIO)
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Vault & Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${vaultSecurity.algorithm} • ${vaultSecurity.provider}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Personalization
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ASSISTANT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it },
                            label = { Text("Your Name") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("user_name_input")
                        )
                        OutlinedButton(
                            onClick = { onSetUserName(nameDraft) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Save")
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistantTone.entries.forEach { tone ->
                            FilterChip(
                                selected = preferences.assistantTone == tone,
                                onClick = { onSetTone(tone) },
                                label = { Text(tone.label) }
                            )
                        }
                    }

                    SettingToggleRow(
                        title = "Voice Replies (TTS)",
                        checked = preferences.voiceTtsEnabled,
                        onCheckedChange = onSetTts
                    )
                    SettingToggleRow(
                        title = "Auto-Remember Facts",
                        checked = preferences.autoRememberEnabled,
                        onCheckedChange = onSetAutoRemember
                    )
                    SettingToggleRow(
                        title = "Use Local Memory Context",
                        checked = preferences.injectMemoryContext,
                        onCheckedChange = onSetInjectContext
                    )
                    SettingToggleRow(
                        title = "Direct Calling",
                        checked = preferences.directCallPreferred,
                        onCheckedChange = onSetDirectCall
                    )
                    SettingToggleRow(
                        title = "Dark Mode (Black & White)",
                        checked = preferences.darkTheme,
                        onCheckedChange = onSetDarkTheme
                    )
                }
            }

            // Permissions
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PERMISSIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PermissionBridgeRow(
                        title = "Phone Calls",
                        granted = hasCallPerm,
                        onGrantClick = { permissionLauncher.launch(Manifest.permission.CALL_PHONE) }
                    )
                    PermissionBridgeRow(
                        title = "Contacts",
                        granted = hasContactsPerm,
                        onGrantClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                    )
                    PermissionBridgeRow(
                        title = "Microphone",
                        granted = hasAudioPerm,
                        onGrantClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                }
            }

            // Encryption Audit & Inline Wipe Confirmation (no pop-up dialog)
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "LOCAL STORAGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$memoryCount memories • $automationCount workflows • $contactCount contacts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${vaultSecurity.algorithm} (${vaultSecurity.provider})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (!confirmWipeInline) {
                            OutlinedButton(
                                onClick = { confirmWipeInline = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("wipe_vault_button")
                            ) {
                                Text("Erase Local Encrypted Data")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { confirmWipeInline = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        onWipeAllData()
                                        confirmWipeInline = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Confirm Erase")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PermissionBridgeRow(
    title: String,
    granted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (granted) {
            Text(
                text = "Granted",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            OutlinedButton(
                onClick = onGrantClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("Allow")
            }
        }
    }
}

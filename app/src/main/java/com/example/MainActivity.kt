package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.ui.JarvisViewModel
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.AutomateScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MemoryVaultScreen
import com.example.ui.screens.VaultSettingsScreen
import com.example.ui.theme.JarvisCoreTheme

private enum class JarvisDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(
        route = "dashboard",
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_dashboard"
    ),
    ASSISTANT(
        route = "assistant",
        labelRes = R.string.nav_assistant,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_assistant"
    ),
    AUTOMATE(
        route = "automate",
        labelRes = R.string.nav_automate,
        selectedIcon = Icons.Filled.Bolt,
        unselectedIcon = Icons.Outlined.Bolt,
        testTag = "nav_automate"
    ),
    MEMORY(
        route = "memory",
        labelRes = R.string.nav_memory,
        selectedIcon = Icons.Filled.Lock,
        unselectedIcon = Icons.Outlined.Lock,
        testTag = "nav_memory"
    ),
    VAULT(
        route = "vault",
        labelRes = R.string.nav_vault,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_vault"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            JarvisCoreApp()
        }
    }
}

@Composable
fun JarvisCoreApp(viewModel: JarvisViewModel = viewModel()) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: JarvisDestination.DASHBOARD.route

    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val automations by viewModel.automations.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val vaultSecurity by viewModel.vaultSecurity.collectAsStateWithLifecycle()
    val inferenceState by viewModel.inferenceState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusBanner()
        }
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    JarvisCoreTheme(darkTheme = preferences.darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavRail) {
                            androidx.compose.foundation.layout.Column {
                                androidx.compose.material3.HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.background
                                ) {
                                    JarvisDestination.entries.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTo(dest.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = stringResource(dest.labelRes)
                                                )
                                            },
                                            label = { Text(stringResource(dest.labelRes)) },
                                            modifier = Modifier.testTag(dest.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .imePadding()
                    ) {
                        if (useNavRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                JarvisDestination.entries.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTo(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = stringResource(dest.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(dest.labelRes)) },
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = JarvisDestination.DASHBOARD.route,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(JarvisDestination.DASHBOARD.route) {
                                DashboardScreen(
                                    preferences = preferences,
                                    memories = memories,
                                    automationCount = automations.size,
                                    contactCount = contacts.size,
                                    vaultSecurity = vaultSecurity,
                                    onOpenAssistant = { navigateTo(JarvisDestination.ASSISTANT.route) },
                                    onOpenAutomate = { navigateTo(JarvisDestination.AUTOMATE.route) },
                                    onOpenMemoryVault = { navigateTo(JarvisDestination.MEMORY.route) },
                                    onOpenVaultSettings = { navigateTo(JarvisDestination.VAULT.route) },
                                    onSubmitQuickCommand = { prompt ->
                                        viewModel.sendCommand(prompt)
                                        navigateTo(JarvisDestination.ASSISTANT.route)
                                    },
                                    onQuickPhoneAction = { plan ->
                                        viewModel.executePhoneAction(context, null, plan)
                                    }
                                )
                            }

                            composable(JarvisDestination.ASSISTANT.route) {
                                AssistantScreen(
                                    messages = messages,
                                    memoryCount = memories.size,
                                    preferences = preferences,
                                    inferenceState = inferenceState,
                                    onSendCommand = viewModel::sendCommand,
                                    onExecuteAction = { messageId, plan ->
                                        viewModel.executePhoneAction(context, messageId, plan)
                                    },
                                    onSaveAsAutomation = { plan ->
                                        viewModel.savePlanAsAutomation(plan)
                                    },
                                    onToggleTts = viewModel::setVoiceTtsEnabled,
                                    onClearChat = viewModel::clearConversation,
                                    onDismissError = viewModel::dismissInferenceError,
                                    onImportContactUri = { uri ->
                                        viewModel.importPickedContact(context, uri)
                                    },
                                    onShowBanner = { msg ->
                                        viewModel.showBanner(msg)
                                    }
                                )
                            }

                            composable(JarvisDestination.AUTOMATE.route) {
                                AutomateScreen(
                                    automations = automations,
                                    contacts = contacts,
                                    onQuickAction = { plan ->
                                        viewModel.executePhoneAction(context, null, plan)
                                    },
                                    onRunAutomation = { rule ->
                                        viewModel.runAutomation(context, rule)
                                    },
                                    onToggleAutomation = viewModel::toggleAutomation,
                                    onSaveAutomation = viewModel::saveAutomation,
                                    onDeleteAutomation = viewModel::deleteAutomation,
                                    onSaveContact = viewModel::saveContactShortcut,
                                    onImportContactUri = { uri ->
                                        viewModel.importPickedContact(context, uri)
                                    },
                                    onDeleteContact = viewModel::deleteContact
                                )
                            }

                            composable(JarvisDestination.MEMORY.route) {
                                MemoryVaultScreen(
                                    memories = memories,
                                    vaultSecurity = vaultSecurity,
                                    onSaveMemory = viewModel::saveMemory,
                                    onDeleteMemory = viewModel::deleteMemory
                                )
                            }

                            composable(JarvisDestination.VAULT.route) {
                                VaultSettingsScreen(
                                    preferences = preferences,
                                    vaultSecurity = vaultSecurity,
                                    memoryCount = memories.size,
                                    automationCount = automations.size,
                                    contactCount = contacts.size,
                                    onSetUserName = viewModel::setUserName,
                                    onSetTone = viewModel::setAssistantTone,
                                    onSetTts = viewModel::setVoiceTtsEnabled,
                                    onSetAutoRemember = viewModel::setAutoRememberEnabled,
                                    onSetInjectContext = viewModel::setInjectMemoryContext,
                                    onSetDirectCall = viewModel::setDirectCallPreferred,
                                    onSetDarkTheme = viewModel::setDarkTheme,
                                    onWipeAllData = viewModel::wipeAllVaultData
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

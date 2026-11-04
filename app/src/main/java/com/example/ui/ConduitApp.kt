package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BeeperModalDrawerContent
import com.example.ui.components.BeeperPermanentSidebar
import com.example.ui.components.NewConversationDialog
import com.example.ui.screens.BridgesScreen
import com.example.ui.screens.ChatThreadPane
import com.example.ui.screens.EmptyThreadPlaceholder
import com.example.ui.screens.SecurityVaultScreen
import com.example.ui.screens.UnifiedInboxPane
import kotlinx.coroutines.launch

@Composable
fun ConduitApp(
    viewModel: ConduitViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedConversationId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val activeConversation by viewModel.activeConversation.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()

    var showNewChatDialog by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= 860.dp
        val isCompactThreadOpen = !isExpandedLayout &&
            currentTab == ConduitTab.INBOX &&
            activeConversation != null

        if (drawerState.isOpen) {
            BackHandler {
                coroutineScope.launch { drawerState.close() }
            }
        } else if (currentTab != ConduitTab.INBOX) {
            BackHandler {
                viewModel.selectTab(ConduitTab.INBOX)
            }
        }

        if (isExpandedLayout) {
            // Beeper Desktop/Tablet 3-Pane Workspace: Spaces Sidebar + Inbox Column + Chat Thread
            Scaffold(
                contentWindowInsets = WindowInsets.statusBars,
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    BeeperPermanentSidebar(
                        uiState = uiState,
                        currentTab = currentTab,
                        onSelectSpace = viewModel::selectSpace,
                        onSelectNetwork = viewModel::setNetworkFilter,
                        onSelectTab = viewModel::selectTab
                    )

                    VerticalDivider(color = MaterialTheme.colorScheme.outline)

                    when (currentTab) {
                        ConduitTab.INBOX -> {
                            Row(modifier = Modifier.fillMaxSize()) {
                                UnifiedInboxPane(
                                    uiState = uiState,
                                    selectedConversationId = selectedConversationId,
                                    showDrawerMenuButton = false,
                                    onOpenDrawer = {},
                                    onConversationClick = viewModel::openConversation,
                                    onToggleArchive = viewModel::toggleArchive,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onToggleUnreadOnly = viewModel::toggleUnreadOnly,
                                    onNetworkFilterSelect = viewModel::setNetworkFilter,
                                    onSpaceSelect = viewModel::selectSpace,
                                    onNewChatClick = { showNewChatDialog = true },
                                    modifier = Modifier
                                        .width(380.dp)
                                        .fillMaxHeight()
                                )

                                VerticalDivider(color = MaterialTheme.colorScheme.outline)

                                val currentConv = activeConversation
                                if (currentConv != null) {
                                    ChatThreadPane(
                                        conversation = currentConv,
                                        messages = activeMessages,
                                        safetyNumber = viewModel.computeSafetyNumber(
                                            currentConv.contactHandle,
                                            currentConv.network
                                        ),
                                        showBackButton = false,
                                        onBack = viewModel::closeConversation,
                                        onSendMessage = viewModel::sendMessage,
                                        onSimulateReply = viewModel::receiveTestBridgeReply,
                                        onTogglePin = { viewModel.togglePin(currentConv) },
                                        onToggleArchive = { viewModel.toggleArchive(currentConv) },
                                        onToggleMute = { viewModel.toggleMute(currentConv) },
                                        onMarkVerified = { viewModel.markConversationVerified(currentConv.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    EmptyThreadPlaceholder(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        ConduitTab.BRIDGES -> {
                            BridgesScreen(
                                bridges = uiState.bridges,
                                showDrawerMenuButton = false,
                                onOpenDrawer = {},
                                onBackToInbox = { viewModel.selectTab(ConduitTab.INBOX) },
                                onUpdateBridge = viewModel::updateBridge,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        ConduitTab.SECURITY -> {
                            SecurityVaultScreen(
                                uiState = uiState,
                                showDrawerMenuButton = false,
                                onOpenDrawer = {},
                                onBackToInbox = { viewModel.selectTab(ConduitTab.INBOX) },
                                onSelectDisplayStyle = viewModel::setDisplayStyle,
                                onToggleDarkMode = viewModel::setDarkMode,
                                onToggleMaskPreviews = viewModel::setMaskInboxPreviews,
                                onRunIntegrityAudit = viewModel::runVaultIntegrityAudit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        } else {
            // Beeper Mobile Layout: Collapsible Spaces Navigation Drawer + Full-Bleed Inbox & Bottom Search Dock
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = !isCompactThreadOpen,
                drawerContent = {
                    BeeperModalDrawerContent(
                        uiState = uiState,
                        currentTab = currentTab,
                        onSelectSpace = { space ->
                            viewModel.selectSpace(space)
                            coroutineScope.launch { drawerState.close() }
                        },
                        onSelectNetwork = { network ->
                            viewModel.setNetworkFilter(network)
                            coroutineScope.launch { drawerState.close() }
                        },
                        onSelectTab = { tab ->
                            viewModel.selectTab(tab)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            ) {
                Scaffold(
                    contentWindowInsets = WindowInsets.statusBars,
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    when (currentTab) {
                        ConduitTab.INBOX -> {
                            val currentConv = activeConversation
                            if (currentConv != null) {
                                ChatThreadPane(
                                    conversation = currentConv,
                                    messages = activeMessages,
                                    safetyNumber = viewModel.computeSafetyNumber(
                                        currentConv.contactHandle,
                                        currentConv.network
                                    ),
                                    showBackButton = true,
                                    onBack = viewModel::closeConversation,
                                    onSendMessage = viewModel::sendMessage,
                                    onSimulateReply = viewModel::receiveTestBridgeReply,
                                    onTogglePin = { viewModel.togglePin(currentConv) },
                                    onToggleArchive = { viewModel.toggleArchive(currentConv) },
                                    onToggleMute = { viewModel.toggleMute(currentConv) },
                                    onMarkVerified = { viewModel.markConversationVerified(currentConv.id) },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                )
                            } else {
                                UnifiedInboxPane(
                                    uiState = uiState,
                                    selectedConversationId = selectedConversationId,
                                    showDrawerMenuButton = true,
                                    onOpenDrawer = {
                                        coroutineScope.launch { drawerState.open() }
                                    },
                                    onConversationClick = viewModel::openConversation,
                                    onToggleArchive = viewModel::toggleArchive,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onToggleUnreadOnly = viewModel::toggleUnreadOnly,
                                    onNetworkFilterSelect = viewModel::setNetworkFilter,
                                    onSpaceSelect = viewModel::selectSpace,
                                    onNewChatClick = { showNewChatDialog = true },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                )
                            }
                        }

                        ConduitTab.BRIDGES -> {
                            BridgesScreen(
                                bridges = uiState.bridges,
                                showDrawerMenuButton = true,
                                onOpenDrawer = {
                                    coroutineScope.launch { drawerState.open() }
                                },
                                onBackToInbox = { viewModel.selectTab(ConduitTab.INBOX) },
                                onUpdateBridge = viewModel::updateBridge,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            )
                        }

                        ConduitTab.SECURITY -> {
                            SecurityVaultScreen(
                                uiState = uiState,
                                showDrawerMenuButton = true,
                                onOpenDrawer = {
                                    coroutineScope.launch { drawerState.open() }
                                },
                                onBackToInbox = { viewModel.selectTab(ConduitTab.INBOX) },
                                onSelectDisplayStyle = viewModel::setDisplayStyle,
                                onToggleDarkMode = viewModel::setDarkMode,
                                onToggleMaskPreviews = viewModel::setMaskInboxPreviews,
                                onRunIntegrityAudit = viewModel::runVaultIntegrityAudit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        NewConversationDialog(
            onDismiss = { showNewChatDialog = false },
            onCreate = { name, handle, network, initialMsg ->
                viewModel.createConversation(name, handle, network, initialMsg)
                showNewChatDialog = false
            }
        )
    }
}

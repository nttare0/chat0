package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.BridgeAccountEntity
import com.example.data.SocialNetwork
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.components.icon

@Composable
fun BridgesScreen(
    bridges: List<BridgeAccountEntity>,
    showDrawerMenuButton: Boolean,
    onOpenDrawer: () -> Unit,
    onBackToInbox: () -> Unit,
    onUpdateBridge: (network: SocialNetwork, handle: String, connected: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingNetwork by remember { mutableStateOf<SocialNetwork?>(null) }
    val bridgeMap = remember(bridges) { bridges.associateBy { it.networkId } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showDrawerMenuButton) {
                    IconButton(onClick = onBackToInbox) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_action)
                        )
                    }
                } else {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = stringResource(R.string.open_spaces_drawer)
                        )
                    }
                }

                Column {
                    Text(
                        text = stringResource(R.string.bridges_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.bridges_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("bridges_list"),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = SocialNetwork.entries,
                    key = { it.id }
                ) { network ->
                    val bridge = bridgeMap[network.id]
                    val isConnected = bridge?.isConnected == true
                    val handle = bridge?.accountHandle.orEmpty()

                    BridgeNetworkCard(
                        network = network,
                        accountHandle = handle,
                        isConnected = isConnected,
                        lastSyncedMs = bridge?.lastSyncedEpochMs ?: 0L,
                        onToggleConnected = { wantConnected ->
                            if (wantConnected && handle.isBlank()) {
                                editingNetwork = network
                            } else {
                                onUpdateBridge(network, handle.ifBlank { "@connected" }, wantConnected)
                            }
                        },
                        onConfigureClick = { editingNetwork = network }
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    editingNetwork?.let { network ->
        val existing = bridgeMap[network.id]
        ConfigureBridgeDialog(
            network = network,
            initialHandle = existing?.accountHandle.orEmpty(),
            isCurrentlyConnected = existing?.isConnected == true,
            onDismiss = { editingNetwork = null },
            onSave = { newHandle, connected ->
                onUpdateBridge(network, newHandle, connected)
                editingNetwork = null
            }
        )
    }
}

@Composable
private fun BridgeNetworkCard(
    network: SocialNetwork,
    accountHandle: String,
    isConnected: Boolean,
    lastSyncedMs: Long,
    onToggleConnected: (Boolean) -> Unit,
    onConfigureClick: () -> Unit
) {
    val cardBorder = if (isConnected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bridge_card_${network.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isConnected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = network.icon(),
                                contentDescription = network.displayName,
                                tint = if (isConnected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = network.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isConnected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = if (isConnected) {
                                        stringResource(R.string.bridge_connected)
                                    } else {
                                        stringResource(R.string.bridge_paused)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isConnected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = network.protocolLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isConnected,
                    onCheckedChange = onToggleConnected,
                    modifier = Modifier.testTag("bridge_switch_${network.id}")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = accountHandle.ifBlank { "No account linked" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isConnected && lastSyncedMs > 0L) {
                        Text(
                            text = "Synced ${formatRelativeTimestamp(lastSyncedMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onConfigureClick,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("configure_bridge_${network.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SettingsEthernet,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.connect_handle_action),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigureBridgeDialog(
    network: SocialNetwork,
    initialHandle: String,
    isCurrentlyConnected: Boolean,
    onDismiss: () -> Unit,
    onSave: (handle: String, connected: Boolean) -> Unit
) {
    var handle by rememberSaveable { mutableStateOf(initialHandle) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("configure_bridge_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.bridge_dialog_title, network.displayName),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = network.protocolLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text(stringResource(R.string.bridge_handle_input)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bridge_handle_field")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    if (isCurrentlyConnected) {
                        TextButton(
                            onClick = { onSave(handle, false) },
                            modifier = Modifier.testTag("pause_bridge_button")
                        ) {
                            Text(stringResource(R.string.disconnect_bridge_action))
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel_action))
                        }
                    }

                    Button(
                        onClick = { onSave(handle, true) },
                        enabled = handle.isNotBlank(),
                        shape = CircleShape,
                        modifier = Modifier.testTag("save_bridge_button")
                    ) {
                        Text(stringResource(R.string.save_bridge_action))
                    }
                }
            }
        }
    }
}

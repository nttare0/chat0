package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SocialNetwork
import com.example.ui.BeeperSpace
import com.example.ui.ConduitTab
import com.example.ui.InboxUiState

@Composable
fun BeeperModalDrawerContent(
    uiState: InboxUiState,
    currentTab: ConduitTab,
    onSelectSpace: (BeeperSpace) -> Unit,
    onSelectNetwork: (SocialNetwork?) -> Unit,
    onSelectTab: (ConduitTab) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .width(304.dp)
            .testTag("beeper_spaces_drawer")
    ) {
        BeeperDrawerBody(
            uiState = uiState,
            currentTab = currentTab,
            onSelectSpace = onSelectSpace,
            onSelectNetwork = onSelectNetwork,
            onSelectTab = onSelectTab
        )
    }
}

@Composable
fun BeeperPermanentSidebar(
    uiState: InboxUiState,
    currentTab: ConduitTab,
    onSelectSpace: (BeeperSpace) -> Unit,
    onSelectNetwork: (SocialNetwork?) -> Unit,
    onSelectTab: (ConduitTab) -> Unit
) {
    PermanentDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .width(264.dp)
            .fillMaxHeight()
            .testTag("beeper_permanent_sidebar")
    ) {
        BeeperDrawerBody(
            uiState = uiState,
            currentTab = currentTab,
            onSelectSpace = onSelectSpace,
            onSelectNetwork = onSelectNetwork,
            onSelectTab = onSelectTab
        )
    }
}

@Composable
private fun BeeperDrawerBody(
    uiState: InboxUiState,
    currentTab: ConduitTab,
    onSelectSpace: (BeeperSpace) -> Unit,
    onSelectNetwork: (SocialNetwork?) -> Unit,
    onSelectTab: (ConduitTab) -> Unit
) {
    val itemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedBadgeColor = MaterialTheme.colorScheme.onPrimaryContainer,
        unselectedContainerColor = MaterialTheme.colorScheme.surface,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurface
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        // Brand & E2EE Vault Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.activeBridgeCount} networks · E2EE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.spaces_header).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )

        SpaceDrawerItem(
            label = stringResource(R.string.space_all_chats),
            icon = Icons.Outlined.Inbox,
            selected = currentTab == ConduitTab.INBOX &&
                uiState.selectedSpace == BeeperSpace.ALL &&
                uiState.selectedNetwork == null,
            badgeCount = uiState.totalUnreadCount,
            colors = itemColors,
            testTag = "drawer_space_all",
            onClick = {
                if (uiState.selectedNetwork != null) onSelectNetwork(null)
                onSelectSpace(BeeperSpace.ALL)
            }
        )

        SpaceDrawerItem(
            label = stringResource(R.string.space_unread),
            icon = Icons.Outlined.MarkChatUnread,
            selected = currentTab == ConduitTab.INBOX && uiState.selectedSpace == BeeperSpace.UNREAD,
            badgeCount = uiState.totalUnreadCount,
            colors = itemColors,
            testTag = "drawer_space_unread",
            onClick = { onSelectSpace(BeeperSpace.UNREAD) }
        )

        SpaceDrawerItem(
            label = stringResource(R.string.space_dms),
            icon = Icons.Outlined.ChatBubbleOutline,
            selected = currentTab == ConduitTab.INBOX && uiState.selectedSpace == BeeperSpace.DMS,
            badgeCount = 0,
            colors = itemColors,
            testTag = "drawer_space_dms",
            onClick = { onSelectSpace(BeeperSpace.DMS) }
        )

        SpaceDrawerItem(
            label = stringResource(R.string.space_groups),
            icon = Icons.Outlined.Forum,
            selected = currentTab == ConduitTab.INBOX && uiState.selectedSpace == BeeperSpace.GROUPS,
            badgeCount = 0,
            colors = itemColors,
            testTag = "drawer_space_groups",
            onClick = { onSelectSpace(BeeperSpace.GROUPS) }
        )

        SpaceDrawerItem(
            label = stringResource(R.string.space_archived),
            icon = Icons.Outlined.Archive,
            selected = currentTab == ConduitTab.INBOX && uiState.selectedSpace == BeeperSpace.ARCHIVED,
            badgeCount = uiState.archivedCount,
            colors = itemColors,
            testTag = "drawer_space_archived",
            onClick = { onSelectSpace(BeeperSpace.ARCHIVED) }
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.drawer_networks_section).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )

        val connectedIds = uiState.bridges.filter { it.isConnected }.map { it.networkId }.toSet()
        SocialNetwork.entries
            .filter { connectedIds.isEmpty() || it.id in connectedIds }
            .forEach { network ->
                val isSelected = currentTab == ConduitTab.INBOX && uiState.selectedNetwork == network
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = network.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = network.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    badge = {
                        Text(
                            text = network.shortCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    selected = isSelected,
                    onClick = { onSelectNetwork(network) },
                    shape = RoundedCornerShape(12.dp),
                    colors = itemColors,
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("drawer_network_${network.id}")
                )
            }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        NavigationDrawerItem(
            label = {
                Text(
                    text = stringResource(R.string.drawer_manage_bridges),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Hub,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            selected = currentTab == ConduitTab.BRIDGES,
            onClick = { onSelectTab(ConduitTab.BRIDGES) },
            shape = RoundedCornerShape(12.dp),
            colors = itemColors,
            modifier = Modifier
                .height(48.dp)
                .testTag("nav_tab_bridges")
        )

        NavigationDrawerItem(
            label = {
                Text(
                    text = stringResource(R.string.drawer_security_prefs),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            selected = currentTab == ConduitTab.SECURITY,
            onClick = { onSelectTab(ConduitTab.SECURITY) },
            shape = RoundedCornerShape(12.dp),
            colors = itemColors,
            modifier = Modifier
                .height(48.dp)
                .testTag("nav_tab_security")
        )
    }
}

@Composable
private fun SpaceDrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int,
    colors: androidx.compose.material3.NavigationDrawerItemColors,
    testTag: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        badge = if (badgeCount > 0) {
            {
                Surface(
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        } else null,
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = colors,
        modifier = Modifier
            .height(46.dp)
            .testTag(testTag)
    )
}

package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DecryptedConversation
import com.example.data.InboxDisplayStyle
import com.example.data.SocialNetwork
import com.example.ui.BeeperSpace
import com.example.ui.InboxUiState
import com.example.ui.components.ContactAvatarWithBridge
import com.example.ui.components.NetworkPillBadge
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.components.icon

@Composable
fun UnifiedInboxPane(
    uiState: InboxUiState,
    selectedConversationId: Long?,
    showDrawerMenuButton: Boolean,
    onOpenDrawer: () -> Unit,
    onConversationClick: (Long) -> Unit,
    onToggleArchive: (DecryptedConversation) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleUnreadOnly: () -> Unit,
    onNetworkFilterSelect: (SocialNetwork?) -> Unit,
    onSpaceSelect: (BeeperSpace) -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProStyle = uiState.settings.displayStyle == InboxDisplayStyle.PRO
    val isSearching = uiState.searchQuery.trim().isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
        ) {
            // Top Beeper Header + Top Search Bar + Unread Filter Toggle + Spacebar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (showDrawerMenuButton) {
                            IconButton(
                                onClick = onOpenDrawer,
                                modifier = Modifier.testTag("open_spaces_drawer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Menu,
                                    contentDescription = stringResource(R.string.open_spaces_drawer),
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        val headerTitle = when {
                            uiState.selectedNetwork != null -> uiState.selectedNetwork.displayName
                            uiState.selectedSpace == BeeperSpace.ARCHIVED -> stringResource(R.string.space_archived)
                            else -> stringResource(R.string.inbox_title)
                        }
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        // Dedicated Unread Filter Toggle Button
                        UnreadFilterToggleButton(
                            isUnreadOnly = uiState.unreadOnly,
                            unreadCount = uiState.totalUnreadCount,
                            onToggle = onToggleUnreadOnly
                        )

                        // Subtle E2EE Shield Pill
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "E2EE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Top Unified Search Bar across all connected accounts & encrypted message history
                TopInboxSearchBar(
                    searchQuery = uiState.searchQuery,
                    matchingCount = uiState.conversations.size,
                    onSearchQueryChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Beeper Horizontal Spacebar Tabs (Spaces + Active Network Pills)
                BeeperSpacebarRow(
                    selectedSpace = uiState.selectedSpace,
                    unreadOnly = uiState.unreadOnly,
                    selectedNetwork = uiState.selectedNetwork,
                    connectedNetworkIds = uiState.bridges.filter { it.isConnected }.map { it.networkId }.toSet(),
                    onSpaceSelect = onSpaceSelect,
                    onNetworkSelect = onNetworkFilterSelect
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            // Main Inbox Feed + Pinned Favorites Row
            val hasPinned = uiState.pinnedConversations.isNotEmpty()
            val hasFeed = uiState.feedConversations.isNotEmpty()

            if (!hasPinned && !hasFeed) {
                EmptyInboxZeroState(
                    isSearching = isSearching,
                    searchQuery = uiState.searchQuery,
                    isUnreadOnly = uiState.unreadOnly,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("conversations_list"),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    // Beeper Signature: Pinned Chats clustered neatly as circular icons at the top
                    if (hasPinned) {
                        item(key = "beeper_pinned_favorites_dock") {
                            BeeperPinnedFavoritesRow(
                                pinnedConversations = uiState.pinnedConversations,
                                selectedConversationId = selectedConversationId,
                                onConversationClick = onConversationClick
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    items(
                        items = uiState.feedConversations,
                        key = { it.id }
                    ) { conversation ->
                        val matchedMessageSnippet = uiState.matchedSnippetsByConversationId[conversation.id]
                        SwipeableBeeperChatRow(
                            conversation = conversation,
                            matchedSearchSnippet = matchedMessageSnippet,
                            isSelected = selectedConversationId == conversation.id,
                            isProStyle = isProStyle,
                            maskPreview = uiState.settings.maskInboxPreviews,
                            onClick = { onConversationClick(conversation.id) },
                            onSwipeToggleArchive = { onToggleArchive(conversation) }
                        )
                    }
                }
            }
        }

        // Clean Floating New Chat Action Button
        FloatingActionButton(
            onClick = onNewChatClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 3.dp,
                pressedElevation = 6.dp
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .imePadding()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(20.dp)
                .size(56.dp)
                .testTag("new_chat_fab")
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.new_chat_fab),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun UnreadFilterToggleButton(
    isUnreadOnly: Boolean,
    unreadCount: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isUnreadOnly) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 160),
        label = "unreadToggleBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isUnreadOnly) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 160),
        label = "unreadToggleFg"
    )

    Surface(
        onClick = onToggle,
        shape = CircleShape,
        color = containerColor,
        border = if (isUnreadOnly) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        modifier = modifier
            .height(40.dp)
            .semantics {
                role = Role.Checkbox
                selected = isUnreadOnly
            }
            .testTag("unread_filter_toggle")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isUnreadOnly) {
                    Icons.Filled.MarkChatUnread
                } else {
                    Icons.Outlined.MarkChatUnread
                },
                contentDescription = stringResource(R.string.unread_only_toggle_desc),
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.unread_only_toggle),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isUnreadOnly) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = contentColor
            )
            if (unreadCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (isUnreadOnly) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnreadOnly) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopInboxSearchBar(
    searchQuery: String,
    matchingCount: Int,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (searchQuery.isNotEmpty()) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        modifier = modifier
            .height(48.dp)
            .testTag("top_inbox_search_container")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = if (searchQuery.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(19.dp)
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = stringResource(R.string.top_search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inbox_search_input")
                )
            }

            if (searchQuery.isNotEmpty()) {
                Text(
                    text = matchingCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("clear_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BeeperSpacebarRow(
    selectedSpace: BeeperSpace,
    unreadOnly: Boolean,
    selectedNetwork: SocialNetwork?,
    connectedNetworkIds: Set<String>,
    onSpaceSelect: (BeeperSpace) -> Unit,
    onNetworkSelect: (SocialNetwork?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val spaces = listOf(
            BeeperSpace.ALL to stringResource(R.string.space_all_chats),
            BeeperSpace.DMS to stringResource(R.string.space_dms),
            BeeperSpace.GROUPS to stringResource(R.string.space_groups),
            BeeperSpace.ARCHIVED to stringResource(R.string.space_archived)
        )

        spaces.forEach { (space, title) ->
            val isSelected = selectedSpace == space &&
                selectedNetwork == null &&
                !(space == BeeperSpace.ALL && unreadOnly)
            SpaceTabPill(
                label = title,
                selected = isSelected,
                onClick = {
                    if (selectedNetwork != null) onNetworkSelect(null)
                    onSpaceSelect(space)
                },
                modifier = Modifier.testTag("space_pill_${space.id}")
            )
        }

        // Subtle divider dot before per-network spaces
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline)
        )

        SocialNetwork.entries
            .filter { connectedNetworkIds.isEmpty() || it.id in connectedNetworkIds }
            .forEach { network ->
                val isSelected = selectedNetwork == network
                SpaceTabPill(
                    label = network.displayName,
                    leadingIcon = network.icon(),
                    selected = isSelected,
                    onClick = { onNetworkSelect(network) },
                    modifier = Modifier.testTag("filter_network_${network.id}")
                )
            }
    }
}

@Composable
private fun SpaceTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = contentColor
            )
        }
    }
}

@Composable
private fun BeeperPinnedFavoritesRow(
    pinnedConversations: List<DecryptedConversation>,
    selectedConversationId: Long?,
    onConversationClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 14.dp)
            .testTag("pinned_favorites_row")
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(
                items = pinnedConversations,
                key = { "pinned_${it.id}" }
            ) { conv ->
                val isSelected = selectedConversationId == conv.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onConversationClick(conv.id) }
                        .padding(vertical = 4.dp)
                        .testTag("pinned_avatar_${conv.id}")
                ) {
                    Box {
                        Surface(
                            shape = CircleShape,
                            border = if (conv.unreadCount > 0 || isSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            },
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                ContactAvatarWithBridge(
                                    initials = conv.avatarInitials,
                                    network = conv.network,
                                    size = 52.dp
                                )
                            }
                        }

                        if (conv.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = conv.unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Text(
                        text = conv.contactName.substringBefore(" "),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (conv.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableBeeperChatRow(
    conversation: DecryptedConversation,
    matchedSearchSnippet: String?,
    isSelected: Boolean,
    isProStyle: Boolean,
    maskPreview: Boolean,
    onClick: () -> Unit,
    onSwipeToggleArchive: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { targetValue ->
            if (targetValue == SwipeToDismissBoxValue.EndToStart ||
                targetValue == SwipeToDismissBoxValue.StartToEnd
            ) {
                onSwipeToggleArchive()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val isArchiving = !conversation.isArchived
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isArchiving) Icons.Outlined.Archive else Icons.Outlined.Unarchive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(
                            if (isArchiving) R.string.swipe_archive_hint else R.string.swipe_unarchive_hint
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    ) {
        BeeperConversationRow(
            conversation = conversation,
            matchedSearchSnippet = matchedSearchSnippet,
            isSelected = isSelected,
            isProStyle = isProStyle,
            maskPreview = maskPreview,
            onClick = onClick
        )
    }
}

@Composable
private fun BeeperConversationRow(
    conversation: DecryptedConversation,
    matchedSearchSnippet: String?,
    isSelected: Boolean,
    isProStyle: Boolean,
    maskPreview: Boolean,
    onClick: () -> Unit
) {
    val rowBg = if (isSelected) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.background
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp)
            .testTag("conversation_row_${conversation.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ContactAvatarWithBridge(
            initials = conversation.avatarInitials,
            network = conversation.network,
            size = 48.dp
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = conversation.contactName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (conversation.unreadCount > 0) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Medium
                            }
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (conversation.isMuted) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = "Muted",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (isProStyle) {
                        if (conversation.isKeyVerified) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = stringResource(R.string.e2ee_verified),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        NetworkPillBadge(network = conversation.network)
                    }
                }

                Text(
                    text = formatRelativeTimestamp(conversation.lastMessageTimestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (conversation.unreadCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val previewText = when {
                    maskPreview -> "🔒 ${conversation.lastCipherPreview}"
                    matchedSearchSnippet != null -> matchedSearchSnippet
                    else -> conversation.lastPlainPreview
                }

                Text(
                    text = previewText,
                    style = if (maskPreview) {
                        MaterialTheme.typography.labelSmall
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = when {
                        matchedSearchSnippet != null && !maskPreview -> MaterialTheme.colorScheme.primary
                        conversation.unreadCount > 0 -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    if (isProStyle) {
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (conversation.isMuted) {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (conversation.isMuted) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                }
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInboxZeroState(
    isSearching: Boolean,
    searchQuery: String,
    isUnreadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .widthIn(max = 260.dp)
                .height(150.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_vault_illustration_1793811354374),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        val titleText = when {
            isSearching -> stringResource(R.string.empty_search_title)
            isUnreadOnly -> stringResource(R.string.empty_unread_title)
            else -> stringResource(R.string.empty_inbox_title)
        }
        val subtitleText = when {
            isSearching -> stringResource(R.string.empty_search_subtitle, searchQuery.trim())
            isUnreadOnly -> stringResource(R.string.empty_unread_subtitle)
            else -> stringResource(R.string.empty_inbox_subtitle)
        }

        Text(
            text = titleText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitleText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 310.dp)
        )
    }
}

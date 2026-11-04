package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DecryptedConversation
import com.example.data.DecryptedMessage
import com.example.ui.components.ContactAvatarWithBridge
import com.example.ui.components.InstagramBlueAction
import com.example.ui.components.InstagramGradientBerry
import com.example.ui.components.InstagramHeartRed
import com.example.ui.components.InstagramIncomingGlassDark
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.SafetyNumberVerificationDialog
import com.example.ui.components.formatMessageTime
import com.example.ui.components.icon

@Composable
fun ChatThreadPane(
    conversation: DecryptedConversation,
    messages: List<DecryptedMessage>,
    safetyNumber: String,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSimulateReply: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onToggleMute: () -> Unit,
    onMarkVerified: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showBackButton) {
        BackHandler(onBack = onBack)
    }

    var draftText by rememberSaveable(conversation.id) { mutableStateOf("") }
    var showSafetyDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var activeReactionPickerMsgId by remember { mutableStateOf<Long?>(null) }
    val messageReactions = remember { mutableStateMapOf<Long, String>() }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Liquid Glass Ambient Backdrop with dynamic refractive fluid orbs
    LiquidGlassBackdrop(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("chat_thread_pane")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Instagram-style Liquid Glass Top Bar
            Surface(
                color = Color(0xFF101015).copy(alpha = 0.88f),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_action),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Contact Avatar with active online indicator dot
                    Box(modifier = Modifier.padding(end = 10.dp)) {
                        ContactAvatarWithBridge(
                            initials = conversation.avatarInitials,
                            network = conversation.network,
                            size = 40.dp
                        )
                        // Instagram green active status dot
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                                .border(BorderStroke(1.5.dp, Color(0xFF101015)), CircleShape)
                        )
                    }

                    // Name and subtitle
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showOverflowMenu = true },
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = conversation.contactName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (conversation.isKeyVerified) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = stringResource(R.string.e2ee_verified),
                                    tint = InstagramBlueAction,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Text(
                            text = "${stringResource(R.string.active_now)} · ${conversation.network.displayName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFFA1A1AA),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Instagram DM action icons on right: Audio, Video, Info
                    IconButton(
                        onClick = onSimulateReply,
                        modifier = Modifier.testTag("chat_audio_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Call,
                            contentDescription = stringResource(R.string.instagram_call),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onSimulateReply,
                        modifier = Modifier.testTag("chat_video_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = stringResource(R.string.instagram_video_call),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("chat_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(R.string.instagram_info),
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.safety_number_title)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showSafetyDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (conversation.isPinned) {
                                            stringResource(R.string.unpin_conversation)
                                        } else {
                                            stringResource(R.string.pin_conversation)
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.PushPin, contentDescription = null)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onTogglePin()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (conversation.isMuted) {
                                            stringResource(R.string.unmute_conversation)
                                        } else {
                                            stringResource(R.string.mute_conversation)
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (conversation.isMuted) {
                                            Icons.Outlined.Notifications
                                        } else {
                                            Icons.Outlined.NotificationsOff
                                        },
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onToggleMute()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (conversation.isArchived) {
                                            stringResource(R.string.unarchive_conversation)
                                        } else {
                                            stringResource(R.string.archive_conversation)
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (conversation.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onToggleArchive()
                                }
                            )
                        }
                    }
                }
            }

            // Message Stream Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("messages_list"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { msg ->
                        val reaction = messageReactions[msg.id]
                        InstagramLiquidGlassMessageBubble(
                            message = msg,
                            reaction = reaction,
                            isLastOutgoing = msg.isOutgoing && msg == messages.lastOrNull { it.isOutgoing },
                            onDoubleTapHeart = {
                                if (messageReactions[msg.id] == "❤️") {
                                    messageReactions.remove(msg.id)
                                } else {
                                    messageReactions[msg.id] = "❤️"
                                }
                            },
                            onLongPress = {
                                activeReactionPickerMsgId = if (activeReactionPickerMsgId == msg.id) null else msg.id
                            }
                        )
                    }
                }

                // Floating Instagram Quick Emoji Reaction Picker
                activeReactionPickerMsgId?.let { targetMsgId ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        InstagramReactionPill(
                            onSelectEmoji = { emoji ->
                                messageReactions[targetMsgId] = emoji
                                activeReactionPickerMsgId = null
                            },
                            onDismiss = { activeReactionPickerMsgId = null }
                        )
                    }
                }
            }

            // Instagram DM Liquid Glass Bottom Composer
            InstagramLiquidGlassComposer(
                draftText = draftText,
                networkName = conversation.network.displayName,
                onDraftTextChange = { draftText = it },
                onSend = {
                    if (draftText.isNotBlank()) {
                        onSendMessage(draftText)
                        draftText = ""
                    }
                },
                onSendHeart = {
                    onSendMessage("❤️")
                },
                onCameraClick = onSimulateReply
            )
        }
    }

    if (showSafetyDialog) {
        SafetyNumberVerificationDialog(
            conversation = conversation,
            safetyNumber = safetyNumber,
            onVerify = onMarkVerified,
            onDismiss = { showSafetyDialog = false }
        )
    }
}

/**
 * Instagram-styled message bubble featuring liquid-glass-js refractive highlights,
 * signature sunset berry gradients for outgoing messages, frosted translucent incoming bubbles,
 * and double-tap to heart reactions with zero annoying popups.
 */
@Composable
private fun InstagramLiquidGlassMessageBubble(
    message: DecryptedMessage,
    reaction: String?,
    isLastOutgoing: Boolean,
    onDoubleTapHeart: () -> Unit,
    onLongPress: () -> Unit
) {
    val alignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleShape = if (message.isOutgoing) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    // Refractive specular rim lighting for Liquid Glass look
    val specularBorder = if (message.isOutgoing) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.40f), // Crisp top highlight
                Color.White.copy(alpha = 0.12f),
                Color.Transparent
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f), // Liquid glass rim
                Color(0xFF00F2FE).copy(alpha = 0.14f), // Subtle chromatic aberration
                Color.White.copy(alpha = 0.08f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Column(horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start) {
            Box {
                Surface(
                    shape = bubbleShape,
                    color = if (message.isOutgoing) Color.Transparent else InstagramIncomingGlassDark,
                    border = BorderStroke(1.dp, specularBorder),
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .clip(bubbleShape)
                        .then(
                            if (message.isOutgoing) {
                                Modifier.background(
                                    Brush.linearGradient(
                                        colors = InstagramGradientBerry,
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(400f, 400f)
                                    )
                                )
                            } else {
                                Modifier
                            }
                        )
                        .pointerInput(message.id) {
                            detectTapGestures(
                                onDoubleTap = { onDoubleTapHeart() },
                                onLongPress = { onLongPress() },
                                onTap = { /* No annoying popup! */ }
                            )
                        }
                        .testTag("message_bubble_${message.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = message.plainText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = if (message.plainText == "❤️") 36.sp else 15.sp,
                                lineHeight = if (message.plainText == "❤️") 42.sp else 21.sp,
                                color = Color.White
                            )
                        )
                    }
                }

                // Floating Reaction Badge (Instagram-style heart on the bottom edge)
                androidx.compose.animation.AnimatedVisibility(
                    visible = reaction != null,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier
                        .align(if (message.isOutgoing) Alignment.BottomStart else Alignment.BottomEnd)
                        .offset(x = if (message.isOutgoing) (-4).dp else 4.dp, y = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E1E26),
                        border = BorderStroke(1.5.dp, Color(0xFF101015)),
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = reaction ?: "❤️",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Instagram Seen / Delivery status indicator beneath last outgoing message
            if (isLastOutgoing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(top = 4.dp, end = 4.dp)
                ) {
                    Text(
                        text = "Seen ${formatMessageTime(message.timestamp)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Color(0xFFA1A1AA)
                        )
                    )
                    Icon(
                        imageVector = Icons.Outlined.DoneAll,
                        contentDescription = null,
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Instagram-style Quick Emoji Reaction Pill with liquid glass effect
 */
@Composable
private fun InstagramReactionPill(
    onSelectEmoji: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf("❤️", "🔥", "😂", "😮", "😢", "👍")

    LiquidGlassPill(
        shape = CircleShape,
        backgroundColor = Color(0xFF1E1E26).copy(alpha = 0.95f),
        specularAlpha = 0.40f,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            emojis.forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .clickable { onSelectEmoji(emoji) }
                        .padding(4.dp)
                )
            }
        }
    }
}

/**
 * Instagram DM Liquid Glass Bottom Composer with photo camera button,
 * message textfield, mic, gallery, instant heart tap, and bold blue "Send" button.
 */
@Composable
private fun InstagramLiquidGlassComposer(
    draftText: String,
    networkName: String,
    onDraftTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendHeart: () -> Unit,
    onCameraClick: () -> Unit
) {
    Surface(
        color = Color(0xFF101015).copy(alpha = 0.88f),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.Transparent
                )
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Instagram blue circular Camera shortcut button
            Surface(
                onClick = onCameraClick,
                shape = CircleShape,
                color = InstagramBlueAction,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("chat_camera_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = stringResource(R.string.instagram_camera),
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Liquid Glass Pill Input Field
            LiquidGlassPill(
                shape = CircleShape,
                backgroundColor = Color(0xFF1C1C24).copy(alpha = 0.92f),
                specularAlpha = 0.32f,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (draftText.isEmpty()) {
                            Text(
                                text = stringResource(R.string.message_input_placeholder),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    color = Color(0xFF94A3B8)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = draftText,
                            onValueChange = onDraftTextChange,
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                color = Color.White
                            ),
                            cursorBrush = SolidColor(InstagramBlueAction),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("message_composer_input")
                        )
                    }

                    // Instagram-style action icons inside input pill
                    if (draftText.isEmpty()) {
                        IconButton(
                            onClick = onCameraClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = stringResource(R.string.instagram_mic),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onCameraClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = stringResource(R.string.instagram_gallery),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Instagram vibrant red heart button
                        IconButton(
                            onClick = onSendHeart,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("chat_heart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = stringResource(R.string.instagram_heart),
                                tint = InstagramHeartRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        // Bold Instagram Blue "Send" text button
                        TextButton(
                            onClick = onSend,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("send_message_button")
                        ) {
                            Text(
                                text = stringResource(R.string.instagram_send),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = InstagramBlueAction,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyThreadPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            LiquidGlassPill(
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = InstagramHeartRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.select_chat_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA1A1AA)
            )
        }
    }
}

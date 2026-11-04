package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SocialNetwork(
    val id: String,
    val displayName: String,
    val shortCode: String,
    val protocolLabel: String
) {
    SIGNAL("signal", "Signal", "SIG", "Double Ratchet · Curve25519"),
    WHATSAPP("whatsapp", "WhatsApp", "WA", "Signal Protocol Bridge"),
    INSTAGRAM("instagram", "Instagram", "IG", "Meta E2EE Messenger Bridge"),
    TELEGRAM("telegram", "Telegram", "TG", "MTProto 2.0 Secret Bridge"),
    DISCORD("discord", "Discord", "DC", "DAVE E2EE DM Bridge"),
    SLACK("slack", "Slack", "SLK", "Matrix Conduit MLS Bridge"),
    X_DM("x_dm", "X Chat", "X", "Encrypted Direct Message Bridge");

    companion object {
        fun fromId(id: String): SocialNetwork =
            entries.firstOrNull { it.id == id } ?: SIGNAL
    }
}

@Entity(tableName = "bridges")
data class BridgeAccountEntity(
    @PrimaryKey val networkId: String,
    val accountHandle: String,
    val isConnected: Boolean,
    val lastSyncedEpochMs: Long
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val contactName: String,
    val contactHandle: String,
    val avatarInitials: String,
    val networkId: String,
    val lastCipherText: String,
    val lastIv: String,
    val lastDigest: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isKeyVerified: Boolean = false,
    val isGroup: Boolean = false,
    val isMuted: Boolean = false
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val conversationId: Long,
    val senderName: String,
    val isOutgoing: Boolean,
    val cipherTextBase64: String,
    val ivBase64: String,
    val sha256Digest: String,
    val timestamp: Long,
    val deliveryStatus: String = "ENCRYPTED"
)

data class DecryptedConversation(
    val id: Long,
    val contactName: String,
    val contactHandle: String,
    val avatarInitials: String,
    val network: SocialNetwork,
    val lastPlainPreview: String,
    val lastCipherPreview: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isKeyVerified: Boolean,
    val isGroup: Boolean = false,
    val isMuted: Boolean = false
)

data class DecryptedMessage(
    val id: Long,
    val conversationId: Long,
    val senderName: String,
    val isOutgoing: Boolean,
    val plainText: String,
    val cipherTextBase64: String,
    val ivBase64: String,
    val sha256Digest: String,
    val timestamp: Long,
    val deliveryStatus: String
)

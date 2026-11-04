package com.example.data

import com.example.crypto.CryptoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ConduitRepository(
    private val dao: ConduitDao,
    private val cryptoEngine: CryptoEngine
) {

    val bridgesFlow: Flow<List<BridgeAccountEntity>> = dao.observeBridges()

    val totalEncryptedMessagesFlow: Flow<Int> = dao.observeTotalEncryptedMessageCount()

    val activeConversationsFlow: Flow<List<DecryptedConversation>> =
        dao.observeActiveConversations().map { entities ->
            entities.map { entity -> entity.toDecryptedConversation() }
        }

    val allConversationsFlow: Flow<List<DecryptedConversation>> =
        dao.observeAllConversations().map { entities ->
            entities.map { entity -> entity.toDecryptedConversation() }
        }

    fun observeConversation(conversationId: Long): Flow<DecryptedConversation?> =
        dao.observeConversationById(conversationId).map { entity ->
            entity?.toDecryptedConversation()
        }

    fun observeMessages(conversationId: Long): Flow<List<DecryptedMessage>> =
        dao.observeMessagesForConversation(conversationId).map { entities ->
            entities.map { entity -> entity.toDecryptedMessage() }
        }

    val allDecryptedMessagesFlow: Flow<List<DecryptedMessage>> =
        dao.observeAllMessages().map { entities ->
            entities.map { entity -> entity.toDecryptedMessage() }
        }

    private fun MessageEntity.toDecryptedMessage(): DecryptedMessage {
        val plain = cryptoEngine.decrypt(
            cipherTextBase64 = cipherTextBase64,
            ivBase64 = ivBase64,
            associatedData = "conv:$conversationId"
        ).getOrElse { "[Decryption failed: key mismatch]" }

        return DecryptedMessage(
            id = id,
            conversationId = conversationId,
            senderName = senderName,
            isOutgoing = isOutgoing,
            plainText = plain,
            cipherTextBase64 = cipherTextBase64,
            ivBase64 = ivBase64,
            sha256Digest = sha256Digest,
            timestamp = timestamp,
            deliveryStatus = deliveryStatus
        )
    }

    private fun ConversationEntity.toDecryptedConversation(): DecryptedConversation {
        val network = SocialNetwork.fromId(networkId)
        val decryptedPreview = if (lastCipherText.isNotEmpty() && lastIv.isNotEmpty()) {
            cryptoEngine.decrypt(
                cipherTextBase64 = lastCipherText,
                ivBase64 = lastIv,
                associatedData = "preview:$networkId:$contactHandle"
            ).getOrElse { "Encrypted message" }
        } else {
            "Encrypted session initialized"
        }

        val shortCipher = if (lastCipherText.length > 28) {
            lastCipherText.take(28) + "…"
        } else {
            lastCipherText.ifEmpty { "AES-256-GCM" }
        }

        return DecryptedConversation(
            id = id,
            contactName = contactName,
            contactHandle = contactHandle,
            avatarInitials = avatarInitials,
            network = network,
            lastPlainPreview = decryptedPreview,
            lastCipherPreview = shortCipher,
            lastMessageTimestamp = lastMessageTimestamp,
            unreadCount = unreadCount,
            isPinned = isPinned,
            isArchived = isArchived,
            isKeyVerified = isKeyVerified,
            isGroup = isGroup,
            isMuted = isMuted
        )
    }

    suspend fun initializeBootstrapVaultIfEmpty() = withContext(Dispatchers.IO) {
        val existingBridges = dao.getAllBridgesOnce()
        if (existingBridges.isEmpty()) {
            val now = System.currentTimeMillis()
            val defaultBridges = SocialNetwork.entries.mapIndexed { index, network ->
                val connectedByDefault = index < 6 // Signal, WhatsApp, Instagram, Telegram, Discord, Slack connected
                val defaultHandle = when (network) {
                    SocialNetwork.SIGNAL -> "+1 (415) 890-4412"
                    SocialNetwork.WHATSAPP -> "+1 (415) 890-4412"
                    SocialNetwork.INSTAGRAM -> "@alex.mercer"
                    SocialNetwork.TELEGRAM -> "@alexmercer_dev"
                    SocialNetwork.DISCORD -> "alexmercer#01"
                    SocialNetwork.SLACK -> "alex@conduit.hq"
                    SocialNetwork.X_DM -> ""
                }
                BridgeAccountEntity(
                    networkId = network.id,
                    accountHandle = defaultHandle,
                    isConnected = connectedByDefault,
                    lastSyncedEpochMs = if (connectedByDefault) now - (index * 120_000L) else 0L
                )
            }
            dao.upsertBridges(defaultBridges)
        }

        if (dao.countConversations() == 0) {
            seedEncryptedConversations()
        }
    }

    private suspend fun seedEncryptedConversations() {
        val now = System.currentTimeMillis()

        data class SeedThread(
            val name: String,
            val handle: String,
            val initials: String,
            val network: SocialNetwork,
            val unread: Int,
            val pinned: Boolean,
            val verified: Boolean,
            val isGroup: Boolean = false,
            val isMuted: Boolean = false,
            val messages: List<Pair<Boolean, String>>,
            val minutesAgo: Long
        )

        val seeds = listOf(
            SeedThread(
                name = "Elena Rostova",
                handle = "@elena.sec",
                initials = "ER",
                network = SocialNetwork.SIGNAL,
                unread = 1,
                pinned = true,
                verified = true,
                minutesAgo = 3,
                messages = listOf(
                    false to "Hey! Did the Matrix bridge handshake complete on your new device?",
                    true to "Yes — keys rotated cleanly and zero plaintext leaves local SQLite.",
                    false to "Awesome. Sending over the Q4 security audit notes here now."
                )
            ),
            SeedThread(
                name = "Marcus Vance",
                handle = "+44 7700 900418",
                initials = "MV",
                network = SocialNetwork.WHATSAPP,
                unread = 2,
                pinned = true,
                verified = true,
                minutesAgo = 14,
                messages = listOf(
                    false to "Landing at Heathrow in 20 mins.",
                    true to "Safe landing! Meet at the espresso bar near Terminal 5?",
                    false to "Perfect, grabbing my carry-on now.",
                    false to "Just cleared customs — see you in 5."
                )
            ),
            SeedThread(
                name = "Sora Takahashi",
                handle = "@sora.studio",
                initials = "ST",
                network = SocialNetwork.INSTAGRAM,
                unread = 0,
                pinned = true,
                verified = true,
                minutesAgo = 41,
                messages = listOf(
                    false to "Shared the minimal Beeper-style spacebar explorations in Figma.",
                    true to "The pinned circle favorites dock looks super clean. Ship it!"
                )
            ),
            SeedThread(
                name = "Design Systems #core",
                handle = "#design-systems",
                initials = "DS",
                network = SocialNetwork.SLACK,
                unread = 3,
                pinned = false,
                verified = true,
                isGroup = true,
                minutesAgo = 26,
                messages = listOf(
                    false to "Updated the spacing tokens to strict 8dp grid alignment.",
                    true to "Checked dark mode contrast — hits AAA across all chat bubbles.",
                    false to "Merging PR #418 into main before afternoon release."
                )
            ),
            SeedThread(
                name = "Protocol Ops Group",
                handle = "@protocol_core",
                initials = "PO",
                network = SocialNetwork.TELEGRAM,
                unread = 1,
                pinned = false,
                verified = false,
                isGroup = true,
                isMuted = true,
                minutesAgo = 95,
                messages = listOf(
                    false to "Relay node eu-central-2 latency dropped to 14ms after the patch.",
                    true to "Nice work. Let's verify the session fingerprint before pushing v2.4.",
                    false to "Ready whenever you want to compare safety numbers."
                )
            ),
            SeedThread(
                name = "Liam O'Connor",
                handle = "liam_oc",
                initials = "LO",
                network = SocialNetwork.DISCORD,
                unread = 0,
                pinned = false,
                verified = true,
                minutesAgo = 240,
                messages = listOf(
                    false to "Are we still running the Rust crypto benchmark tonight?",
                    true to "Count me in — 8pm UTC works great."
                )
            )
        )

        for (seed in seeds) {
            val lastText = seed.messages.last().second
            val lastTs = now - (seed.minutesAgo * 60_000L)
            val previewEnvelope = cryptoEngine.encrypt(
                plainText = lastText,
                associatedData = "preview:${seed.network.id}:${seed.handle}"
            )

            val convId = dao.insertConversation(
                ConversationEntity(
                    contactName = seed.name,
                    contactHandle = seed.handle,
                    avatarInitials = seed.initials,
                    networkId = seed.network.id,
                    lastCipherText = previewEnvelope.cipherTextBase64,
                    lastIv = previewEnvelope.ivBase64,
                    lastDigest = previewEnvelope.sha256Digest,
                    lastMessageTimestamp = lastTs,
                    unreadCount = seed.unread,
                    isPinned = seed.pinned,
                    isArchived = false,
                    isKeyVerified = seed.verified,
                    isGroup = seed.isGroup,
                    isMuted = seed.isMuted
                )
            )

            seed.messages.forEachIndexed { idx, (isOut, body) ->
                val msgTs = lastTs - ((seed.messages.size - 1 - idx) * 180_000L)
                val msgEnvelope = cryptoEngine.encrypt(
                    plainText = body,
                    associatedData = "conv:$convId"
                )
                dao.insertMessage(
                    MessageEntity(
                        conversationId = convId,
                        senderName = if (isOut) "You" else seed.name,
                        isOutgoing = isOut,
                        cipherTextBase64 = msgEnvelope.cipherTextBase64,
                        ivBase64 = msgEnvelope.ivBase64,
                        sha256Digest = msgEnvelope.sha256Digest,
                        timestamp = msgTs,
                        deliveryStatus = "ENCRYPTED"
                    )
                )
            }
        }
    }

    suspend fun sendMessage(
        conversationId: Long,
        plainText: String,
        isOutgoing: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val trimmed = plainText.trim()
        if (trimmed.isEmpty()) return@withContext
        val conv = dao.getConversationByIdOnce(conversationId) ?: return@withContext
        val now = System.currentTimeMillis()

        val messageEnvelope = cryptoEngine.encrypt(
            plainText = trimmed,
            associatedData = "conv:$conversationId"
        )
        dao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                senderName = if (isOutgoing) "You" else conv.contactName,
                isOutgoing = isOutgoing,
                cipherTextBase64 = messageEnvelope.cipherTextBase64,
                ivBase64 = messageEnvelope.ivBase64,
                sha256Digest = messageEnvelope.sha256Digest,
                timestamp = now,
                deliveryStatus = "ENCRYPTED"
            )
        )

        val previewEnvelope = cryptoEngine.encrypt(
            plainText = trimmed,
            associatedData = "preview:${conv.networkId}:${conv.contactHandle}"
        )
        dao.updateConversation(
            conv.copy(
                lastCipherText = previewEnvelope.cipherTextBase64,
                lastIv = previewEnvelope.ivBase64,
                lastDigest = previewEnvelope.sha256Digest,
                lastMessageTimestamp = now,
                isArchived = false,
                unreadCount = if (isOutgoing) 0 else conv.unreadCount + 1
            )
        )
    }

    suspend fun createNewConversation(
        contactName: String,
        contactHandle: String,
        network: SocialNetwork,
        initialMessage: String
    ): Long = withContext(Dispatchers.IO) {
        val cleanName = contactName.trim()
        val cleanHandle = contactHandle.trim().ifEmpty {
            "@${cleanName.lowercase().replace(" ", ".")}"
        }
        val initials = cleanName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "EC" }

        val now = System.currentTimeMillis()
        val firstBody = initialMessage.trim().ifEmpty {
            "End-to-end encrypted bridge session established via ${network.displayName}."
        }

        val previewEnvelope = cryptoEngine.encrypt(
            plainText = firstBody,
            associatedData = "preview:${network.id}:$cleanHandle"
        )

        val convId = dao.insertConversation(
            ConversationEntity(
                contactName = cleanName,
                contactHandle = cleanHandle,
                avatarInitials = initials,
                networkId = network.id,
                lastCipherText = previewEnvelope.cipherTextBase64,
                lastIv = previewEnvelope.ivBase64,
                lastDigest = previewEnvelope.sha256Digest,
                lastMessageTimestamp = now,
                unreadCount = 0,
                isPinned = false,
                isArchived = false,
                isKeyVerified = true,
                isGroup = cleanHandle.startsWith("#") || cleanName.contains("Group", ignoreCase = true),
                isMuted = false
            )
        )

        if (initialMessage.isNotBlank()) {
            val msgEnvelope = cryptoEngine.encrypt(
                plainText = initialMessage.trim(),
                associatedData = "conv:$convId"
            )
            dao.insertMessage(
                MessageEntity(
                    conversationId = convId,
                    senderName = "You",
                    isOutgoing = true,
                    cipherTextBase64 = msgEnvelope.cipherTextBase64,
                    ivBase64 = msgEnvelope.ivBase64,
                    sha256Digest = msgEnvelope.sha256Digest,
                    timestamp = now,
                    deliveryStatus = "ENCRYPTED"
                )
            )
        }
        convId
    }

    suspend fun markConversationRead(conversationId: Long) = withContext(Dispatchers.IO) {
        dao.markConversationRead(conversationId)
    }

    suspend fun togglePinConversation(conversationId: Long, currentlyPinned: Boolean) =
        withContext(Dispatchers.IO) {
            dao.setConversationPinned(conversationId, !currentlyPinned)
        }

    suspend fun setConversationArchived(conversationId: Long, archived: Boolean) =
        withContext(Dispatchers.IO) {
            dao.setConversationArchived(conversationId, archived)
        }

    suspend fun archiveConversation(conversationId: Long) =
        setConversationArchived(conversationId, true)

    suspend fun toggleMuteConversation(conversationId: Long, currentlyMuted: Boolean) =
        withContext(Dispatchers.IO) {
            dao.setConversationMuted(conversationId, !currentlyMuted)
        }

    suspend fun markSessionVerified(conversationId: Long) = withContext(Dispatchers.IO) {
        dao.setConversationVerified(conversationId, true)
    }

    suspend fun updateBridgeConnection(
        network: SocialNetwork,
        handle: String,
        connected: Boolean
    ) = withContext(Dispatchers.IO) {
        dao.upsertBridge(
            BridgeAccountEntity(
                networkId = network.id,
                accountHandle = handle.trim(),
                isConnected = connected,
                lastSyncedEpochMs = if (connected) System.currentTimeMillis() else 0L
            )
        )
    }

    suspend fun verifyVaultIntegrity(): Int = withContext(Dispatchers.IO) {
        val allMessages = dao.getAllMessagesOnce()
        var verifiedCount = 0
        for (msg in allMessages) {
            val result = cryptoEngine.decrypt(
                cipherTextBase64 = msg.cipherTextBase64,
                ivBase64 = msg.ivBase64,
                associatedData = "conv:${msg.conversationId}"
            )
            if (result.isSuccess) {
                verifiedCount++
            }
        }
        verifiedCount
    }

    fun getSafetyNumber(peerHandle: String, network: SocialNetwork): String {
        return cryptoEngine.generateSafetyNumber(peerHandle, network.id)
    }

    fun getDeviceFingerprint(deviceSeed: String): String {
        return cryptoEngine.deriveDeviceFingerprint(deviceSeed)
    }
}

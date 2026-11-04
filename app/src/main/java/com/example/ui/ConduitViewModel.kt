package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoEngine
import com.example.data.BridgeAccountEntity
import com.example.data.ConduitDatabase
import com.example.data.ConduitRepository
import com.example.data.DecryptedConversation
import com.example.data.DecryptedMessage
import com.example.data.InboxDisplayStyle
import com.example.data.PreferencesRepository
import com.example.data.SocialNetwork
import com.example.data.VaultSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ConduitTab(val route: String) {
    INBOX("inbox"),
    BRIDGES("bridges"),
    SECURITY("security")
}

/**
 * Beeper-style Spaces / Custom Tabs along the top Spacebar & Sidebar Drawer.
 */
enum class BeeperSpace(val id: String) {
    ALL("all"),
    UNREAD("unread"),
    DMS("dms"),
    GROUPS("groups"),
    ARCHIVED("archived")
}

data class InboxUiState(
    val pinnedConversations: List<DecryptedConversation> = emptyList(),
    val feedConversations: List<DecryptedConversation> = emptyList(),
    val conversations: List<DecryptedConversation> = emptyList(),
    val matchedSnippetsByConversationId: Map<Long, String> = emptyMap(),
    val bridges: List<BridgeAccountEntity> = emptyList(),
    val activeBridgeCount: Int = 0,
    val totalUnreadCount: Int = 0,
    val archivedCount: Int = 0,
    val selectedNetwork: SocialNetwork? = null,
    val selectedSpace: BeeperSpace = BeeperSpace.ALL,
    val unreadOnly: Boolean = false,
    val searchQuery: String = "",
    val totalEncryptedMessages: Int = 0,
    val verifiedPeerCount: Int = 0,
    val settings: VaultSettings = VaultSettings(),
    val deviceFingerprint: String = "",
    val statusBannerMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ConduitViewModel(
    private val repository: ConduitRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(ConduitTab.INBOX)
    val currentTab: StateFlow<ConduitTab> = _currentTab.asStateFlow()

    private val _selectedConversationId = MutableStateFlow<Long?>(null)
    val selectedConversationId: StateFlow<Long?> = _selectedConversationId.asStateFlow()

    private val _selectedNetwork = MutableStateFlow<SocialNetwork?>(null)
    private val _selectedSpace = MutableStateFlow(BeeperSpace.ALL)
    private val _unreadOnly = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _statusBanner = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            preferencesRepository.ensureDeviceSeedInitialized()
            repository.initializeBootstrapVaultIfEmpty()
        }
    }

    private val filterTupleFlow = combine(
        _selectedNetwork,
        _selectedSpace,
        _unreadOnly,
        _searchQuery,
        _statusBanner
    ) { network, space, unreadOnly, query, banner ->
        FilterParams(network, space, unreadOnly, query, banner)
    }

    private val vaultDataFlow = combine(
        repository.allConversationsFlow,
        repository.allDecryptedMessagesFlow,
        repository.bridgesFlow
    ) { conversations, messages, bridges ->
        Triple(conversations, messages, bridges)
    }

    val uiState: StateFlow<InboxUiState> = combine(
        vaultDataFlow,
        repository.totalEncryptedMessagesFlow,
        preferencesRepository.settingsFlow,
        filterTupleFlow
    ) { (allConversations, allMessages, bridges), totalMsgs, settings, filters ->
        val connectedNetworkIds = bridges.filter { it.isConnected }.map { it.networkId }.toSet()

        val networkFiltered = allConversations.filter { conv ->
            val bridgeActiveMatch = connectedNetworkIds.isEmpty() || conv.network.id in connectedNetworkIds
            val networkMatch = filters.network == null || conv.network == filters.network
            bridgeActiveMatch && networkMatch
        }

        val queryTrimmed = filters.query.trim()
        val isSearching = queryTrimmed.isNotEmpty()
        val isEffectiveUnreadOnly = filters.unreadOnly || filters.space == BeeperSpace.UNREAD

        // Index full decrypted message history by conversationId when searching
        val matchedSnippetMap = mutableMapOf<Long, String>()
        if (isSearching) {
            for (msg in allMessages) {
                if (msg.conversationId !in matchedSnippetMap &&
                    msg.plainText.contains(queryTrimmed, ignoreCase = true)
                ) {
                    val prefix = if (msg.isOutgoing) "You: " else ""
                    matchedSnippetMap[msg.conversationId] = "$prefix${msg.plainText}"
                }
            }
        }

        val spaceFiltered = networkFiltered.filter { conv ->
            val archiveMatch = if (filters.space == BeeperSpace.ARCHIVED) {
                conv.isArchived
            } else {
                !conv.isArchived
            }
            val spaceTypeMatch = when (filters.space) {
                BeeperSpace.ALL -> true
                BeeperSpace.UNREAD -> conv.unreadCount > 0
                BeeperSpace.DMS -> !conv.isGroup
                BeeperSpace.GROUPS -> conv.isGroup
                BeeperSpace.ARCHIVED -> true
            }
            val unreadToggleMatch = !isEffectiveUnreadOnly || conv.unreadCount > 0

            val searchMatch = !isSearching ||
                conv.contactName.contains(queryTrimmed, ignoreCase = true) ||
                conv.contactHandle.contains(queryTrimmed, ignoreCase = true) ||
                conv.lastPlainPreview.contains(queryTrimmed, ignoreCase = true) ||
                conv.network.displayName.contains(queryTrimmed, ignoreCase = true) ||
                conv.id in matchedSnippetMap

            archiveMatch && spaceTypeMatch && unreadToggleMatch && searchMatch
        }

        // Beeper signature behavior: Pinned chats appear as circular Favorites at the top
        // and are removed from the main vertical feed below (when not searching or viewing Archive)
        val showFavoritesDock = !isSearching && filters.space != BeeperSpace.ARCHIVED
        val pinnedList = if (showFavoritesDock) {
            spaceFiltered.filter { it.isPinned }
        } else {
            emptyList()
        }
        val feedList = if (showFavoritesDock) {
            spaceFiltered.filterNot { it.isPinned }
        } else {
            spaceFiltered
        }

        val activeUnarchived = networkFiltered.filterNot { it.isArchived }

        InboxUiState(
            pinnedConversations = pinnedList,
            feedConversations = feedList,
            conversations = spaceFiltered,
            matchedSnippetsByConversationId = matchedSnippetMap,
            bridges = bridges,
            activeBridgeCount = bridges.count { it.isConnected },
            totalUnreadCount = activeUnarchived.sumOf { it.unreadCount },
            archivedCount = networkFiltered.count { it.isArchived },
            selectedNetwork = filters.network,
            selectedSpace = filters.space,
            unreadOnly = isEffectiveUnreadOnly,
            searchQuery = filters.query,
            totalEncryptedMessages = totalMsgs,
            verifiedPeerCount = allConversations.count { it.isKeyVerified },
            settings = settings,
            deviceFingerprint = repository.getDeviceFingerprint(settings.deviceSeed),
            statusBannerMessage = filters.banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InboxUiState()
    )

    val activeConversation: StateFlow<DecryptedConversation?> =
        _selectedConversationId.flatMapLatest { convId ->
            if (convId == null) flowOf(null) else repository.observeConversation(convId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val activeMessages: StateFlow<List<DecryptedMessage>> =
        _selectedConversationId.flatMapLatest { convId ->
            if (convId == null) flowOf(emptyList()) else repository.observeMessages(convId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun selectTab(tab: ConduitTab) {
        _currentTab.value = tab
    }

    fun openConversation(conversationId: Long) {
        _selectedConversationId.value = conversationId
        viewModelScope.launch {
            repository.markConversationRead(conversationId)
        }
    }

    fun closeConversation() {
        _selectedConversationId.value = null
    }

    fun setNetworkFilter(network: SocialNetwork?) {
        _selectedNetwork.value = if (_selectedNetwork.value == network) null else network
        _currentTab.value = ConduitTab.INBOX
    }

    fun selectSpace(space: BeeperSpace) {
        _selectedSpace.value = space
        if (space == BeeperSpace.UNREAD) {
            _unreadOnly.value = true
        } else if (space == BeeperSpace.ALL || space == BeeperSpace.ARCHIVED) {
            _unreadOnly.value = false
        }
        _currentTab.value = ConduitTab.INBOX
    }

    fun toggleUnreadOnly() {
        val currentlyUnread = _unreadOnly.value || _selectedSpace.value == BeeperSpace.UNREAD
        val nextUnread = !currentlyUnread
        _unreadOnly.value = nextUnread
        if (!nextUnread && _selectedSpace.value == BeeperSpace.UNREAD) {
            _selectedSpace.value = BeeperSpace.ALL
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun sendMessage(plainText: String) {
        val convId = _selectedConversationId.value ?: return
        if (plainText.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(convId, plainText, isOutgoing = true)
        }
    }

    fun receiveTestBridgeReply() {
        val conv = activeConversation.value ?: return
        val replies = listOf(
            "Got it — received cleanly over the ${conv.network.displayName} E2EE bridge.",
            "Confirmed. Checksum and GCM auth tag verified on my end.",
            "Sounds great, let's sync up right after standup."
        )
        val picked = replies[(System.currentTimeMillis() % replies.size).toInt()]
        viewModelScope.launch {
            repository.sendMessage(conv.id, picked, isOutgoing = false)
            repository.markConversationRead(conv.id)
        }
    }

    fun createConversation(
        contactName: String,
        contactHandle: String,
        network: SocialNetwork,
        initialMessage: String
    ) {
        if (contactName.isBlank()) return
        viewModelScope.launch {
            val currentBridge = uiState.value.bridges.firstOrNull { it.networkId == network.id }
            if (currentBridge == null || !currentBridge.isConnected) {
                repository.updateBridgeConnection(
                    network = network,
                    handle = currentBridge?.accountHandle?.ifBlank { "@you" } ?: "@you",
                    connected = true
                )
            }
            val newId = repository.createNewConversation(
                contactName = contactName,
                contactHandle = contactHandle,
                network = network,
                initialMessage = initialMessage
            )
            _selectedSpace.value = BeeperSpace.ALL
            _unreadOnly.value = false
            _currentTab.value = ConduitTab.INBOX
            _selectedConversationId.value = newId
        }
    }

    fun togglePin(conversation: DecryptedConversation) {
        viewModelScope.launch {
            repository.togglePinConversation(conversation.id, conversation.isPinned)
        }
    }

    fun toggleArchive(conversation: DecryptedConversation) {
        viewModelScope.launch {
            val nextArchived = !conversation.isArchived
            repository.setConversationArchived(conversation.id, nextArchived)
            if (nextArchived && _selectedConversationId.value == conversation.id) {
                _selectedConversationId.value = null
            }
        }
    }

    fun archiveConversation(conversationId: Long) {
        viewModelScope.launch {
            repository.archiveConversation(conversationId)
            if (_selectedConversationId.value == conversationId) {
                _selectedConversationId.value = null
            }
        }
    }

    fun toggleMute(conversation: DecryptedConversation) {
        viewModelScope.launch {
            repository.toggleMuteConversation(conversation.id, conversation.isMuted)
        }
    }

    fun markConversationVerified(conversationId: Long) {
        viewModelScope.launch {
            repository.markSessionVerified(conversationId)
        }
    }

    fun updateBridge(network: SocialNetwork, handle: String, connected: Boolean) {
        viewModelScope.launch {
            repository.updateBridgeConnection(network, handle, connected)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkMode(enabled)
        }
    }

    fun setMaskInboxPreviews(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setMaskInboxPreviews(enabled)
        }
    }

    fun setDisplayStyle(style: InboxDisplayStyle) {
        viewModelScope.launch {
            preferencesRepository.setDisplayStyle(style)
        }
    }

    fun runVaultIntegrityAudit(onVerifiedCount: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.verifyVaultIntegrity()
            onVerifiedCount(count)
        }
    }

    fun computeSafetyNumber(peerHandle: String, network: SocialNetwork): String {
        return repository.getSafetyNumber(peerHandle, network)
    }

    private data class FilterParams(
        val network: SocialNetwork?,
        val space: BeeperSpace,
        val unreadOnly: Boolean,
        val query: String,
        val banner: String?
    )

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val db = ConduitDatabase.getInstance(appContext)
                    val crypto = CryptoEngine()
                    val repo = ConduitRepository(db.conduitDao(), crypto)
                    val prefs = PreferencesRepository(appContext)
                    return ConduitViewModel(repo, prefs) as T
                }
            }
    }
}

package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.CryptoEngine
import com.example.data.ConduitDatabase
import com.example.data.ConduitRepository
import com.example.data.PreferencesRepository
import com.example.ui.ConduitViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: ConduitDatabase
    private lateinit var cryptoEngine: CryptoEngine
    private lateinit var repository: ConduitRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ConduitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cryptoEngine = CryptoEngine()
        repository = ConduitRepository(db.conduitDao(), cryptoEngine)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Conduit", appName)
    }

    @Test
    fun `encrypt and decrypt roundtrip enforces AES-256-GCM AAD integrity`() {
        val secretText = "Zero-knowledge social inbox message"
        val envelope = cryptoEngine.encrypt(secretText, associatedData = "conv:42")
        assertNotEquals(secretText, envelope.cipherTextBase64)

        val decrypted = cryptoEngine.decrypt(
            cipherTextBase64 = envelope.cipherTextBase64,
            ivBase64 = envelope.ivBase64,
            associatedData = "conv:42"
        )
        assertTrue(decrypted.isSuccess)
        assertEquals(secretText, decrypted.getOrThrow())

        val tampered = cryptoEngine.decrypt(
            cipherTextBase64 = envelope.cipherTextBase64,
            ivBase64 = envelope.ivBase64,
            associatedData = "conv:999"
        )
        assertTrue(tampered.isFailure)
    }

    @Test
    fun `top search across message history and unread toggle filter inbox accurately`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository.initializeBootstrapVaultIfEmpty()
        val prefs = PreferencesRepository(context)
        val viewModel = ConduitViewModel(repository, prefs)

        val collectJob = launch {
            viewModel.uiState.collect {}
        }

        try {
            val initialState = withTimeout(5_000) {
                viewModel.uiState.first { it.conversations.size == 6 }
            }
            assertFalse(initialState.unreadOnly)

            // Toggle Unread-only filter -> only unread conversations remain (4 of 6)
            viewModel.toggleUnreadOnly()
            val unreadState = withTimeout(5_000) {
                viewModel.uiState.first { it.unreadOnly && it.conversations.size == 4 }
            }
            assertTrue(unreadState.conversations.all { it.unreadCount > 0 })

            // Toggle Unread-only off -> back to all 6 active conversations
            viewModel.toggleUnreadOnly()
            val restoredState = withTimeout(5_000) {
                viewModel.uiState.first { !it.unreadOnly && it.conversations.size == 6 }
            }
            assertEquals(6, restoredState.conversations.size)

            // Search an earlier encrypted message inside Marcus Vance's history ("Heathrow")
            viewModel.setSearchQuery("Heathrow")
            val searchState = withTimeout(5_000) {
                viewModel.uiState.first {
                    it.searchQuery == "Heathrow" && it.conversations.size == 1
                }
            }
            assertEquals("Marcus Vance", searchState.conversations.first().contactName)
            assertTrue(
                searchState.matchedSnippetsByConversationId.values.any {
                    it.contains("Heathrow", ignoreCase = true)
                }
            )
        } finally {
            collectJob.cancel()
        }
    }
}

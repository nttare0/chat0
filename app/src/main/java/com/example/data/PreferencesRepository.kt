package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "conduit_preferences"
)

enum class InboxDisplayStyle {
    MINIMAL,
    PRO
}

data class VaultSettings(
    val isDarkMode: Boolean = true,
    val maskInboxPreviews: Boolean = false,
    val displayStyle: InboxDisplayStyle = InboxDisplayStyle.PRO,
    val deviceSeed: String = "conduit-default-seed"
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val MASK_PREVIEWS = booleanPreferencesKey("mask_inbox_previews")
        val DISPLAY_STYLE = stringPreferencesKey("inbox_display_style")
        val DEVICE_SEED = stringPreferencesKey("device_identity_seed")
    }

    val settingsFlow: Flow<VaultSettings> = context.dataStore.data.map { prefs ->
        val rawStyle = prefs[Keys.DISPLAY_STYLE]
        val style = if (rawStyle == InboxDisplayStyle.MINIMAL.name) {
            InboxDisplayStyle.MINIMAL
        } else {
            InboxDisplayStyle.PRO
        }
        VaultSettings(
            isDarkMode = prefs[Keys.DARK_MODE] ?: true,
            maskInboxPreviews = prefs[Keys.MASK_PREVIEWS] ?: false,
            displayStyle = style,
            deviceSeed = prefs[Keys.DEVICE_SEED] ?: "conduit-local-vault-01"
        )
    }

    suspend fun ensureDeviceSeedInitialized() {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.DEVICE_SEED] == null) {
                prefs[Keys.DEVICE_SEED] = UUID.randomUUID().toString()
            }
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun setMaskInboxPreviews(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MASK_PREVIEWS] = enabled
        }
    }

    suspend fun setDisplayStyle(style: InboxDisplayStyle) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISPLAY_STYLE] = style.name
        }
    }
}

package com.jameszp98.aiquota.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jameszp98.aiquota.domain.ProviderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persistent settings storage using DataStore.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
        val REFRESH_INTERVAL_MINUTES = intPreferencesKey("refresh_interval_minutes")
        val ENABLED_PROVIDERS = stringSetPreferencesKey("enabled_providers")
    }

    val demoMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEMO_MODE] ?: false
    }

    val refreshIntervalMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.REFRESH_INTERVAL_MINUTES] ?: DEFAULT_REFRESH_INTERVAL
    }

    val enabledProviderIds: Flow<Set<ProviderId>> = context.dataStore.data.map { prefs ->
        val ids = prefs[Keys.ENABLED_PROVIDERS] ?: setOf("cursor")
        ids.map { ProviderId(it) }.toSet()
    }

    suspend fun setDemoMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEMO_MODE] = enabled
        }
    }

    suspend fun setRefreshInterval(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REFRESH_INTERVAL_MINUTES] = minutes.coerceIn(15, 120)
        }
    }

    suspend fun setEnabledProviders(providerIds: Set<ProviderId>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLED_PROVIDERS] = providerIds.map { it.value }.toSet()
        }
    }

    companion object {
        const val DEFAULT_REFRESH_INTERVAL = 30
    }
}

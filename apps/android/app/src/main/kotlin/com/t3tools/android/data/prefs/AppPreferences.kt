package com.t3tools.android.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "t3_prefs")

private val KEY_SAVED_SERVERS = stringPreferencesKey("saved_servers")
private val KEY_LAST_SERVER = stringPreferencesKey("last_server")

@Serializable
data class SavedServer(val url: String, val label: String)

class AppPreferences(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val savedServers: Flow<List<SavedServer>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_SERVERS]?.let {
            runCatching { json.decodeFromString<List<SavedServer>>(it) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    val lastServerUrl: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SERVER]
    }

    suspend fun saveServer(server: SavedServer) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SAVED_SERVERS]?.let {
                runCatching { json.decodeFromString<List<SavedServer>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            val updated = (current.filter { it.url != server.url } + server).takeLast(10)
            prefs[KEY_SAVED_SERVERS] = json.encodeToString(updated)
        }
    }

    suspend fun removeServer(url: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SAVED_SERVERS]?.let {
                runCatching { json.decodeFromString<List<SavedServer>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            prefs[KEY_SAVED_SERVERS] = json.encodeToString(current.filter { it.url != url })
        }
    }

    suspend fun setLastServerUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[KEY_LAST_SERVER] = url }
    }
}

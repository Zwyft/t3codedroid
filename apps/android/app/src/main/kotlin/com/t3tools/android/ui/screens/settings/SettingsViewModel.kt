package com.t3tools.android.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t3tools.android.T3Application
import com.t3tools.android.data.prefs.SavedServer
import com.t3tools.android.data.rpc.WsConnectionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val savedServers: List<SavedServer> = emptyList(),
    val currentServerUrl: String? = null,
    val isConnected: Boolean = false,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as T3Application).container

    val uiState: StateFlow<SettingsUiState> = combine(
        container.appPreferences.savedServers,
        container.appPreferences.lastServerUrl,
        container.connectionManager.connectionState,
    ) { servers, lastUrl, connState ->
        SettingsUiState(
            savedServers = servers,
            currentServerUrl = lastUrl,
            isConnected = connState is WsConnectionState.Connected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun removeServer(url: String) {
        viewModelScope.launch { container.appPreferences.removeServer(url) }
    }

    fun disconnect() {
        container.connectionManager.disconnect()
        viewModelScope.launch { container.appPreferences.setLastServerUrl("") }
    }
}

package com.t3tools.android.ui.screens.connect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t3tools.android.T3Application
import com.t3tools.android.data.prefs.SavedServer
import com.t3tools.android.data.rpc.WsConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectUiState(
    val serverUrl: String = "",
    val isConnecting: Boolean = false,
    val error: String? = null,
)

class ConnectViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as T3Application).container

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<WsConnectionState> = container.connectionManager.connectionState
        .stateIn(viewModelScope, SharingStarted.Eagerly, WsConnectionState.Idle)

    val savedServers = container.appPreferences.savedServers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url, error = null) }
    }

    fun connect(url: String = _uiState.value.serverUrl, onConnected: () -> Unit) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(error = "Please enter a server URL") }
            return
        }
        _uiState.update { it.copy(isConnecting = true, error = null) }
        container.connectionManager.connect(trimmed)

        viewModelScope.launch {
            container.connectionManager.connectionState.collect { state ->
                when (state) {
                    is WsConnectionState.Connected -> {
                        _uiState.update { it.copy(isConnecting = false) }
                        container.appPreferences.saveServer(SavedServer(url = trimmed, label = trimmed))
                        container.appPreferences.setLastServerUrl(trimmed)
                        onConnected()
                    }
                    is WsConnectionState.Failed -> {
                        _uiState.update { it.copy(isConnecting = false, error = state.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun disconnect() {
        container.connectionManager.disconnect()
        container.appPreferences.run {
            viewModelScope.launch { setLastServerUrl("") }
        }
    }
}

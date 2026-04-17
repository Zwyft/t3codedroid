package com.t3tools.android.data.rpc

import android.util.Log
import com.t3tools.android.data.model.RpcServerMessage
import com.t3tools.android.data.model.parseRpcServerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "WsConnectionManager"

sealed class WsConnectionState {
    object Idle : WsConnectionState()
    object Connecting : WsConnectionState()
    data class Connected(val serverUrl: String) : WsConnectionState()
    data class Reconnecting(val attempt: Int) : WsConnectionState()
    data class Failed(val message: String) : WsConnectionState()
    object Disconnected : WsConnectionState()
}

private val RECONNECT_DELAYS_MS = listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 64_000L)

class WsConnectionManager(private val okHttpClient: OkHttpClient) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _connectionState = MutableStateFlow<WsConnectionState>(WsConnectionState.Idle)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<RpcServerMessage>(extraBufferCapacity = 512)
    val incomingMessages: SharedFlow<RpcServerMessage> = _incomingMessages.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var activeServerUrl: String? = null
    private val isConnecting = AtomicBoolean(false)
    private var reconnectJob: Job? = null

    fun connect(serverUrl: String) {
        activeServerUrl = normalizeWsUrl(serverUrl)
        reconnectJob?.cancel()
        reconnectJob = scope.launch { connectWithRetry(0) }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = WsConnectionState.Disconnected
    }

    fun send(json: String): Boolean {
        return webSocket?.send(json) ?: false
    }

    private suspend fun connectWithRetry(attempt: Int) {
        val url = activeServerUrl ?: return
        if (attempt > 0) {
            val delayMs = RECONNECT_DELAYS_MS.getOrElse(attempt - 1) { RECONNECT_DELAYS_MS.last() }
            _connectionState.value = WsConnectionState.Reconnecting(attempt)
            delay(delayMs)
        } else {
            _connectionState.value = WsConnectionState.Connecting
        }

        if (!isConnecting.compareAndSet(false, true)) return

        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnecting.set(false)
                _connectionState.value = WsConnectionState.Connected(url)
                Log.d(TAG, "Connected to $url")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val jsonObj = Json.parseToJsonElement(text).jsonObject
                    val msg = parseRpcServerMessage(jsonObj)
                    if (msg != null) {
                        _incomingMessages.tryEmit(msg)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse message: ${e.message}")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnecting.set(false)
                Log.w(TAG, "WebSocket failure: ${t.message}")
                _connectionState.value = WsConnectionState.Failed(t.message ?: "Unknown error")
                scheduleReconnect(attempt + 1)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnecting.set(false)
                if (_connectionState.value is WsConnectionState.Disconnected) return
                Log.d(TAG, "WebSocket closed: $code $reason")
                scheduleReconnect(attempt + 1)
            }
        })
    }

    private fun scheduleReconnect(nextAttempt: Int) {
        if (nextAttempt > RECONNECT_DELAYS_MS.size + 1) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch { connectWithRetry(nextAttempt) }
    }

    private fun normalizeWsUrl(input: String): String {
        val base = if (input.startsWith("ws://") || input.startsWith("wss://")) {
            input.trimEnd('/')
        } else {
            "ws://$input".trimEnd('/')
        }
        return if (base.endsWith("/ws")) base else "$base/ws"
    }
}

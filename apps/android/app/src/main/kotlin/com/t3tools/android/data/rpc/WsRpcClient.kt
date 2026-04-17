package com.t3tools.android.data.rpc

import android.util.Log
import com.t3tools.android.data.model.RpcChunk
import com.t3tools.android.data.model.RpcExit
import com.t3tools.android.data.model.RpcProtocolError
import com.t3tools.android.data.model.RpcRequest
import com.t3tools.android.util.IdGenerator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "WsRpcClient"

class RpcException(errorMessage: String, val jsonCause: JsonElement? = null) : Exception(errorMessage)

class WsRpcClient(private val connectionManager: WsConnectionManager) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<JsonElement>>()
    private val activeSubscriptions = ConcurrentHashMap<String, Channel<JsonElement>>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    init {
        scope.launch {
            connectionManager.incomingMessages.collect { message ->
                when (message) {
                    is RpcChunk -> {
                        activeSubscriptions[message.requestId]?.trySend(message.value)
                    }
                    is RpcExit -> {
                        val deferred = pendingRequests.remove(message.requestId)
                        if (deferred != null) {
                            if (message.exitValue.tag == "Success") {
                                deferred.complete(message.exitValue.value ?: JsonNull)
                            } else {
                                deferred.completeExceptionally(
                                    RpcException("RPC failure", message.exitValue.cause)
                                )
                            }
                        } else {
                            activeSubscriptions.remove(message.requestId)?.close()
                        }
                    }
                    is RpcProtocolError -> {
                        Log.w(TAG, "Protocol error: ${message.message}")
                        pendingRequests.values.forEach {
                            it.completeExceptionally(RpcException("Protocol error: ${message.message}"))
                        }
                        pendingRequests.clear()
                        activeSubscriptions.values.forEach { it.close() }
                        activeSubscriptions.clear()
                    }
                }
            }
        }
    }

    suspend fun <T> request(
        method: String,
        payload: JsonElement,
        deserializer: KSerializer<T>,
    ): T {
        val requestId = IdGenerator.newId()
        val deferred = CompletableDeferred<JsonElement>()
        pendingRequests[requestId] = deferred
        sendRequest(requestId, method, payload)
        val result = deferred.await()
        return json.decodeFromJsonElement(deserializer, result)
    }

    fun <T> subscribe(
        method: String,
        payload: JsonElement,
        deserializer: KSerializer<T>,
    ): Flow<T> = channelFlow {
        val requestId = IdGenerator.newId()
        val channel = Channel<JsonElement>(Channel.UNLIMITED)
        activeSubscriptions[requestId] = channel
        sendRequest(requestId, method, payload)
        try {
            for (item in channel) {
                send(json.decodeFromJsonElement(deserializer, item))
            }
        } finally {
            activeSubscriptions.remove(requestId)?.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun sendRequest(requestId: String, method: String, payload: JsonElement) {
        val frame = RpcRequest(id = requestId, tag = method, payload = payload)
        val text = json.encodeToString(RpcRequest.serializer(), frame)
        connectionManager.send(text)
    }
}

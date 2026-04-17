package com.t3tools.android.domain

import com.t3tools.android.data.model.OrchestrationThreadStreamItem
import com.t3tools.android.data.model.ThreadApprovalRespondCommand
import com.t3tools.android.data.model.ThreadCreateCommand
import com.t3tools.android.data.model.ThreadTurnInterruptCommand
import com.t3tools.android.data.model.ThreadTurnStartCommand
import com.t3tools.android.data.rpc.WsRpcClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.math.min
import kotlin.math.pow

class ThreadRepository(private val rpcClient: WsRpcClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun subscribeThread(threadId: String): Flow<OrchestrationThreadStreamItem> =
        rpcClient.subscribe(
            method = "orchestration.subscribeThread",
            payload = buildJsonObject { put("threadId", threadId.toJsonPrimitive()) },
            deserializer = OrchestrationThreadStreamItem.serializer(),
        ).retryWhen { _, attempt ->
            delay(min(250L * 2.0.pow(attempt.toInt()).toLong(), 16_000L))
            true
        }

    suspend fun createThread(command: ThreadCreateCommand) {
        rpcClient.request(
            method = "orchestration.dispatchCommand",
            payload = json.encodeToJsonElement(command),
            deserializer = kotlinx.serialization.json.JsonElement.serializer(),
        )
    }

    suspend fun startTurn(command: ThreadTurnStartCommand) {
        rpcClient.request(
            method = "orchestration.dispatchCommand",
            payload = json.encodeToJsonElement(command),
            deserializer = kotlinx.serialization.json.JsonElement.serializer(),
        )
    }

    suspend fun interruptTurn(command: ThreadTurnInterruptCommand) {
        rpcClient.request(
            method = "orchestration.dispatchCommand",
            payload = json.encodeToJsonElement(command),
            deserializer = kotlinx.serialization.json.JsonElement.serializer(),
        )
    }

    suspend fun respondToApproval(command: ThreadApprovalRespondCommand) {
        rpcClient.request(
            method = "orchestration.dispatchCommand",
            payload = json.encodeToJsonElement(command),
            deserializer = kotlinx.serialization.json.JsonElement.serializer(),
        )
    }
}

private fun String.toJsonPrimitive() = JsonPrimitive(this)

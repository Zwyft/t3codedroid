package com.t3tools.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Effect RPC wire protocol frames
// Client → Server: Request
// Server → Client: Chunk (stream item) or Exit (unary/stream end)

@Serializable
data class RpcRequest(
    @SerialName("_tag") val frameTag: String = "Request",
    val id: String,
    val tag: String,         // RPC method name e.g. "orchestration.subscribeShell"
    val payload: JsonElement,
)

@Serializable
data class RpcExitValue(
    @SerialName("_tag") val tag: String,   // "Success" or "Failure"
    val value: JsonElement? = null,
    val cause: JsonElement? = null,
)

sealed class RpcServerMessage {
    abstract val requestId: String
}

data class RpcChunk(
    override val requestId: String,
    val value: JsonElement,
) : RpcServerMessage()

data class RpcExit(
    override val requestId: String,
    val exitValue: RpcExitValue,
) : RpcServerMessage()

data class RpcProtocolError(
    override val requestId: String = "",
    val message: String,
) : RpcServerMessage()

fun parseRpcServerMessage(json: JsonObject): RpcServerMessage? {
    val msgTag = json["_tag"]?.jsonPrimitive?.content ?: return null
    return when (msgTag) {
        "Chunk" -> {
            val requestId = json["requestId"]?.jsonPrimitive?.content ?: return null
            val value = json["value"] ?: return null
            RpcChunk(requestId = requestId, value = value)
        }
        "Exit" -> {
            val requestId = json["requestId"]?.jsonPrimitive?.content ?: return null
            val valueObj = json["value"]?.jsonObject ?: return null
            val exitTag = valueObj["_tag"]?.jsonPrimitive?.content ?: return null
            val innerValue = valueObj["value"]
            val cause = valueObj["cause"]
            RpcExit(
                requestId = requestId,
                exitValue = RpcExitValue(tag = exitTag, value = innerValue, cause = cause),
            )
        }
        "ClientProtocolError", "Defect" -> {
            RpcProtocolError(message = msgTag)
        }
        else -> null
    }
}

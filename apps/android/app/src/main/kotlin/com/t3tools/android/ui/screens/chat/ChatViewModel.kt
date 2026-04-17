package com.t3tools.android.ui.screens.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.t3tools.android.T3Application
import com.t3tools.android.data.model.OrchestrationEvent
import com.t3tools.android.data.model.OrchestrationMessage
import com.t3tools.android.data.model.OrchestrationSession
import com.t3tools.android.data.model.OrchestrationSessionStatus
import com.t3tools.android.data.model.OrchestrationThreadActivity
import com.t3tools.android.data.model.OrchestrationThreadStreamItem
import com.t3tools.android.data.model.ProviderApprovalDecision
import com.t3tools.android.data.model.RuntimeMode
import com.t3tools.android.data.model.ThreadApprovalRespondCommand
import com.t3tools.android.data.model.ThreadTurnInterruptCommand
import com.t3tools.android.data.model.ThreadTurnStartCommand
import com.t3tools.android.data.model.TurnMessage
import com.t3tools.android.ui.navigation.Route
import com.t3tools.android.util.IdGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

data class PendingApprovalUiModel(
    val requestId: String,
    val summary: String,
    val kind: String,
    val tone: String,
)

data class ChatUiState(
    val threadTitle: String = "",
    val branch: String? = null,
    val modelLabel: String = "",
    val runtimeMode: RuntimeMode = RuntimeMode.full_access,
    val messages: List<OrchestrationMessage> = emptyList(),
    val activities: List<OrchestrationThreadActivity> = emptyList(),
    val pendingApprovals: List<PendingApprovalUiModel> = emptyList(),
    val session: OrchestrationSession? = null,
    val composerText: String = "",
    val error: String? = null,
    val workspaceRoot: String? = null,
) {
    val isRunning: Boolean
        get() = session?.status == OrchestrationSessionStatus.running ||
            session?.status == OrchestrationSessionStatus.starting
}

class ChatViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val container = (app as T3Application).container
    private val threadId: String = savedStateHandle[Route.Chat.threadIdArg] ?: ""

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.threadRepository.subscribeThread(threadId).collect { item ->
                when (item) {
                    is OrchestrationThreadStreamItem.Snapshot -> {
                        val thread = item.snapshot.thread
                        val workspaceRoot = container.serverRepository.shellState.value.projects
                            .find { it.id == thread.projectId }?.workspaceRoot
                        _uiState.update {
                            ChatUiState(
                                threadTitle = thread.title,
                                branch = thread.branch,
                                modelLabel = "${thread.modelSelection.provider}/${thread.modelSelection.model}",
                                runtimeMode = thread.runtimeMode,
                                messages = thread.messages,
                                activities = thread.activities,
                                pendingApprovals = extractPendingApprovals(thread.activities),
                                session = thread.session,
                                composerText = it.composerText,
                                workspaceRoot = workspaceRoot,
                            )
                        }
                    }
                    is OrchestrationThreadStreamItem.Event -> {
                        _uiState.update { current -> applyEvent(current, item.event) }
                    }
                }
            }
        }
    }

    private fun applyEvent(current: ChatUiState, event: OrchestrationEvent): ChatUiState =
        when (event.type) {
            "thread.message-sent" -> {
                val payload = event.payload.jsonObject
                val msg = OrchestrationMessage(
                    id = payload["messageId"]?.jsonPrimitive?.content ?: IdGenerator.newId(),
                    role = payload["role"]?.jsonPrimitive?.content ?: "assistant",
                    text = payload["text"]?.jsonPrimitive?.content ?: "",
                    turnId = payload["turnId"]?.jsonPrimitive?.content,
                    streaming = payload["streaming"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                    createdAt = payload["createdAt"]?.jsonPrimitive?.content ?: "",
                    updatedAt = payload["updatedAt"]?.jsonPrimitive?.content ?: "",
                )
                current.copy(messages = current.messages + msg)
            }
            "thread.message.assistant.delta" -> {
                val payload = event.payload.jsonObject
                val messageId = payload["messageId"]?.jsonPrimitive?.content ?: return current
                val delta = payload["delta"]?.jsonPrimitive?.content ?: return current
                current.copy(
                    messages = current.messages.map { msg ->
                        if (msg.id == messageId) msg.copy(text = msg.text + delta) else msg
                    },
                )
            }
            "thread.message.assistant.complete" -> {
                val payload = event.payload.jsonObject
                val messageId = payload["messageId"]?.jsonPrimitive?.content ?: return current
                current.copy(
                    messages = current.messages.map { msg ->
                        if (msg.id == messageId) msg.copy(streaming = false) else msg
                    },
                )
            }
            "thread.session-set" -> {
                val payload = event.payload.jsonObject
                val sessionObj = payload["session"]?.jsonObject ?: return current
                val statusStr = sessionObj["status"]?.jsonPrimitive?.content ?: "idle"
                // Wire values are lowercase (e.g. "running"), enum entries match
                val status = OrchestrationSessionStatus.entries.firstOrNull { it.name == statusStr }
                    ?: OrchestrationSessionStatus.idle
                current.copy(
                    session = current.session?.copy(status = status)
                        ?: OrchestrationSession(
                            threadId = threadId,
                            status = status,
                            providerName = sessionObj["providerName"]?.jsonPrimitive?.content,
                            updatedAt = Instant.now().toString(),
                        ),
                )
            }
            "thread.activity-appended" -> {
                val payload = event.payload.jsonObject
                val activityObj = payload["activity"]?.jsonObject ?: return current
                val activity = OrchestrationThreadActivity(
                    id = activityObj["id"]?.jsonPrimitive?.content ?: IdGenerator.newId(),
                    tone = activityObj["tone"]?.jsonPrimitive?.content ?: "info",
                    kind = activityObj["kind"]?.jsonPrimitive?.content ?: "",
                    summary = activityObj["summary"]?.jsonPrimitive?.content ?: "",
                    payload = activityObj["payload"] ?: kotlinx.serialization.json.JsonNull,
                    turnId = activityObj["turnId"]?.jsonPrimitive?.content,
                    sequence = activityObj["sequence"]?.jsonPrimitive?.content?.toIntOrNull(),
                    createdAt = activityObj["createdAt"]?.jsonPrimitive?.content ?: "",
                )
                val updated = current.activities + activity
                current.copy(
                    activities = updated,
                    pendingApprovals = extractPendingApprovals(updated),
                )
            }
            "thread.meta-updated" -> {
                val payload = event.payload.jsonObject
                current.copy(
                    threadTitle = payload["title"]?.jsonPrimitive?.content ?: current.threadTitle,
                    branch = payload["branch"]?.jsonPrimitive?.content ?: current.branch,
                )
            }
            else -> current
        }

    private fun extractPendingApprovals(
        activities: List<OrchestrationThreadActivity>,
    ): List<PendingApprovalUiModel> =
        activities
            .filter { it.tone == "approval" }
            .mapNotNull { activity ->
                val requestId = runCatching {
                    activity.payload.jsonObject["requestId"]?.jsonPrimitive?.content
                }.getOrNull() ?: return@mapNotNull null
                PendingApprovalUiModel(
                    requestId = requestId,
                    summary = activity.summary,
                    kind = activity.kind,
                    tone = activity.tone,
                )
            }

    fun onComposerTextChange(text: String) {
        _uiState.update { it.copy(composerText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.composerText.trim()
        if (text.isEmpty() || _uiState.value.isRunning) return
        val runtimeMode = _uiState.value.runtimeMode
        val now = Instant.now().toString()

        _uiState.update { it.copy(composerText = "") }

        viewModelScope.launch {
            try {
                container.threadRepository.startTurn(
                    ThreadTurnStartCommand(
                        commandId = IdGenerator.newId(),
                        threadId = threadId,
                        message = TurnMessage(
                            messageId = IdGenerator.newId(),
                            text = text,
                        ),
                        runtimeMode = runtimeMode,
                        createdAt = now,
                    ),
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun interruptTurn() {
        viewModelScope.launch {
            try {
                container.threadRepository.interruptTurn(
                    ThreadTurnInterruptCommand(
                        commandId = IdGenerator.newId(),
                        threadId = threadId,
                        createdAt = Instant.now().toString(),
                    ),
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun respondToApproval(requestId: String, decision: ProviderApprovalDecision) {
        viewModelScope.launch {
            try {
                container.threadRepository.respondToApproval(
                    ThreadApprovalRespondCommand(
                        commandId = IdGenerator.newId(),
                        threadId = threadId,
                        requestId = requestId,
                        decision = decision,
                        createdAt = Instant.now().toString(),
                    ),
                )
                _uiState.update { current ->
                    current.copy(
                        pendingApprovals = current.pendingApprovals.filter { it.requestId != requestId },
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

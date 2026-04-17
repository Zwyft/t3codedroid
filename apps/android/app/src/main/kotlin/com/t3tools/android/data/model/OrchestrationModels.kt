package com.t3tools.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

@Serializable
enum class RuntimeMode {
    @SerialName("approval-required") approval_required,
    @SerialName("auto-accept-edits") auto_accept_edits,
    @SerialName("full-access") full_access,
}

@Serializable
enum class ProviderInteractionMode {
    @SerialName("default") default,
    @SerialName("plan") plan,
}

@Serializable
enum class OrchestrationSessionStatus {
    @SerialName("idle") idle,
    @SerialName("starting") starting,
    @SerialName("running") running,
    @SerialName("ready") ready,
    @SerialName("interrupted") interrupted,
    @SerialName("stopped") stopped,
    @SerialName("error") error,
}

@Serializable
enum class ProviderApprovalDecision {
    @SerialName("accept") accept,
    @SerialName("acceptForSession") acceptForSession,
    @SerialName("decline") decline,
    @SerialName("cancel") cancel,
}

@Serializable
enum class OrchestrationLatestTurnState {
    @SerialName("running") running,
    @SerialName("interrupted") interrupted,
    @SerialName("completed") completed,
    @SerialName("error") error,
}

// ---------------------------------------------------------------------------
// Model Selection
// ---------------------------------------------------------------------------

@Serializable
data class ModelSelection(
    val provider: String,
    val model: String,
)

// ---------------------------------------------------------------------------
// Session
// ---------------------------------------------------------------------------

@Serializable
data class OrchestrationSession(
    val threadId: String,
    val status: OrchestrationSessionStatus,
    val providerName: String? = null,
    val runtimeMode: RuntimeMode = RuntimeMode.full_access,
    val activeTurnId: String? = null,
    val lastError: String? = null,
    val updatedAt: String,
)

// ---------------------------------------------------------------------------
// Latest Turn
// ---------------------------------------------------------------------------

@Serializable
data class OrchestrationLatestTurn(
    val turnId: String,
    val state: OrchestrationLatestTurnState,
    val requestedAt: String,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val assistantMessageId: String? = null,
)

// ---------------------------------------------------------------------------
// Message
// ---------------------------------------------------------------------------

@Serializable
data class OrchestrationMessage(
    val id: String,
    val role: String,
    val text: String,
    val attachments: List<JsonElement> = emptyList(),
    val turnId: String? = null,
    val streaming: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

// ---------------------------------------------------------------------------
// Activity
// ---------------------------------------------------------------------------

@Serializable
data class OrchestrationThreadActivity(
    val id: String,
    val tone: String,           // "info" | "tool" | "approval" | "error"
    val kind: String,
    val summary: String,
    val payload: JsonElement = JsonNull,
    val turnId: String? = null,
    val sequence: Int? = null,
    val createdAt: String,
)

// ---------------------------------------------------------------------------
// Checkpoint
// ---------------------------------------------------------------------------

@Serializable
data class OrchestrationCheckpointFile(
    val path: String,
    val kind: String,
    val additions: Int,
    val deletions: Int,
)

@Serializable
data class OrchestrationCheckpointSummary(
    val turnId: String,
    val checkpointTurnCount: Int,
    val checkpointRef: String,
    val status: String,
    val files: List<OrchestrationCheckpointFile> = emptyList(),
    val assistantMessageId: String? = null,
    val completedAt: String,
)

// ---------------------------------------------------------------------------
// Proposed Plan
// ---------------------------------------------------------------------------

@Serializable
data class OrchestrationProposedPlan(
    val id: String,
    val turnId: String? = null,
    val planMarkdown: String,
    val implementedAt: String? = null,
    val implementationThreadId: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

// ---------------------------------------------------------------------------
// Thread (detail - used by subscribeThread snapshot)
// ---------------------------------------------------------------------------

@Serializable
data class OrchestrationThread(
    val id: String,
    val projectId: String,
    val title: String,
    val modelSelection: ModelSelection,
    val runtimeMode: RuntimeMode,
    val interactionMode: ProviderInteractionMode = ProviderInteractionMode.default,
    val branch: String? = null,
    val worktreePath: String? = null,
    val latestTurn: OrchestrationLatestTurn? = null,
    val createdAt: String,
    val updatedAt: String,
    val archivedAt: String? = null,
    val deletedAt: String? = null,
    val messages: List<OrchestrationMessage> = emptyList(),
    val proposedPlans: List<OrchestrationProposedPlan> = emptyList(),
    val activities: List<OrchestrationThreadActivity> = emptyList(),
    val checkpoints: List<OrchestrationCheckpointSummary> = emptyList(),
    val session: OrchestrationSession? = null,
)

@Serializable
data class OrchestrationThreadDetailSnapshot(
    val snapshotSequence: Int,
    val thread: OrchestrationThread,
)

// ---------------------------------------------------------------------------
// Shell models (used by subscribeShell)
// ---------------------------------------------------------------------------

@Serializable
data class OrchestrationProjectShell(
    val id: String,
    val title: String,
    val workspaceRoot: String,
    val defaultModelSelection: ModelSelection? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class OrchestrationThreadShell(
    val id: String,
    val projectId: String,
    val title: String,
    val modelSelection: ModelSelection,
    val runtimeMode: RuntimeMode,
    val interactionMode: ProviderInteractionMode = ProviderInteractionMode.default,
    val branch: String? = null,
    val worktreePath: String? = null,
    val latestTurn: OrchestrationLatestTurn? = null,
    val createdAt: String,
    val updatedAt: String,
    val archivedAt: String? = null,
    val session: OrchestrationSession? = null,
    val latestUserMessageAt: String? = null,
    val hasPendingApprovals: Boolean = false,
    val hasPendingUserInput: Boolean = false,
    val hasActionableProposedPlan: Boolean = false,
)

@Serializable
data class OrchestrationShellSnapshot(
    val snapshotSequence: Int,
    val projects: List<OrchestrationProjectShell>,
    val threads: List<OrchestrationThreadShell>,
    val updatedAt: String,
)

// Discriminated by "kind" field
@Serializable
@JsonClassDiscriminator("kind")
sealed class OrchestrationShellStreamItem {
    @Serializable
    @SerialName("snapshot")
    data class Snapshot(val snapshot: OrchestrationShellSnapshot) : OrchestrationShellStreamItem()

    @Serializable
    @SerialName("project-upserted")
    data class ProjectUpserted(
        val sequence: Int,
        val project: OrchestrationProjectShell,
    ) : OrchestrationShellStreamItem()

    @Serializable
    @SerialName("project-removed")
    data class ProjectRemoved(
        val sequence: Int,
        val projectId: String,
    ) : OrchestrationShellStreamItem()

    @Serializable
    @SerialName("thread-upserted")
    data class ThreadUpserted(
        val sequence: Int,
        val thread: OrchestrationThreadShell,
    ) : OrchestrationShellStreamItem()

    @Serializable
    @SerialName("thread-removed")
    data class ThreadRemoved(
        val sequence: Int,
        val threadId: String,
    ) : OrchestrationShellStreamItem()
}

// ---------------------------------------------------------------------------
// Thread stream event (used by subscribeThread)
// We store payload as JsonElement and decode specific fields in ViewModels
// ---------------------------------------------------------------------------

@Serializable
data class OrchestrationEvent(
    val type: String,
    val eventId: String,
    val sequence: Int,
    val occurredAt: String,
    val commandId: String? = null,
    val payload: JsonElement = JsonNull,
)

@Serializable
@JsonClassDiscriminator("kind")
sealed class OrchestrationThreadStreamItem {
    @Serializable
    @SerialName("snapshot")
    data class Snapshot(val snapshot: OrchestrationThreadDetailSnapshot) : OrchestrationThreadStreamItem()

    @Serializable
    @SerialName("event")
    data class Event(val event: OrchestrationEvent) : OrchestrationThreadStreamItem()
}

// ---------------------------------------------------------------------------
// Commands sent to server via orchestration.dispatchCommand
// ---------------------------------------------------------------------------

@Serializable
data class ThreadCreateCommand(
    val type: String = "thread.create",
    val commandId: String,
    val threadId: String,
    val projectId: String,
    val title: String,
    val modelSelection: ModelSelection,
    val runtimeMode: RuntimeMode,
    val interactionMode: ProviderInteractionMode = ProviderInteractionMode.default,
    val branch: String? = null,
    val worktreePath: String? = null,
    val createdAt: String,
)

@Serializable
data class TurnMessage(
    val messageId: String,
    val role: String = "user",
    val text: String,
    val attachments: List<JsonElement> = emptyList(),
)

@Serializable
data class ThreadTurnStartCommand(
    val type: String = "thread.turn.start",
    val commandId: String,
    val threadId: String,
    val message: TurnMessage,
    val runtimeMode: RuntimeMode,
    val interactionMode: ProviderInteractionMode = ProviderInteractionMode.default,
    val createdAt: String,
)

@Serializable
data class ThreadTurnInterruptCommand(
    val type: String = "thread.turn.interrupt",
    val commandId: String,
    val threadId: String,
    val createdAt: String,
)

@Serializable
data class ThreadApprovalRespondCommand(
    val type: String = "thread.approval.respond",
    val commandId: String,
    val threadId: String,
    val requestId: String,
    val decision: ProviderApprovalDecision,
    val createdAt: String,
)

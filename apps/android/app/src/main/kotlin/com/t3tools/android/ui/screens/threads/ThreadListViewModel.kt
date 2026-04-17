package com.t3tools.android.ui.screens.threads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.t3tools.android.T3Application
import com.t3tools.android.data.model.ModelSelection
import com.t3tools.android.data.model.OrchestrationSessionStatus
import com.t3tools.android.data.model.ProviderInteractionMode
import com.t3tools.android.data.model.RuntimeMode
import com.t3tools.android.data.model.ThreadCreateCommand
import com.t3tools.android.data.model.ThreadTurnStartCommand
import com.t3tools.android.data.model.TurnMessage
import com.t3tools.android.ui.navigation.Route
import com.t3tools.android.util.IdGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class ThreadSummaryUiModel(
    val id: String,
    val title: String,
    val branch: String?,
    val modelLabel: String,
    val isRunning: Boolean,
    val hasPendingApprovals: Boolean,
    val hasPendingUserInput: Boolean,
    val updatedAt: String,
)

data class ThreadListUiState(
    val projectTitle: String = "",
    val threads: List<ThreadSummaryUiModel> = emptyList(),
    val availableModels: List<ModelSelection> = emptyList(),
    val showNewThreadSheet: Boolean = false,
    val isCreating: Boolean = false,
)

class ThreadListViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val container = (app as T3Application).container
    private val projectId: String = savedStateHandle[Route.ThreadList.projectIdArg] ?: ""
    private val _extras = MutableStateFlow(ThreadListUiState())

    val uiState: StateFlow<ThreadListUiState> = combine(
        container.serverRepository.shellState,
        _extras,
    ) { shell, extras ->
        val project = shell.projects.find { it.id == projectId }
        val threads = shell.threads
            .filter { it.projectId == projectId && it.archivedAt == null }
            .sortedByDescending { it.updatedAt }
            .map { t ->
                ThreadSummaryUiModel(
                    id = t.id,
                    title = t.title,
                    branch = t.branch,
                    modelLabel = "${t.modelSelection.provider}/${t.modelSelection.model}",
                    isRunning = t.session?.status == OrchestrationSessionStatus.running ||
                        t.session?.status == OrchestrationSessionStatus.starting,
                    hasPendingApprovals = t.hasPendingApprovals,
                    hasPendingUserInput = t.hasPendingUserInput,
                    updatedAt = t.updatedAt,
                )
            }
        val defaultModel = project?.defaultModelSelection
            ?: ModelSelection(provider = "claudeAgent", model = "claude-opus-4-5-20251001")
        extras.copy(
            projectTitle = project?.title ?: projectId,
            threads = threads,
            availableModels = listOf(
                ModelSelection("claudeAgent", "claude-opus-4-5-20251001"),
                ModelSelection("claudeAgent", "claude-sonnet-4-6"),
                ModelSelection("claudeAgent", "claude-haiku-4-5-20251001"),
                defaultModel,
            ).distinctBy { "${it.provider}/${it.model}" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThreadListUiState())

    fun showNewThreadSheet() = _extras.update { it.copy(showNewThreadSheet = true) }
    fun hideNewThreadSheet() = _extras.update { it.copy(showNewThreadSheet = false) }

    fun createThread(
        title: String,
        modelSelection: ModelSelection,
        runtimeMode: RuntimeMode,
        initialMessage: String,
        onCreated: (threadId: String) -> Unit,
    ) {
        val threadId = IdGenerator.newId()
        val messageId = IdGenerator.newId()
        val commandId = IdGenerator.newId()
        val now = Instant.now().toString()

        viewModelScope.launch {
            _extras.update { it.copy(isCreating = true) }
            try {
                container.threadRepository.createThread(
                    ThreadCreateCommand(
                        commandId = IdGenerator.newId(),
                        threadId = threadId,
                        projectId = projectId,
                        title = title.ifBlank { "New thread" },
                        modelSelection = modelSelection,
                        runtimeMode = runtimeMode,
                        interactionMode = ProviderInteractionMode.default,
                        branch = null,
                        worktreePath = null,
                        createdAt = now,
                    ),
                )
                if (initialMessage.isNotBlank()) {
                    container.threadRepository.startTurn(
                        ThreadTurnStartCommand(
                            commandId = commandId,
                            threadId = threadId,
                            message = TurnMessage(
                                messageId = messageId,
                                text = initialMessage,
                            ),
                            runtimeMode = runtimeMode,
                            createdAt = now,
                        ),
                    )
                }
                _extras.update { it.copy(showNewThreadSheet = false, isCreating = false) }
                onCreated(threadId)
            } catch (e: Exception) {
                _extras.update { it.copy(isCreating = false) }
            }
        }
    }
}

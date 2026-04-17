package com.t3tools.android.ui.screens.projects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t3tools.android.T3Application
import com.t3tools.android.data.rpc.WsConnectionState
import com.t3tools.android.domain.ShellState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProjectUiModel(
    val id: String,
    val title: String,
    val workspaceRoot: String,
    val threadCount: Int,
    val activeThreadCount: Int,
)

data class ProjectListUiState(
    val projects: List<ProjectUiModel> = emptyList(),
    val connectionState: WsConnectionState = WsConnectionState.Idle,
    val isBootstrapped: Boolean = false,
)

class ProjectListViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as T3Application).container

    val uiState: StateFlow<ProjectListUiState> = combine(
        container.serverRepository.shellState,
        container.connectionManager.connectionState,
    ) { shell, connState ->
        val projects = shell.projects.map { project ->
            val threads = shell.threads.filter { it.projectId == project.id }
            val activeCount = threads.count {
                it.session?.status == com.t3tools.android.data.model.OrchestrationSessionStatus.running ||
                    it.session?.status == com.t3tools.android.data.model.OrchestrationSessionStatus.starting
            }
            ProjectUiModel(
                id = project.id,
                title = project.title,
                workspaceRoot = project.workspaceRoot,
                threadCount = threads.size,
                activeThreadCount = activeCount,
            )
        }
        ProjectListUiState(
            projects = projects,
            connectionState = connState,
            isBootstrapped = shell.isBootstrapped,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectListUiState())
}

package com.t3tools.android.domain

import com.t3tools.android.data.model.OrchestrationProjectShell
import com.t3tools.android.data.model.OrchestrationShellStreamItem
import com.t3tools.android.data.model.OrchestrationThreadShell
import com.t3tools.android.data.rpc.WsRpcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlin.math.min
import kotlin.math.pow

data class ShellState(
    val projects: List<OrchestrationProjectShell> = emptyList(),
    val threads: List<OrchestrationThreadShell> = emptyList(),
    val isBootstrapped: Boolean = false,
)

class ServerRepository(private val rpcClient: WsRpcClient) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _shellState = MutableStateFlow(ShellState())
    val shellState: StateFlow<ShellState> = _shellState.asStateFlow()

    fun start() {
        scope.launch {
            subscribeShell()
                .retryWhen { _, attempt ->
                    delay(min(1000L * 2.0.pow(attempt.toInt()).toLong(), 64_000L))
                    true
                }
                .collect { item ->
                    _shellState.update { current -> applyShellItem(current, item) }
                }
        }
    }

    private fun subscribeShell(): Flow<OrchestrationShellStreamItem> =
        rpcClient.subscribe(
            method = "orchestration.subscribeShell",
            payload = buildJsonObject {},
            deserializer = OrchestrationShellStreamItem.serializer(),
        )

    private fun applyShellItem(current: ShellState, item: OrchestrationShellStreamItem): ShellState =
        when (item) {
            is OrchestrationShellStreamItem.Snapshot -> ShellState(
                projects = item.snapshot.projects,
                threads = item.snapshot.threads,
                isBootstrapped = true,
            )
            is OrchestrationShellStreamItem.ProjectUpserted -> current.copy(
                projects = current.projects
                    .filter { it.id != item.project.id } + item.project,
            )
            is OrchestrationShellStreamItem.ProjectRemoved -> current.copy(
                projects = current.projects.filter { it.id != item.projectId },
            )
            is OrchestrationShellStreamItem.ThreadUpserted -> current.copy(
                threads = current.threads
                    .filter { it.id != item.thread.id } + item.thread,
            )
            is OrchestrationShellStreamItem.ThreadRemoved -> current.copy(
                threads = current.threads.filter { it.id != item.threadId },
            )
        }
}

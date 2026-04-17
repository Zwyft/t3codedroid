package com.t3tools.android.ui.screens.threads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.t3tools.android.data.model.ModelSelection
import com.t3tools.android.data.model.RuntimeMode
import com.t3tools.android.ui.components.ThreadStatusIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadListScreen(
    projectId: String,
    onThreadClick: (String) -> Unit,
    onBack: () -> Unit,
    vm: ThreadListViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.projectTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::showNewThreadSheet) {
                Icon(Icons.Default.Add, contentDescription = "New thread")
            }
        },
    ) { padding ->
        if (uiState.threads.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No threads yet", style = MaterialTheme.typography.titleMedium)
                Text("Tap + to start a new conversation", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.threads, key = { it.id }) { thread ->
                    ThreadRow(thread = thread, onClick = { onThreadClick(thread.id) })
                }
            }
        }
    }

    if (uiState.showNewThreadSheet) {
        ModalBottomSheet(
            onDismissRequest = vm::hideNewThreadSheet,
            sheetState = sheetState,
        ) {
            NewThreadSheetContent(
                availableModels = uiState.availableModels,
                isCreating = uiState.isCreating,
                onCreate = { title, model, runtime, message ->
                    vm.createThread(title, model, runtime, message, onCreated = onThreadClick)
                },
                onDismiss = vm::hideNewThreadSheet,
            )
        }
    }
}

@Composable
private fun ThreadRow(thread: ThreadSummaryUiModel, onClick: () -> Unit) {
    val sessionStatus = if (thread.isRunning)
        com.t3tools.android.data.model.OrchestrationSessionStatus.running
    else null

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThreadStatusIcon(status = sessionStatus)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    thread.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (thread.branch != null) {
                        Text(
                            "⎇ ${thread.branch}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        thread.modelLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (thread.hasPendingApprovals) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Pending approval",
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewThreadSheetContent(
    availableModels: List<ModelSelection>,
    isCreating: Boolean,
    onCreate: (String, ModelSelection, RuntimeMode, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(availableModels.firstOrNull() ?: ModelSelection("claudeAgent", "claude-sonnet-4-6")) }
    var selectedRuntime by remember { mutableStateOf(RuntimeMode.full_access) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("New Thread", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Initial message (optional)") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 4,
        )

        // Model picker
        ExposedDropdownMenuBox(
            expanded = modelDropdownExpanded,
            onExpandedChange = { modelDropdownExpanded = it },
        ) {
            OutlinedTextField(
                value = "${selectedModel.provider}/${selectedModel.model}",
                onValueChange = {},
                readOnly = true,
                label = { Text("Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelDropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = modelDropdownExpanded,
                onDismissRequest = { modelDropdownExpanded = false },
            ) {
                availableModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text("${model.provider}/${model.model}") },
                        onClick = {
                            selectedModel = model
                            modelDropdownExpanded = false
                        },
                    )
                }
            }
        }

        // Runtime mode chips
        Text("Mode", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuntimeMode.entries.forEach { mode ->
                val label = when (mode) {
                    RuntimeMode.full_access -> "Full Access"
                    RuntimeMode.auto_accept_edits -> "Auto Accept"
                    RuntimeMode.approval_required -> "Approval Required"
                }
                val selected = selectedRuntime == mode
                androidx.compose.material3.FilterChip(
                    selected = selected,
                    onClick = { selectedRuntime = mode },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { onCreate(title, selectedModel, selectedRuntime, message) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating,
        ) {
            if (isCreating) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            }
            Text("Create Thread")
        }
    }
}

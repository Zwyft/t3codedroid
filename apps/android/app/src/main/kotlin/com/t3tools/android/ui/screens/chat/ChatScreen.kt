package com.t3tools.android.ui.screens.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.t3tools.android.data.model.ProviderApprovalDecision
import com.t3tools.android.ui.screens.git.GitStatusSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    threadId: String,
    onBack: () -> Unit,
    vm: ChatViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showGitSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(uiState.error ?: "Error")
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            uiState.threadTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (uiState.branch != null) {
                            Text(
                                "⎇ ${uiState.branch}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.workspaceRoot != null) {
                        IconButton(onClick = { showGitSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = "Git status",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            // Messages take up remaining space
            MessagesTimeline(
                messages = uiState.messages,
                activities = uiState.activities,
                isRunning = uiState.isRunning,
                listState = listState,
                modifier = Modifier.weight(1f),
            )

            // Approval panel sits above composer when needed
            if (uiState.pendingApprovals.isNotEmpty()) {
                ApprovalPanel(
                    approvals = uiState.pendingApprovals,
                    onDecision = { requestId, decision ->
                        vm.respondToApproval(requestId, decision)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Composer
            Surface(shadowElevation = 4.dp) {
                ComposerBar(
                    text = uiState.composerText,
                    onTextChange = vm::onComposerTextChange,
                    runtimeMode = uiState.runtimeMode,
                    isRunning = uiState.isRunning,
                    onSend = vm::sendMessage,
                    onInterrupt = vm::interruptTurn,
                )
            }
        }
    }

    if (showGitSheet && uiState.workspaceRoot != null) {
        GitStatusSheet(
            workspaceRoot = uiState.workspaceRoot!!,
            onDismiss = { showGitSheet = false },
        )
    }
}

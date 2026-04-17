package com.t3tools.android.ui.screens.git

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t3tools.android.T3Application
import com.t3tools.android.data.model.GitBranch
import com.t3tools.android.data.model.GitChangedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GitStatusUiState(
    val currentBranch: String? = null,
    val branches: List<GitBranch> = emptyList(),
    val changedFiles: List<GitChangedFile> = emptyList(),
    val aheadCount: Int = 0,
    val behindCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class GitStatusViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as T3Application).container
    private val _uiState = MutableStateFlow(GitStatusUiState())
    val uiState: StateFlow<GitStatusUiState> = _uiState.asStateFlow()

    fun load(cwd: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val status = container.gitRepository.refreshStatus(cwd)
                val branches = runCatching { container.gitRepository.listBranches(cwd) }
                    .getOrNull()
                _uiState.update {
                    GitStatusUiState(
                        currentBranch = status.branch,
                        branches = branches?.branches ?: emptyList(),
                        changedFiles = status.workingTree.files,
                        aheadCount = status.aheadCount,
                        behindCount = status.behindCount,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

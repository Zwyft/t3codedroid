package com.t3tools.android.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GitBranch(
    val name: String,
    val current: Boolean = false,
    val isDefault: Boolean = false,
    val worktreePath: String? = null,
)

@Serializable
data class GitListBranchesResult(
    val branches: List<GitBranch>,
)

@Serializable
data class GitChangedFile(
    val path: String,
    val insertions: Int = 0,
    val deletions: Int = 0,
)

@Serializable
data class GitWorkingTree(
    val files: List<GitChangedFile> = emptyList(),
    val insertions: Int = 0,
    val deletions: Int = 0,
)

@Serializable
data class GitStatusResult(
    val isRepo: Boolean,
    val branch: String? = null,
    val hasWorkingTreeChanges: Boolean = false,
    val workingTree: GitWorkingTree = GitWorkingTree(),
    val hasUpstream: Boolean = false,
    val aheadCount: Int = 0,
    val behindCount: Int = 0,
)

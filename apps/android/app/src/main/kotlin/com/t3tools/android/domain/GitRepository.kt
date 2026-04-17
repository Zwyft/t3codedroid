package com.t3tools.android.domain

import com.t3tools.android.data.model.GitListBranchesResult
import com.t3tools.android.data.model.GitStatusResult
import com.t3tools.android.data.rpc.WsRpcClient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class GitRepository(private val rpcClient: WsRpcClient) {

    suspend fun listBranches(cwd: String): GitListBranchesResult =
        rpcClient.request(
            method = "git.listBranches",
            payload = buildJsonObject { put("cwd", cwd) },
            deserializer = GitListBranchesResult.serializer(),
        )

    suspend fun refreshStatus(cwd: String): GitStatusResult =
        rpcClient.request(
            method = "git.refreshStatus",
            payload = buildJsonObject { put("cwd", cwd) },
            deserializer = GitStatusResult.serializer(),
        )
}

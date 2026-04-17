package com.t3tools.android

import android.app.Application
import com.t3tools.android.data.prefs.AppPreferences
import com.t3tools.android.data.rpc.WsConnectionManager
import com.t3tools.android.data.rpc.WsRpcClient
import com.t3tools.android.domain.GitRepository
import com.t3tools.android.domain.ServerRepository
import com.t3tools.android.domain.ThreadRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(app: Application) {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)   // no read timeout for WebSocket streams
        .build()

    val appPreferences = AppPreferences(app)
    val connectionManager = WsConnectionManager(okHttpClient)
    val rpcClient = WsRpcClient(connectionManager)
    val serverRepository = ServerRepository(rpcClient)
    val threadRepository = ThreadRepository(rpcClient)
    val gitRepository = GitRepository(rpcClient)
}

class T3Application : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

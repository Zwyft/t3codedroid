package com.t3tools.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.t3tools.android.data.rpc.WsConnectionState
import com.t3tools.android.ui.navigation.AppNavigation
import com.t3tools.android.ui.navigation.Route
import com.t3tools.android.ui.theme.T3CodeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as T3Application).container

        // Start shell subscription (handles its own reconnect internally)
        container.serverRepository.start()

        // Determine start destination: if already connected, go to projects
        var startDest = Route.Connect.path

        setContent {
            T3CodeTheme {
                AppNavigation(startDestination = startDest)
            }
        }

        // Auto-reconnect on last known server
        lifecycleScope.launch {
            val lastUrl = container.appPreferences.lastServerUrl.first()
            if (lastUrl != null) {
                container.connectionManager.connect(lastUrl)
                startDest = Route.Projects.path
            }
        }
    }
}

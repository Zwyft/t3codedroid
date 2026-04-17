package com.t3tools.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.t3tools.android.ui.screens.chat.ChatScreen
import com.t3tools.android.ui.screens.connect.ConnectScreen
import com.t3tools.android.ui.screens.projects.ProjectListScreen
import com.t3tools.android.ui.screens.settings.SettingsScreen
import com.t3tools.android.ui.screens.threads.ThreadListScreen

sealed class Route(val path: String) {
    object Connect : Route("connect")
    object Projects : Route("projects")
    object Settings : Route("settings")

    object ThreadList : Route("projects/{projectId}/threads") {
        fun forProject(projectId: String) = "projects/$projectId/threads"
        val projectIdArg = "projectId"
    }

    object Chat : Route("threads/{threadId}/chat") {
        fun forThread(threadId: String) = "threads/$threadId/chat"
        val threadIdArg = "threadId"
    }
}

@Composable
fun AppNavigation(startDestination: String = Route.Connect.path) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Route.Connect.path) {
            ConnectScreen(
                onConnected = { navController.navigate(Route.Projects.path) },
                onSettings = { navController.navigate(Route.Settings.path) },
            )
        }

        composable(Route.Projects.path) {
            ProjectListScreen(
                onProjectClick = { projectId ->
                    navController.navigate(Route.ThreadList.forProject(projectId))
                },
                onSettings = { navController.navigate(Route.Settings.path) },
            )
        }

        composable(
            route = Route.ThreadList.path,
            arguments = listOf(navArgument(Route.ThreadList.projectIdArg) { type = NavType.StringType }),
        ) { backStack ->
            val projectId = backStack.arguments?.getString(Route.ThreadList.projectIdArg) ?: return@composable
            ThreadListScreen(
                projectId = projectId,
                onThreadClick = { threadId -> navController.navigate(Route.Chat.forThread(threadId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Route.Chat.path,
            arguments = listOf(navArgument(Route.Chat.threadIdArg) { type = NavType.StringType }),
        ) { backStack ->
            val threadId = backStack.arguments?.getString(Route.Chat.threadIdArg) ?: return@composable
            ChatScreen(
                threadId = threadId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Route.Settings.path) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onDisconnect = {
                    navController.navigate(Route.Connect.path) {
                        popUpTo(Route.Connect.path) { inclusive = true }
                    }
                },
            )
        }
    }
}

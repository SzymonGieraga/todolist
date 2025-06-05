package com.example.todolist.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.todolist.ui.screens.AddEditTaskScreen
import com.example.todolist.ui.screens.SettingsScreen
import com.example.todolist.ui.screens.TaskListScreen
import com.example.todolist.viewmodel.TaskViewModel
import androidx.navigation.navDeepLink

sealed class Screen(val route: String) {
    object TaskList : Screen("taskList")
    object AddTask : Screen("addTask")
    object EditTask : Screen("editTask/{taskId}") {
        fun createRoute(taskId: Int): String {
            return "editTask/$taskId"
        }
    }
    object Settings : Screen("settings")

    companion object {
        const val DEEP_LINK_URI_BASE = "app://com.example.todolist"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    taskViewModel: TaskViewModel
) {


    NavHost(navController = navController, startDestination = Screen.TaskList.route) {
        composable(Screen.TaskList.route) {
            TaskListScreen(
                navController = navController,
                taskViewModel = taskViewModel
            )
        }
        composable(Screen.AddTask.route) {
            AddEditTaskScreen(
                navController = navController,
                taskViewModel = taskViewModel,
                taskId = null
            )
        }
        composable(
            route = Screen.EditTask.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.IntType
                    // nullable = false // Domyślnie dla IntType
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "${Screen.DEEP_LINK_URI_BASE}/task/{taskId}" }
            )
        ) { backStackEntry ->
            val taskIdFromArgs = backStackEntry.arguments?.getInt("taskId")
            if (taskIdFromArgs == null && backStackEntry.destination.route == Screen.EditTask.route) {
                android.util.Log.e("AppNavigation", "taskIdFromArgs is null for EditTask route!")
            }
            AddEditTaskScreen(
                navController = navController,
                taskViewModel = taskViewModel,
                taskId = taskIdFromArgs
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                taskViewModel = taskViewModel
            )
        }
    }
}

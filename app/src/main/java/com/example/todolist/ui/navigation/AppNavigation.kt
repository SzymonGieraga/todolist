package com.example.todolist.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.todolist.ui.screens.AddEditTaskScreen
import com.example.todolist.ui.screens.SettingsScreen
import com.example.todolist.ui.screens.TaskListScreen
import com.example.todolist.viewmodel.TaskViewModel

sealed class Screen(val route: String) {
    object TaskList : Screen("taskList")
    object AddTask : Screen("addTask")
    object EditTask : Screen("editTask/{taskId}") {
        fun createRoute(taskId: Int): String {
            return "editTask/$taskId"
        }
    }
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(taskViewModel: TaskViewModel) {
    val navController = rememberNavController()

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
                taskId = null // Dla nowego zadania taskId jest null
            )
        }
        composable(
            route = Screen.EditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.IntType })
        ) { backStackEntry ->
            val taskIdFromArgs = backStackEntry.arguments?.getInt("taskId")
            AddEditTaskScreen(
                navController = navController,
                taskViewModel = taskViewModel,
                taskId = taskIdFromArgs // Przekaż pobrane taskId
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

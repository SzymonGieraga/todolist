package com.example.todolist.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.todolist.notifications.NotificationHelper
import com.example.todolist.ui.navigation.AppNavigation
import com.example.todolist.ui.theme.TodolistTheme
import com.example.todolist.viewmodel.TaskViewModel
import com.example.todolist.viewmodel.TaskViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("MainActivity", "Notification permission GRANTED")
            } else {
                Log.w("MainActivity", "Notification permission DENIED")
            }
        }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.i("MainActivity", "Notification permission already granted.")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.i("MainActivity", "Showing rationale for notification permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    Log.i("MainActivity", "Requesting notification permission.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called. Intent action: ${intent?.action}, data: ${intent?.dataString}")

        NotificationHelper.createNotificationChannel(this)
        askNotificationPermission()

        val factory = TaskViewModelFactory(application)
        val taskViewModel: TaskViewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        setContent {
            TodolistTheme {
                navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(navController = navController, taskViewModel = taskViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called. Intent action: ${intent.action}, data: ${intent.dataString}")
        setIntent(intent)
        if (::navController.isInitialized) {
            val handled = navController.handleDeepLink(intent)
            Log.d("MainActivity", "Deep link handled by NavController in onNewIntent: $handled")
        } else {
            Log.w("MainActivity", "NavController not initialized in onNewIntent when trying to handle deep link.")
        }
    }

}

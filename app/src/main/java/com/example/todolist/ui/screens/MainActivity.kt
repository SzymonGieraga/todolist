package com.example.todolist.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.todolist.notifications.NotificationHelper
import com.example.todolist.ui.navigation.AppNavigation
import com.example.todolist.ui.theme.TodolistTheme
import com.example.todolist.viewmodel.TaskViewModel
import com.example.todolist.viewmodel.TaskViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Uprawnienie przyznane
            } else {
                // Uprawnienie odrzucone, obsłuż odpowiednio
                // Możesz np. wyświetlić Toast informujący użytkownika
            }
        }

    private fun askNotificationPermission() {
        // Dotyczy tylko Android 13 (API 33) i nowszych
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Uprawnienie już przyznane
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Tutaj możesz wyświetlić użytkownikowi wyjaśnienie, dlaczego potrzebujesz tego uprawnienia,
                // np. w Dialogu, a następnie wywołać requestPermissionLauncher.launch()
                // Dla uproszczenia, od razu prosimy o uprawnienie:
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Bezpośrednio poproś o uprawnienie
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Utwórz kanał powiadomień
        NotificationHelper.createNotificationChannel(this)
        // Poproś o uprawnienia do powiadomień (dla Android 13+)
        askNotificationPermission()

        // Inicjalizacja ViewModelu
        // Upewnij się, że TodoApplication jest poprawnie zdefiniowana i zarejestrowana w Manifeście
        // oraz że TaskViewModelFactory przyjmuje Application jako argument
        val factory = TaskViewModelFactory(application)
        val taskViewModel: TaskViewModel = ViewModelProvider(this, factory).get(TaskViewModel::class.java)

        setContent {
            TodolistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), // Usunięto .Companion
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(taskViewModel = taskViewModel)
                }
            }
        }
    }
}

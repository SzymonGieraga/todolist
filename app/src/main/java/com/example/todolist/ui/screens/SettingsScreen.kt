package com.example.todolist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todolist.viewmodel.TaskViewModel
import com.example.todolist.ui.components.AddCategoryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    taskViewModel: TaskViewModel
) {
    val appSettings by taskViewModel.appSettings.collectAsState()
    val allUserAndPredefinedCategories by taskViewModel.allAvailableCategories.collectAsState()

    var localNotificationOffset by remember(appSettings.notificationOffsetMinutes) {
        mutableStateOf(appSettings.notificationOffsetMinutes.toString())
    }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            onConfirm = { newCategoryName ->
                taskViewModel.addNewUserCategory(newCategoryName)
                showAddCategoryDialog = false
            },
            existingCategories = allUserAndPredefinedCategories
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Potwierdź usunięcie") },
            text = { Text("Czy na pewno chcesz usunąć kategorię \"$category\"? Zadania z tą kategorią nie zostaną usunięte, ale stracą przypisaną kategorię.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskViewModel.removeUserCategory(category)
                        categoryToDelete = null
                    }
                ) { Text("Usuń") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Anuluj") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Ogólne", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        taskViewModel.updateHideCompletedTasks(!appSettings.hideCompletedTasks)
                    }
                    .padding(vertical = 8.dp)
            ) {
                Text("Ukryj globalnie ukończone zadania", modifier = Modifier.weight(1f))
                Switch(
                    checked = appSettings.hideCompletedTasks,
                    onCheckedChange = { taskViewModel.updateHideCompletedTasks(it) }
                )
            }
            Text(
                "Indywidualnie ukryte zadania (ikoną oka) pozostaną ukryte niezależnie od tego ustawienia, chyba że użyjesz filtra 'Pokaż tylko ukryte'.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))
            Text("Powiadomienia", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Czas powiadomienia (minuty przed terminem):", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = localNotificationOffset,
                onValueChange = { newValue ->
                    val filteredValue = newValue.filter { it.isDigit() }
                    localNotificationOffset = if (filteredValue.length <= 3) filteredValue else filteredValue.take(3)
                    localNotificationOffset.toIntOrNull()?.let {
                        if (it >= 0) taskViewModel.updateNotificationOffset(it)
                    }
                },
                label = { Text("Minuty") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kategorie Zadań:", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { showAddCategoryDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Dodaj nową kategorię")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nowa")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (allUserAndPredefinedCategories.isEmpty()) {
                Text("Brak zdefiniowanych kategorii.")
            } else {
                val predefined = taskViewModel.settingsRepository.predefinedCategories.sorted()
                val userDefined = appSettings.userDefinedCategories.sorted()

                if (predefined.isNotEmpty()) {
                    Text("Predefiniowane:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    predefined.forEach { category ->
                        CategoryRow(categoryName = category, isPredefined = true) {}
                    }
                }

                if (userDefined.isNotEmpty()) {
                    Text("Twoje kategorie:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    userDefined.forEach { category ->
                        CategoryRow(categoryName = category, isPredefined = false) {
                            categoryToDelete = category
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    categoryName: String,
    isPredefined: Boolean,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = categoryName,
            fontStyle = if (isPredefined) FontStyle.Italic else FontStyle.Normal,
            modifier = Modifier.weight(1f)
        )
        if (!isPredefined) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Usuń kategorię \"$categoryName\"")
            }
        }
    }
}

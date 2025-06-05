package com.example.todolist.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.todolist.data.Task
import com.example.todolist.ui.components.FilterTasksDialog
import com.example.todolist.ui.navigation.Screen
import com.example.todolist.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    navController: NavController,
    taskViewModel: TaskViewModel
) {
    val tasks by taskViewModel.filteredAndSortedTasks.collectAsState()
    val searchQuery by taskViewModel.searchQuery.collectAsState()
    val allAvailableCategories by taskViewModel.allAvailableCategories.collectAsState()
    val activeCategoryFilters by taskViewModel.activeCategoryFilters.collectAsState()
    val showOnlyHiddenTasks by taskViewModel.showOnlyHiddenTasks.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    if (showFilterDialog) {
        FilterTasksDialog(
            allAvailableCategories = allAvailableCategories,
            initiallySelectedCategories = activeCategoryFilters,
            initiallyShowOnlyHidden = showOnlyHiddenTasks,
            onDismissRequest = { showFilterDialog = false },
            onApplyFilters = { selectedCategories, showHidden ->
                taskViewModel.updateActiveCategoryFilters(selectedCategories)
                taskViewModel.setShowOnlyHiddenTasks(showHidden)
                showFilterDialog = false
            }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Potwierdź usunięcie") },
            text = { Text("Czy na pewno chcesz usunąć zadanie \"${task.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskViewModel.deleteTask(task)
                        taskToDelete = null
                    }
                ) { Text("Usuń") }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("Anuluj") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista Zadań") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ustawienia")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddTask.route) }) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj zadanie")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { taskViewModel.setSearchQuery(it) },
                    label = { Text("Szukaj zadań...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { taskViewModel.setSearchQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Wyczyść wyszukiwanie")
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Filled.FilterList, contentDescription = "Filtruj")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (tasks.isEmpty()) {
                    item {
                        val message = when {
                            searchQuery.isNotBlank() -> "Brak zadań pasujących do wyszukiwania."
                            showOnlyHiddenTasks && activeCategoryFilters.isNotEmpty() -> "Brak ukrytych zadań w wybranych kategoriach."
                            showOnlyHiddenTasks -> "Brak ukrytych zadań."
                            activeCategoryFilters.isNotEmpty() -> "Brak zadań w wybranych kategoriach."
                            else -> "Brak zadań. Dodaj nowe!"
                        }
                        Text(
                            text = message,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp
                        )
                    }
                } else {
                    items(tasks, key = { task -> task.id }) { task ->
                        TaskItem(
                            task = task,
                            taskViewModel = taskViewModel,
                            showOnlyHiddenFilterActive = showOnlyHiddenTasks,
                            onTaskClick = {
                                navController.navigate(Screen.EditTask.createRoute(task.id))
                            },
                            onEditTask = {
                                navController.navigate(Screen.EditTask.createRoute(task.id))
                            },
                            onDeleteRequest = { taskToDelete = task }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    taskViewModel: TaskViewModel,
    showOnlyHiddenFilterActive: Boolean,
    onTaskClick: () -> Unit,
    onEditTask: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onTaskClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { taskViewModel.toggleTaskCompleted(task) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted && !showOnlyHiddenFilterActive && !task.isIndividuallyHidden) TextDecoration.LineThrough else TextDecoration.None
                )
                task.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    task.category?.let {
                        Text("[$it]", style = MaterialTheme.typography.labelSmall)
                    }
                    task.executionTime?.let {
                        Text(
                            "Termin: ${dateFormat.format(Date(it))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (task.attachments.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Attachment, contentDescription = "Załączniki", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("(${task.attachments.size})", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.width(4.dp))

            if (task.isCompleted) {
                IconButton(
                    onClick = { taskViewModel.toggleIndividualHide(task) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (task.isIndividuallyHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (task.isIndividuallyHidden) "Przywróć zadanie" else "Ukryj ukończone zadanie"
                    )
                }
            }

            IconButton(onClick = onDeleteRequest, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Usuń")
            }
        }
    }
}

package com.example.todolist.ui.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todolist.data.Task
import com.example.todolist.notifications.TaskAlarmScheduler
import com.example.todolist.viewmodel.TaskViewModel
import kotlinx.coroutines.flow.filterNotNull
import java.text.SimpleDateFormat
import java.util.*
import com.example.todolist.ui.components.AddCategoryDialog

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    taskId: Int?
) {
    val context = LocalContext.current
    val alarmScheduler = remember { TaskAlarmScheduler(context) }

    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var executionTime by remember { mutableStateOf<Long?>(null) }
    var notificationEnabled by remember { mutableStateOf(false) }
    var attachments by remember { mutableStateOf<List<String>>(emptyList()) }

    val isEditMode = taskId != null
    val allAvailableCategories by taskViewModel.allAvailableCategories.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Efekt do ładowania danych zadania w trybie edycji
    LaunchedEffect(key1 = taskId) {
        if (isEditMode && taskId != null) {
            taskViewModel.getTaskById(taskId).filterNotNull().collect { task ->
                taskToEdit = task
                title = task.title
                description = task.description ?: ""
                selectedCategory = task.category
                executionTime = task.executionTime
                notificationEnabled = task.notificationEnabled
                attachments = task.attachments
            }
        }
    }

    // Efekt do aktualizacji selectedCategory, jeśli kategoria zadania się zmieni
    // (np. po dodaniu nowej kategorii i automatycznym jej wybraniu)
    LaunchedEffect(taskToEdit?.category, allAvailableCategories) {
        if (isEditMode && taskToEdit != null) {
            selectedCategory = taskToEdit?.category
        } else if (!isEditMode && selectedCategory == null && allAvailableCategories.isNotEmpty() && !allAvailableCategories.contains(selectedCategory)) {
            // Opcjonalnie: ustaw pierwszą dostępną kategorię jako domyślną dla nowego zadania
            // selectedCategory = allAvailableCategories.firstOrNull { it != "Dodaj nową..." }
        }
    }


    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newAttachmentUris = uris.map { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            uri.toString()
        }
        attachments = attachments + newAttachmentUris
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            onConfirm = { newCategoryName ->
                taskViewModel.addNewUserCategory(newCategoryName)
                // Po dodaniu, nowa kategoria powinna pojawić się w allAvailableCategories
                // i można ją automatycznie wybrać.
                // Czekamy, aż ViewModel zaktualizuje listę i wybieramy.
                // To może wymagać lekkiego opóźnienia lub obserwacji zmiany w allAvailableCategories.
                // Na razie prościej:
                selectedCategory = newCategoryName
                showAddCategoryDialog = false
            },
            existingCategories = allAvailableCategories // Przekaż istniejące kategorie do walidacji
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edytuj Zadanie" else "Dodaj Zadanie") },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tytuł") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis (opcjonalnie)") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Wybór kategorii z Dropdown
            var categoryMenuExpanded by remember { mutableStateOf(false) }
            val specialOptionAddNew = "Dodaj nową..."

            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField( // Zmienione na OutlinedTextField dla spójności
                    value = selectedCategory ?: "Wybierz kategorię",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategoria") },
                    trailingIcon = {
                        Icon(
                            if (categoryMenuExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Rozwiń/Zwiń kategorie",
                            modifier = Modifier.clickable { categoryMenuExpanded = !categoryMenuExpanded }
                        )
                    },
                    modifier = Modifier
                        .menuAnchor() // Ważne dla ExposedDropdownMenuBox
                        .fillMaxWidth()
                        .clickable { categoryMenuExpanded = !categoryMenuExpanded } // Klikalność na całym polu
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth() // Aby menu miało szerokość pola
                ) {
                    allAvailableCategories.forEach { categoryOption ->
                        DropdownMenuItem(
                            text = { Text(categoryOption) },
                            onClick = {
                                selectedCategory = categoryOption
                                categoryMenuExpanded = false
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text(specialOptionAddNew, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            showAddCategoryDialog = true
                            categoryMenuExpanded = false
                        }
                    )
                }
            }


            Spacer(modifier = Modifier.height(16.dp))
            ExecutionDateTimePicker(
                executionTime = executionTime,
                onDateTimeSelected = { executionTime = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Powiadomienie:")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = notificationEnabled,
                    onCheckedChange = { notificationEnabled = it },
                    enabled = executionTime != null
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Załączniki:", style = MaterialTheme.typography.titleMedium)
            attachments.forEachIndexed { index, uriString ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Załącznik ${index + 1}: ${Uri.parse(uriString).lastPathSegment ?: uriString}", maxLines = 1)
                    IconButton(onClick = {
                        attachments = attachments.toMutableList().apply { removeAt(index) }
                    }) {
                        Icon(Icons.Filled.Clear, "Usuń załącznik")
                    }
                }
            }
            Button(onClick = { filePickerLauncher.launch("*/*") }) {
                Icon(Icons.Filled.Attachment, contentDescription = "Dodaj załącznik")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dodaj Załącznik")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val currentAppSettings = taskViewModel.appSettings.value
                        val currentTime = System.currentTimeMillis()
                        val task = if (isEditMode && taskToEdit != null) {
                            taskToEdit!!.copy(
                                title = title.trim(),
                                description = description.trim().ifBlank { null },
                                category = selectedCategory,
                                executionTime = executionTime,
                                notificationEnabled = notificationEnabled && executionTime != null,
                                attachments = attachments
                            )
                        } else {
                            Task(
                                title = title.trim(),
                                description = description.trim().ifBlank { null },
                                creationTime = currentTime,
                                executionTime = executionTime,
                                isCompleted = false,
                                notificationEnabled = notificationEnabled && executionTime != null,
                                category = selectedCategory,
                                attachments = attachments
                            )
                        }

                        if (isEditMode) {
                            taskViewModel.updateTask(task)
                        } else {
                            taskViewModel.insertTask(task)
                        }

                        if (isEditMode && taskToEdit != null) {
                            alarmScheduler.cancelNotification(taskToEdit!!)
                        }
                        if (task.notificationEnabled && task.executionTime != null && task.executionTime!! > System.currentTimeMillis()) {
                            alarmScheduler.scheduleNotification(task, currentAppSettings.notificationOffsetMinutes)
                        } else if (!task.notificationEnabled || task.isCompleted) {
                            alarmScheduler.cancelNotification(task)
                        }
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "Tytuł nie może być pusty!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditMode) "Zapisz Zmiany" else "Dodaj Zadanie")
            }
        }
    }
}

@Composable
fun ExecutionDateTimePicker(
    executionTime: Long?,
    onDateTimeSelected: (Long?) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    executionTime?.let { calendar.timeInMillis = it }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0) // Ustaw sekundy na 0 dla precyzji
            calendar.set(Calendar.MILLISECOND, 0) // Ustaw milisekundy na 0
            onDateTimeSelected(calendar.timeInMillis)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true // 24-godzinny format
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            timePickerDialog.show() // Pokaż dialog czasu po wybraniu daty
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000 // Ustaw minimalną datę na dzisiaj


    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Termin wykonania (opcjonalnie):")
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    executionTime?.let { dateFormat.format(Date(it)) } ?: "Wybierz datę i godzinę"
                )
            }
            if (executionTime != null) {
                IconButton(onClick = { onDateTimeSelected(null) }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Wyczyść termin")
                }
            }
        }
    }
}

package com.example.todolist.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.todolist.data.Task
import com.example.todolist.notifications.TaskAlarmScheduler
import com.example.todolist.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import com.example.todolist.ui.components.AddCategoryDialog

fun getFileName(context: Context, uri: Uri): String {
    var fileName: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("getFileName", "Error querying content resolver for display name: ${e.message}")
        }
    }
    if (fileName == null) {
        fileName = uri.path
        val cut = fileName?.lastIndexOf('/')
        if (cut != -1 && cut != null) {
            fileName = fileName?.substring(cut + 1)
        }
    }
    return fileName?.takeIf { it.isNotBlank() } ?: "nieznany_plik_${UUID.randomUUID()}"
}

fun generateUniqueInternalFileName(baseName: String, extension: String, directory: File): String {
    val sanitizedBaseName = baseName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val sanitizedExtension = extension.takeIf { it.isNotBlank() }?.let { "." + it.replace(Regex("[^a-zA-Z0-9]"), "") } ?: ""
    var finalName = "$sanitizedBaseName$sanitizedExtension"
    if (finalName.isBlank() || finalName == ".") {
        finalName = "plik_${UUID.randomUUID()}$sanitizedExtension"
    }
    var counter = 1
    var tempFile = File(directory, finalName)
    while (tempFile.exists()) {
        finalName = "$sanitizedBaseName ($counter)$sanitizedExtension"
        if (finalName == "$sanitizedBaseName ($counter)$sanitizedExtension" && counter > 1 && baseName.endsWith(" ($counter-1)")) {
            finalName = "$sanitizedBaseName$counter$sanitizedExtension"
        }
        tempFile = File(directory, finalName)
        counter++
        if (counter > 1000) {
            Log.e("generateUniqueInternalFileName", "Could not generate a unique name after 1000 tries for $baseName")
            return "plik_awaryjny_${UUID.randomUUID()}$sanitizedExtension"
        }
    }
    return finalName
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    taskId: Int?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val alarmScheduler = remember { TaskAlarmScheduler(context) }

    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var executionTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var notificationEnabledState by rememberSaveable { mutableStateOf(false) }
    var attachments by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    val isEditMode = taskId != null
    val allAvailableCategories by taskViewModel.allAvailableCategories.collectAsState()
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var hasNotificationPermission by rememberSaveable {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else { true }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(key1 = taskId) {
        Log.d("AddEditTaskScreen", "LaunchedEffect for taskId ($taskId) triggered. isEditMode: $isEditMode")
        if (taskId != null) {
            Log.i("AddEditTaskScreen", "EDIT MODE - Subscribing to task with ID: $taskId")
            taskViewModel.getTaskById(taskId).collectLatest { loadedTask ->
                if (loadedTask != null) {
                    Log.i("AddEditTaskScreen", "EDIT MODE - Received task data: ${loadedTask.title}, attachments: ${loadedTask.attachments.size}")
                    taskToEdit = loadedTask
                    title = loadedTask.title
                    description = loadedTask.description ?: ""
                    selectedCategory = loadedTask.category
                    executionTime = loadedTask.executionTime
                    notificationEnabledState = loadedTask.notificationEnabled
                    attachments = loadedTask.attachments
                } else {
                    Log.w("AddEditTaskScreen", "EDIT MODE - Task with ID $taskId not found or is null. Clearing fields.")
                    taskToEdit = null
                    title = ""
                    description = ""
                    selectedCategory = null
                    executionTime = null
                    notificationEnabledState = false
                    attachments = emptyList()
                }
            }
        } else {
            Log.i("AddEditTaskScreen", "ADD MODE - Initializing for new task.")
            if (taskToEdit == null && title.isEmpty() && description.isEmpty() && attachments.isEmpty() && selectedCategory == null && executionTime == null && !notificationEnabledState) {
                Log.d("AddEditTaskScreen", "ADD MODE - Fields are empty, ensuring default empty values.")
            } else {
                Log.d("AddEditTaskScreen", "ADD MODE - Fields might have been restored by rememberSaveable or are being edited. Title: '$title'")
            }
        }
    }


    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        coroutineScope.launch {
            val newAttachmentNames = mutableListOf<String>()
            uris.forEach { uri ->
                try {
                    val attachmentsDir = File(context.filesDir, "attachments")
                    if (!attachmentsDir.exists()) { attachmentsDir.mkdirs() }

                    val originalFileNameWithExt = getFileName(context, uri)
                    val baseName = originalFileNameWithExt.substringBeforeLast('.', originalFileNameWithExt)
                    val extension = originalFileNameWithExt.substringAfterLast('.', "")
                    val internalFileName = generateUniqueInternalFileName(baseName, extension, attachmentsDir)
                    val internalFile = File(attachmentsDir, internalFileName)

                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            FileOutputStream(internalFile).use { outputStream -> inputStream.copyTo(outputStream) }
                        }
                    }
                    newAttachmentNames.add(internalFileName)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Błąd podczas kopiowania pliku: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
            attachments = attachments + newAttachmentNames
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            onConfirm = { newCategoryName ->
                taskViewModel.addNewUserCategory(newCategoryName)
                selectedCategory = newCategoryName
                showAddCategoryDialog = false
            },
            existingCategories = allAvailableCategories
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
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tytuł") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Opis (opcjonalnie)") }, modifier = Modifier.fillMaxWidth().height(120.dp))
            Spacer(modifier = Modifier.height(8.dp))

            var categoryMenuExpanded by rememberSaveable { mutableStateOf(false) }
            val specialOptionAddNew = "Dodaj nową..."
            ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
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
                        .menuAnchor()
                        .fillMaxWidth()
                        .clickable { categoryMenuExpanded = !categoryMenuExpanded }
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
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
            ExecutionDateTimePicker(executionTime = executionTime, onDateTimeSelected = { executionTime = it })
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Powiadomienie:")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = notificationEnabledState,
                    onCheckedChange = { notificationEnabledState = it },
                    enabled = executionTime != null && hasNotificationPermission
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission && executionTime != null) {
                    TextButton(onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Włącz powiadomienia")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Załączniki:", style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.fillMaxWidth()) {
                attachments.forEachIndexed { index, internalFileName ->
                    val internalFile = File(File(context.filesDir, "attachments"), internalFileName)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (internalFile.exists()) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", internalFile)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, context.contentResolver.getType(uri))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: ActivityNotFoundException) {
                                        Toast.makeText(context, "Nie znaleziono aplikacji do otwarcia tego pliku.", Toast.LENGTH_SHORT).show()
                                    } catch (e: SecurityException) {
                                        Toast.makeText(context, "Brak uprawnień do otwarcia pliku (FileProvider).", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Plik załącznika nie istnieje: $internalFileName", Toast.LENGTH_LONG).show()
                                    Log.e("AddEditTaskScreen", "Attachment file does not exist: ${internalFile.absolutePath}")
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Załącznik ${index + 1}: $internalFileName", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                if (internalFile.exists()) {
                                    val deleted = internalFile.delete()
                                    Log.d("AddEditTaskScreen", "Attempted to delete $internalFileName, success: $deleted")
                                } else {
                                    Log.w("AddEditTaskScreen", "Attempted to delete non-existent file: $internalFileName")
                                }
                            }
                            val currentAttachments = attachments.toMutableList()
                            currentAttachments.removeAt(index)
                            attachments = currentAttachments.toList()
                        }) {
                            Icon(Icons.Filled.Clear, "Usuń załącznik")
                        }
                    }
                }
            }

            Button(onClick = { filePickerLauncher.launch("*/*") }) {
                Icon(Icons.Filled.Attachment, contentDescription = "Dodaj załącznik"); Spacer(modifier = Modifier.width(8.dp)); Text("Dodaj Załącznik")
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        coroutineScope.launch {
                            val currentAppSettings = taskViewModel.appSettings.value
                            val currentTime = System.currentTimeMillis()
                            val actualNotificationEnabled = notificationEnabledState && hasNotificationPermission

                            val taskToSave = if (isEditMode && taskToEdit != null) {
                                taskToEdit!!.copy(
                                    title = title.trim(), description = description.trim().ifBlank { null }, category = selectedCategory,
                                    executionTime = executionTime, notificationEnabled = actualNotificationEnabled, attachments = attachments
                                )
                            } else {
                                Task(
                                    title = title.trim(), description = description.trim().ifBlank { null }, creationTime = currentTime,
                                    executionTime = executionTime, isCompleted = false, notificationEnabled = actualNotificationEnabled,
                                    category = selectedCategory, attachments = attachments
                                )
                            }

                            var finalTaskForNotification = taskToSave

                            if (isEditMode) {
                                taskViewModel.updateTask(taskToSave)
                                if (taskToEdit != null) {
                                    alarmScheduler.cancelNotification(taskToEdit!!)
                                }
                            } else {
                                val newTaskId = taskViewModel.insertTask(taskToSave)
                                finalTaskForNotification = taskToSave.copy(id = newTaskId.toInt()) // Poprawiono
                                Log.d("AddEditTaskScreen", "New task inserted with ID: $newTaskId. Task for notification: $finalTaskForNotification")
                            }

                            if (finalTaskForNotification.notificationEnabled &&
                                finalTaskForNotification.executionTime != null &&
                                finalTaskForNotification.executionTime!! > System.currentTimeMillis() &&
                                finalTaskForNotification.id != 0) {
                                alarmScheduler.scheduleNotification(finalTaskForNotification, currentAppSettings.notificationOffsetMinutes)
                                Log.d("AddEditTaskScreen", "Scheduled notification for task ID: ${finalTaskForNotification.id}")
                            } else if (!finalTaskForNotification.notificationEnabled || finalTaskForNotification.isCompleted) {
                                alarmScheduler.cancelNotification(finalTaskForNotification) // Poprawiono
                                Log.d("AddEditTaskScreen", "Cancelled notification for task ID: ${finalTaskForNotification.id}")
                            }
                            withContext(Dispatchers.Main) {
                                navController.popBackStack()
                            }
                        }
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
fun ExecutionDateTimePicker(executionTime: Long?, onDateTimeSelected: (Long?) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    executionTime?.let { calendar.timeInMillis = it }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val timePickerDialog = TimePickerDialog(context, { _, hourOfDay, minute ->
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay); calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        onDateTimeSelected(calendar.timeInMillis)
    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
    val datePickerDialog = DatePickerDialog(context, { _, year, month, dayOfMonth ->
        calendar.set(Calendar.YEAR, year); calendar.set(Calendar.MONTH, month); calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        timePickerDialog.show()
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Termin wykonania (opcjonalnie):"); Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.weight(1f)) {
                Text(executionTime?.let { dateFormat.format(Date(it)) } ?: "Wybierz datę i godzinę")
            }
            if (executionTime != null) {
                IconButton(onClick = { onDateTimeSelected(null) }) { Icon(Icons.Filled.Clear, contentDescription = "Wyczyść termin") }
            }
        }
    }
}

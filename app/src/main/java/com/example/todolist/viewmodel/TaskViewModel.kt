package com.example.todolist.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.example.todolist.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao: TaskDao = AppDatabase.getDatabase(application).taskDao()
    val settingsRepository: SettingsRepository = SettingsRepository(application)
    private val appContext = application.applicationContext

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeCategoryFilters = MutableStateFlow<Set<String>>(emptySet())
    val activeCategoryFilters: StateFlow<Set<String>> = _activeCategoryFilters.asStateFlow()

    private val _showOnlyHiddenTasks = MutableStateFlow(false)
    val showOnlyHiddenTasks: StateFlow<Boolean> = _showOnlyHiddenTasks.asStateFlow()

    val appSettings: StateFlow<AppSettings> = settingsRepository.appSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Dodano L dla Long
            initialValue = AppSettings(false, 15, emptySet(), emptySet(), false)
        )

    init {
        viewModelScope.launch {
            appSettings.collectLatest { settings ->
                _activeCategoryFilters.value = settings.persistedActiveCategoryFilters
                _showOnlyHiddenTasks.value = settings.persistedShowOnlyHiddenTasks
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateActiveCategoryFilters(selectedCategories: Set<String>) {
        _activeCategoryFilters.value = selectedCategories
        viewModelScope.launch {
            settingsRepository.updatePersistedActiveCategoryFilters(selectedCategories)
        }
    }

    fun setShowOnlyHiddenTasks(showHidden: Boolean) {
        _showOnlyHiddenTasks.value = showHidden
        viewModelScope.launch {
            settingsRepository.updatePersistedShowOnlyHiddenTasks(showHidden)
        }
    }

    val allAvailableCategories: StateFlow<List<String>> = appSettings
        .map { settings ->
            (settingsRepository.predefinedCategories + settings.userDefinedCategories)
                .distinctBy { it.lowercase() }
                .sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = settingsRepository.predefinedCategories.sorted()
        )

    val filteredAndSortedTasks: StateFlow<List<Task>> =
        combine(
            taskDao.getAllTasks(),
            appSettings,
            _activeCategoryFilters,
            _showOnlyHiddenTasks,
            _searchQuery
        ) { tasks, settings, categoryFilters, uiShowOnlyHidden, query ->
            tasks
                .filter { task ->
                    if (uiShowOnlyHidden) {
                        if (!task.isIndividuallyHidden) return@filter false
                        if (categoryFilters.isNotEmpty()) {
                            val taskCategory = task.category ?: "Bez kategorii"
                            if (!categoryFilters.any { it.equals(taskCategory, ignoreCase = true) }) return@filter false
                        }
                    } else {
                        if (task.isIndividuallyHidden) return@filter false
                        if (settings.hideCompletedTasks && task.isCompleted) return@filter false


                        if (categoryFilters.isNotEmpty()) {
                            val taskCategory = task.category ?: "Bez kategorii"
                            if (!categoryFilters.any { it.equals(taskCategory, ignoreCase = true) }) return@filter false
                        }
                    }
                    // Filtr wyszukiwania
                    query.isBlank() ||
                            task.title.contains(query, ignoreCase = true) ||
                            (task.description?.contains(query, ignoreCase = true) == true)
                }
                .sortedWith(
                    compareBy(nullsLast()) { it.executionTime }
                )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun getTaskById(taskId: Int): StateFlow<Task?> {
        return taskDao.getTaskById(taskId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = null
            )
    }

    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch { taskDao.updateTask(task) }

    fun deleteTask(task: Task) = viewModelScope.launch {
        deleteAttachmentsForTask(task.attachments, appContext)
        taskDao.deleteTask(task)
    }

    private suspend fun deleteAttachmentsForTask(attachmentFileNames: List<String>, context: Context) {
        if (attachmentFileNames.isEmpty()) return
        withContext(Dispatchers.IO) {
            val attachmentsDir = File(context.filesDir, "attachments")
            if (attachmentsDir.exists() && attachmentsDir.isDirectory) {
                attachmentFileNames.forEach { fileName ->
                    try {
                        val fileToDelete = File(attachmentsDir, fileName)
                        if (fileToDelete.exists() && fileToDelete.isFile) {
                            fileToDelete.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun toggleTaskCompleted(task: Task) = viewModelScope.launch {
        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        if (!updatedTask.isCompleted) {
            updatedTask.isIndividuallyHidden = false
        }
        taskDao.updateTask(updatedTask)
    }

    fun toggleIndividualHide(task: Task) = viewModelScope.launch {
        if (task.isCompleted) {
            val updatedTask = task.copy(isIndividuallyHidden = !task.isIndividuallyHidden)
            taskDao.updateTask(updatedTask)
        }
    }

    fun updateNotificationOffset(minutes: Int) = viewModelScope.launch {
        settingsRepository.updateNotificationOffset(minutes)
    }

    fun addNewUserCategory(category: String) = viewModelScope.launch {
        if (category.isNotBlank() &&
            !settingsRepository.predefinedCategories.any { it.equals(category, ignoreCase = true) }) {
            settingsRepository.addUserCategory(category.trim())
        }
    }

    fun removeUserCategory(category: String) = viewModelScope.launch {
        if (!settingsRepository.predefinedCategories.any { it.equals(category, ignoreCase = true) }) {
            settingsRepository.removeUserCategory(category)
        }
    }

    fun updateHideCompletedTasks(hide: Boolean) = viewModelScope.launch {
        settingsRepository.updateHideCompletedTasks(hide)
    }
}

class TaskViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.todolist.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.todolist.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
// kotlinx.coroutines.flow.combine jest już importowane przez kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao: TaskDao = AppDatabase.getDatabase(application).taskDao()
    val settingsRepository: SettingsRepository = SettingsRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeListFilter = MutableStateFlow<String?>(null)
    val activeListFilter: StateFlow<String?> = _activeListFilter.asStateFlow()

    val specialFilterUkryte = "Ukryte"

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setActiveListFilter(filter: String?) {
        _activeListFilter.value = filter
    }

    val appSettings: StateFlow<AppSettings> = settingsRepository.appSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings(emptySet(), 15, emptySet())
        )

    val allAvailableCategories: StateFlow<List<String>> = appSettings
        .map { settings ->
            (settingsRepository.predefinedCategories + settings.userDefinedCategories)
                .distinctBy { it.lowercase() }
                .sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.predefinedCategories.sorted()
        )

    val filteredAndSortedTasks: StateFlow<List<Task>> =
        combine(
            taskDao.getAllTasks(),
            _activeListFilter,
            _searchQuery
        ) { tasks, activeFilter, query ->
            tasks
                .filter { task ->
                    if (activeFilter == specialFilterUkryte) {
                        if (!task.isIndividuallyHidden) return@filter false
                    } else {
                        if (task.isIndividuallyHidden) return@filter false
                        if (activeFilter != null) {
                            val taskCategory = task.category ?: "Bez kategorii"
                            if (!taskCategory.equals(activeFilter, ignoreCase = true)) return@filter false
                        }
                    }
                    val filterSearch = query.isBlank() ||
                            task.title.contains(query, ignoreCase = true) ||
                            (task.description?.contains(query, ignoreCase = true) == true)
                    filterSearch
                }
                .sortedWith(
                    compareBy(nullsLast()) { it.executionTime }
                )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed( 5000),
            initialValue = emptyList()
        )

    val categoriesForTaskListFilter: StateFlow<List<String>> =
        combine(taskDao.getAllTasks(), appSettings) { tasks, settings ->
            val categoriesFromTasks = tasks.mapNotNull { it.category }.distinct()
            (settingsRepository.predefinedCategories + settings.userDefinedCategories + categoriesFromTasks)
                .distinctBy { it.lowercase() }
                .sorted()
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = settingsRepository.predefinedCategories.sorted()
            )

    fun getTaskById(taskId: Int): StateFlow<Task?> {
        return taskDao.getTaskById(taskId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    fun insertTask(task: Task) = viewModelScope.launch {
        taskDao.insertTask(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        taskDao.updateTask(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        taskDao.deleteTask(task)
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
}

// --- DODANA KLASA FABRYKI ---
class TaskViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

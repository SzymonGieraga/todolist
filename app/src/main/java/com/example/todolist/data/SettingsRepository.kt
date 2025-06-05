package com.example.todolist.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsRepository(context: Context) {

    private val dataStore = context.dataStore
    val predefinedCategories: List<String> = listOf("Praca", "Dom", "Osobiste")

    object PreferencesKeys {
        val HIDE_COMPLETED_TASKS = booleanPreferencesKey("hide_completed_tasks")
        val NOTIFICATION_OFFSET_MINUTES = intPreferencesKey("notification_offset_minutes")
        val USER_DEFINED_CATEGORIES = stringSetPreferencesKey("user_defined_categories")
        val PERSISTED_ACTIVE_CATEGORY_FILTERS = stringSetPreferencesKey("persisted_active_category_filters")
        val PERSISTED_SHOW_ONLY_HIDDEN_TASKS = booleanPreferencesKey("persisted_show_only_hidden_tasks")
    }

    val appSettingsFlow: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val hideCompleted = preferences[PreferencesKeys.HIDE_COMPLETED_TASKS] ?: false
            val notificationOffset = preferences[PreferencesKeys.NOTIFICATION_OFFSET_MINUTES] ?: 15
            val userDefinedCategories = preferences[PreferencesKeys.USER_DEFINED_CATEGORIES] ?: emptySet()
            val persistedActiveCategoryFilters = preferences[PreferencesKeys.PERSISTED_ACTIVE_CATEGORY_FILTERS] ?: emptySet()
            val persistedShowOnlyHiddenTasks = preferences[PreferencesKeys.PERSISTED_SHOW_ONLY_HIDDEN_TASKS] ?: false

            AppSettings(
                hideCompletedTasks = hideCompleted,
                notificationOffsetMinutes = notificationOffset,
                userDefinedCategories = userDefinedCategories,
                persistedActiveCategoryFilters = persistedActiveCategoryFilters,
                persistedShowOnlyHiddenTasks = persistedShowOnlyHiddenTasks
            )
        }

    suspend fun updateHideCompletedTasks(hide: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_COMPLETED_TASKS] = hide
        }
    }

    suspend fun updateNotificationOffset(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_OFFSET_MINUTES] = minutes
        }
    }

    suspend fun addUserCategory(category: String) {
        dataStore.edit { preferences ->
            val currentCategories = preferences[PreferencesKeys.USER_DEFINED_CATEGORIES] ?: emptySet()
            if (category.isNotBlank() &&
                !predefinedCategories.any { it.equals(category, ignoreCase = true) } &&
                !currentCategories.any { it.equals(category, ignoreCase = true) }) {
                preferences[PreferencesKeys.USER_DEFINED_CATEGORIES] = currentCategories + category.trim()
            }
        }
    }

    suspend fun removeUserCategory(category: String) {
        dataStore.edit { preferences ->
            val currentCategories = preferences[PreferencesKeys.USER_DEFINED_CATEGORIES] ?: emptySet()
            preferences[PreferencesKeys.USER_DEFINED_CATEGORIES] = currentCategories.filterNot { it.equals(category, ignoreCase = true) }.toSet()
        }
    }

    suspend fun updatePersistedActiveCategoryFilters(categories: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PERSISTED_ACTIVE_CATEGORY_FILTERS] = categories
        }
    }

    suspend fun updatePersistedShowOnlyHiddenTasks(showHidden: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PERSISTED_SHOW_ONLY_HIDDEN_TASKS] = showHidden
        }
    }
}

data class AppSettings(
    val hideCompletedTasks: Boolean,
    val notificationOffsetMinutes: Int,
    val userDefinedCategories: Set<String>,
    val persistedActiveCategoryFilters: Set<String>,
    val persistedShowOnlyHiddenTasks: Boolean
)

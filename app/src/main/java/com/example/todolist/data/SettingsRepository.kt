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
        // HIDE_COMPLETED_TASKS zostało usunięte
        val SELECTED_FILTER_CATEGORIES = stringSetPreferencesKey("selected_filter_categories")
        val NOTIFICATION_OFFSET_MINUTES = intPreferencesKey("notification_offset_minutes")
        val USER_DEFINED_CATEGORIES = stringSetPreferencesKey("user_defined_categories")
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
            // hideCompleted zostało usunięte
            val selectedFilterCategories = preferences[PreferencesKeys.SELECTED_FILTER_CATEGORIES] ?: emptySet()
            val notificationOffset = preferences[PreferencesKeys.NOTIFICATION_OFFSET_MINUTES] ?: 15
            val userDefinedCategories = preferences[PreferencesKeys.USER_DEFINED_CATEGORIES] ?: emptySet()

            AppSettings(selectedFilterCategories, notificationOffset, userDefinedCategories)
        }

    // updateHideCompletedTasks zostało usunięte

    suspend fun updateSelectedFilterCategories(categories: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_FILTER_CATEGORIES] = categories
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
}

data class AppSettings(
    // hideCompletedTasks zostało usunięte
    val selectedFilterCategories: Set<String>,
    val notificationOffsetMinutes: Int,
    val userDefinedCategories: Set<String>
)

package com.example.todolist

import android.app.Application
import com.example.todolist.data.AppDatabase

class TodoApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
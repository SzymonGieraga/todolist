package com.example.todolist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "tasks")
@TypeConverters(Converters::class)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var title: String,
    var description: String?,
    val creationTime: Long,
    var executionTime: Long?,
    var isCompleted: Boolean = false,
    var notificationEnabled: Boolean = false,
    var category: String?,
    var attachments: List<String> = emptyList(),
    var isIndividuallyHidden: Boolean = false
)

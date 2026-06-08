package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "removal_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalPath: String,
    val removedPath: String,
    val timestamp: Long = System.currentTimeMillis()
)

package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val label: String,
    val keyValue: String,
    val createdAt: Long = System.currentTimeMillis(),
    val scriptType: String, // "bash", "python", "nodejs"
    val serviceType: String, // "AI Assistant", "Web Scraper", "Database Link", "Universal Script"
    val status: String = "ACTIVE", // "ACTIVE", "REVOKED", "EXPIRED"
    val isSynced: Boolean = false
)

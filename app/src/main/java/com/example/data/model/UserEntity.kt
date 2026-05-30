package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val displayName: String,
    val avatarUrl: String,
    val isAdmin: Boolean = false,
    val passwordHash: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

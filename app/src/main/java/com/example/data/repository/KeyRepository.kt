package com.example.data.repository

import com.example.data.dao.ApiKeyDao
import com.example.data.dao.UserDao
import com.example.data.model.ApiKeyEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

class KeyRepository(
    private val userDao: UserDao,
    private val apiKeyDao: ApiKeyDao
) {
    // User operations
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)

    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)

    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)

    // ApiKey operations
    fun getKeysForUser(userEmail: String): Flow<List<ApiKeyEntity>> = apiKeyDao.getKeysForUser(userEmail)

    fun getAllKeys(): Flow<List<ApiKeyEntity>> = apiKeyDao.getAllKeys()

    suspend fun getKeyById(id: Int): ApiKeyEntity? = apiKeyDao.getKeyById(id)

    suspend fun insertKey(key: ApiKeyEntity) = apiKeyDao.insertKey(key)

    suspend fun updateKey(key: ApiKeyEntity) = apiKeyDao.updateKey(key)

    suspend fun deleteKeyById(id: Int) = apiKeyDao.deleteKeyById(id)
}

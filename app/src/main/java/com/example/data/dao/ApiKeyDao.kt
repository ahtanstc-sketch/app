package com.example.data.dao

import androidx.room.*
import com.example.data.model.ApiKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys WHERE userEmail = :userEmail ORDER BY createdAt DESC")
    fun getKeysForUser(userEmail: String): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys ORDER BY createdAt DESC")
    fun getAllKeys(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys WHERE id = :id LIMIT 1")
    suspend fun getKeyById(id: Int): ApiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: ApiKeyEntity)

    @Update
    suspend fun updateKey(key: ApiKeyEntity)

    @Delete
    suspend fun deleteKey(key: ApiKeyEntity)

    @Query("DELETE FROM api_keys WHERE id = :id")
    suspend fun deleteKeyById(id: Int)
}

package com.mohamed.miqaat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InvocationDao {

    @Query("SELECT * FROM invocation ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<InvocationEntity>>

    @Query("SELECT * FROM invocation ORDER BY sortOrder, id")
    suspend fun getAll(): List<InvocationEntity>

    @Insert
    suspend fun insert(entity: InvocationEntity): Long

    /** Semis des invocations livrées : sans effet si l'id est déjà pris. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entities: List<InvocationEntity>)

    @Update
    suspend fun update(entity: InvocationEntity)

    @Query("SELECT * FROM invocation WHERE id = :id")
    suspend fun getById(id: Long): InvocationEntity?

    /** Une invocation livrée ne se supprime pas : la clause le garantit côté base. */
    @Query("DELETE FROM invocation WHERE id = :id AND builtinKey IS NULL")
    suspend fun deleteUserInvocation(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM invocation")
    suspend fun maxSortOrder(): Int
}

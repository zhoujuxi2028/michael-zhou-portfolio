package com.michaelzhou.composedemo.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE userEmail = :email ORDER BY id ASC")
    suspend fun getTodosForUser(email: String): List<TodoEntity>

    @Insert
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM todos WHERE userEmail = :email")
    suspend fun deleteAllForUser(email: String)
}

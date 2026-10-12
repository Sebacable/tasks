package com.example.planner

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY category, title")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM completions")
    fun getAllCompletions(): Flow<List<Completion>>

    @Insert
    suspend fun insertTask(task: Task): Long

    @Delete
    suspend fun deleteTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(c: Completion)

    @Query("DELETE FROM completions WHERE taskId = :taskId AND periodKey = :periodKey")
    suspend fun deleteCompletion(taskId: Long, periodKey: String)
}

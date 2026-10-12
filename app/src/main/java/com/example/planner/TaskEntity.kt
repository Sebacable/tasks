package com.example.planner

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PeriodType { DAILY, WEEKLY, MONTHLY }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val period: PeriodType,
    /** Fecha de creación en epochDay (LocalDate.toEpochDay) */
    val createdEpochDay: Long
)

/**
 * Un registro por cada período en que la tarea se cumplió.
 * periodKey: "2026-10-12" (diario), "2026-W42" (semanal), "2026-10" (mensual).
 */
@Entity(
    tableName = "completions",
    primaryKeys = ["taskId", "periodKey"],
    foreignKeys = [ForeignKey(
        entity = Task::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("taskId")]
)
data class Completion(
    val taskId: Long,
    val periodKey: String
)

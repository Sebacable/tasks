package com.example.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TaskUi(
    val task: Task,
    val doneNow: Boolean,
    val doneCount: Int,
    val rate: Float?,        // null = todavía sin períodos para medir
    val streak: Int
)

data class CategoryStat(val name: String, val rate: Float)

data class StatsReport(
    val globalRate: Float?,
    val doneThisPeriod: Int,
    val totalTasks: Int,
    val categories: List<CategoryStat>,
    val strongPoints: List<String>,
    val weakPoints: List<String>
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).taskDao()

    private val _selectedPeriod = MutableStateFlow(PeriodType.DAILY)
    val selectedPeriod: StateFlow<PeriodType> = _selectedPeriod.asStateFlow()

    /** Se actualiza al volver a la app y una vez por minuto, para detectar el cambio de día. */
    private val _today = MutableStateFlow(LocalDate.now())

    fun refreshDate() {
        val now = LocalDate.now()
        if (now != _today.value) _today.value = now
    }

    private val allUi: StateFlow<List<TaskUi>> = combine(
        dao.getAllTasks(), dao.getAllCompletions(), _today
    ) { tasks, completions, today ->
        val byTask = completions.groupBy({ it.taskId }, { it.periodKey })
        tasks.map { buildUi(it, byTask[it.id].orEmpty().toSet(), today) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentTasks: StateFlow<List<TaskUi>> = combine(allUi, _selectedPeriod) { list, p ->
        list.filter { it.task.period == p }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<StatsReport> = allUi.map { calculateStats(it) }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            StatsReport(null, 0, 0, emptyList(), emptyList(), emptyList())
        )

    fun selectPeriod(period: PeriodType) { _selectedPeriod.value = period }

    fun toggleTask(ui: TaskUi) {
        val key = Periods.key(ui.task.period, _today.value)
        viewModelScope.launch {
            if (ui.doneNow) dao.deleteCompletion(ui.task.id, key)
            else dao.insertCompletion(Completion(ui.task.id, key))
        }
    }

    fun addTask(title: String, category: String, period: PeriodType) {
        viewModelScope.launch {
            dao.insertTask(
                Task(
                    title = title.trim(),
                    category = category.trim().lowercase().replaceFirstChar { it.uppercase() },
                    period = period,
                    createdEpochDay = _today.value.toEpochDay()
                )
            )
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { dao.deleteTask(task) }
    }

    private fun buildUi(task: Task, keys: Set<String>, today: LocalDate): TaskUi {
        val created = LocalDate.ofEpochDay(task.createdEpochDay)
        val currentKey = Periods.key(task.period, today)
        val doneNow = currentKey in keys
        // Denominador: períodos cerrados + el actual sólo si ya se cumplió
        // (así un período en curso no baja el porcentaje).
        val closed = Periods.closedPeriods(task.period, created, today)
        val denominator = closed + if (doneNow) 1 else 0
        val rate = if (denominator > 0) (keys.size.toFloat() / denominator * 100f).coerceAtMost(100f) else null

        // Racha: períodos consecutivos cumplidos hacia atrás (el actual cuenta si ya está hecho).
        var streak = 0
        var date = if (doneNow) today else previous(task.period, today)
        while (!date.isBefore(created) && Periods.key(task.period, date) in keys) {
            streak++
            date = previous(task.period, date)
        }
        return TaskUi(task, doneNow, keys.size, rate, streak)
    }

    private fun previous(p: PeriodType, d: LocalDate): LocalDate = when (p) {
        PeriodType.DAILY -> d.minusDays(1)
        PeriodType.WEEKLY -> d.minusWeeks(1)
        PeriodType.MONTHLY -> d.minusMonths(1)
    }

    private fun calculateStats(items: List<TaskUi>): StatsReport {
        val measured = items.filter { it.rate != null }
        val doneNow = items.count { it.doneNow }

        if (measured.isEmpty()) {
            return StatsReport(
                globalRate = null,
                doneThisPeriod = doneNow,
                totalTasks = items.size,
                categories = emptyList(),
                strongPoints = listOf("Sin datos suficientes todavía"),
                weakPoints = listOf("Sin datos suficientes todavía")
            )
        }

        val global = measured.map { it.rate!! }.average().toFloat()
        val categories = measured.groupBy { it.task.category }
            .map { (name, list) -> CategoryStat(name, list.map { it.rate!! }.average().toFloat()) }
            .sortedByDescending { it.rate }

        val strong = categories.filter { it.rate >= 70f }.map { it.name }
        val weak = categories.filter { it.rate < 50f }.map { it.name }

        return StatsReport(
            globalRate = global,
            doneThisPeriod = doneNow,
            totalTasks = items.size,
            categories = categories,
            strongPoints = strong.ifEmpty { listOf("Ningún área supera el 70%") },
            weakPoints = weak.ifEmpty { listOf("No hay áreas críticas detectadas") }
        )
    }
}

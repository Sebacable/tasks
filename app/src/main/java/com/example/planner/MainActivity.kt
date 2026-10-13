package com.example.planner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlannerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerApp(viewModel: TaskViewModel = viewModel()) {
    var currentTab by remember { mutableIntStateOf(0) }
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val selectedArea by viewModel.selectedArea.collectAsStateWithLifecycle()
    val tasks by viewModel.currentTasks.collectAsStateWithLifecycle()
    val areas by viewModel.areas.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Detectar cambio de día al volver a la app y mientras está abierta.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDate() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            viewModel.refreshDate()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AreasDrawer(
                areas = areas,
                selectedArea = selectedArea,
                onSelect = { area ->
                    viewModel.selectArea(area)
                    currentTab = 0
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Áreas")
                        }
                    },
                    title = {
                        Text(
                            when {
                                currentTab == 1 -> "Diagnóstico & Balance"
                                selectedArea != null -> selectedArea!!
                                else -> "Mi Plan de Acción"
                            }
                        )
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        label = { Text("Tareas") },
                        icon = { Text("✅") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        label = { Text("Estadísticas") },
                        icon = { Text("📊") }
                    )
                }
            },
            floatingActionButton = {
                if (currentTab == 0) {
                    FloatingActionButton(onClick = { showAddDialog = true }) {
                        Text("+", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (currentTab == 0) {
                    TasksScreen(
                        selectedPeriod = selectedPeriod,
                        selectedArea = selectedArea,
                        tasks = tasks,
                        onSelectPeriod = viewModel::selectPeriod,
                        onClearArea = { viewModel.selectArea(null) },
                        onToggle = viewModel::toggleTask,
                        onLongPress = { taskToDelete = it.task }
                    )
                } else {
                    StatsScreen(stats)
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            period = selectedPeriod,
            existingAreas = areas.map { it.name },
            initialArea = selectedArea ?: "",
            onDismiss = { showAddDialog = false },
            onConfirm = { title, cat ->
                viewModel.addTask(title, cat, selectedPeriod)
                showAddDialog = false
            }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Eliminar tarea") },
            text = { Text("¿Borrar \"${task.title}\" y todo su historial? No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task)
                    taskToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun AreasDrawer(areas: List<AreaInfo>, selectedArea: String?, onSelect: (String?) -> Unit) {
    ModalDrawerSheet {
        Text(
            "Áreas",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 12.dp)
        )
        LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
            item {
                NavigationDrawerItem(
                    label = { Text("Todas las áreas") },
                    badge = { Text("${areas.sumOf { it.total }}") },
                    selected = selectedArea == null,
                    onClick = { onSelect(null) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            if (areas.isEmpty()) {
                item {
                    Text(
                        "Todavía no hay áreas. Se crean solas cuando agregás una tarea.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(areas, key = { it.name }) { area ->
                NavigationDrawerItem(
                    label = { Text(area.name) },
                    badge = {
                        Text(
                            if (area.pending > 0) "${area.pending} pend. · ${area.total}" else "✓ ${area.total}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    selected = selectedArea == area.name,
                    onClick = { onSelect(area.name) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    selectedPeriod: PeriodType,
    selectedArea: String?,
    tasks: List<TaskUi>,
    onSelectPeriod: (PeriodType) -> Unit,
    onClearArea: () -> Unit,
    onToggle: (TaskUi) -> Unit,
    onLongPress: (TaskUi) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PeriodType.entries.forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { onSelectPeriod(period) },
                    label = { Text(Periods.label(period)) }
                )
            }
        }

        if (selectedArea != null) {
            InputChip(
                selected = true,
                onClick = onClearArea,
                label = { Text("Área: $selectedArea") },
                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Quitar filtro") },
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (tasks.isEmpty()) {
            Text(
                "No hay tareas ${Periods.label(selectedPeriod).lowercase()}s" +
                    (selectedArea?.let { " en $it" } ?: "") + ".\nTocá + para agregar una.",
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Mantené apretada una tarea para eliminarla",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        val grouped = tasks.groupBy { it.task.category }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            grouped.forEach { (area, list) ->
                if (selectedArea == null) {
                    stickyHeader(key = "h_$area") {
                        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "$area · ${list.count { it.doneNow }}/${list.size}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                    }
                }
                items(list, key = { it.task.id }) { ui -> TaskCard(ui, onToggle, onLongPress) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(ui: TaskUi, onToggle: (TaskUi) -> Unit, onLongPress: (TaskUi) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onToggle(ui) },
                onLongClick = { onLongPress(ui) }
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(ui.task.title, style = MaterialTheme.typography.titleMedium)
                val rateText = ui.rate?.let { "%.0f%% cumplido".format(it) } ?: "Recién creada"
                val streakText = if (ui.streak > 1) " · 🔥 racha ${ui.streak}" else ""
                Text(
                    rateText + streakText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = ui.doneNow,
                onCheckedChange = { onToggle(ui) }
            )
        }
    }
}

@Composable
fun StatsScreen(stats: StatsReport) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Efectividad Global (histórica)", style = MaterialTheme.typography.titleMedium)
                Text(
                    stats.globalRate?.let { "%.1f%%".format(it) } ?: "—",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Hechas en el período actual: ${stats.doneThisPeriod} de ${stats.totalTasks}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (stats.categories.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Por área", style = MaterialTheme.typography.titleSmall)
                    stats.categories.forEach { c ->
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(c.name, style = MaterialTheme.typography.bodyMedium)
                                Text("%.0f%%".format(c.rate), style = MaterialTheme.typography.bodyMedium)
                            }
                            LinearProgressIndicator(
                                progress = { c.rate / 100f },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                color = when {
                                    c.rate >= 70f -> MaterialTheme.colorScheme.primary
                                    c.rate < 50f -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.tertiary
                                }
                            )
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Puntos Fuertes (≥ 70% éxito)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                stats.strongPoints.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Puntos Flojos (< 50% éxito)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                stats.weakPoints.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    period: PeriodType,
    existingAreas: List<String>,
    initialArea: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialArea) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva tarea · ${Periods.label(period)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la tarea") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Área (nueva o elegí abajo)") },
                    singleLine = true
                )
                if (existingAreas.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        existingAreas.forEach { area ->
                            FilterChip(
                                selected = category.trim().equals(area, ignoreCase = true),
                                onClick = { category = area },
                                label = { Text(area) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && category.isNotBlank(),
                onClick = { onConfirm(title, category) }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

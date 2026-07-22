package com.aviatechnik.android.ui.screens.workorders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aviatechnik.android.data.api.ProcessesData
import com.aviatechnik.android.data.api.TasksData
import com.aviatechnik.android.data.repository.ApiResult
import com.aviatechnik.android.data.repository.WorkorderRepository
import com.aviatechnik.android.ui.theme.AviaDeepSkyBlue
import com.aviatechnik.android.ui.theme.AviaTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

/* ═══════════════════════════ Tasks ═══════════════════════════ */

data class TasksUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: TasksData? = null,
    val busy: Boolean = false,
    val actionError: String? = null,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: WorkorderRepository,
) : ViewModel() {

    private val woId: Int = checkNotNull(savedState["id"]).toString().toInt()

    val state: StateFlow<TasksUiState> get() = _state
    private val _state = MutableStateFlow(TasksUiState())

    init { reload() }

    fun reload() {
        _state.value = _state.value.copy(loading = _state.value.data == null, error = null)
        viewModelScope.launch {
            when (val res = repo.tasks(woId)) {
                is ApiResult.Ok -> _state.value = _state.value.copy(loading = false, data = res.data)
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }

    fun setDate(taskId: Int, field: String, date: String) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, actionError = null)
        viewModelScope.launch {
            when (val res = repo.updateTaskDate(woId, taskId, field, date)) {
                is ApiResult.Ok -> { _state.value = _state.value.copy(busy = false); reload() }
                is ApiResult.Error -> _state.value = _state.value.copy(busy = false, actionError = res.message)
            }
        }
    }
}

@Composable
fun TasksScreen(onGo: (String) -> Unit, vm: TasksViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    ScreenScaffold(active = "tasks", busy = state.busy, actionError = state.actionError, onGo = onGo) {
        when {
            state.loading -> Centered { CircularProgressIndicator() }
            state.error != null -> Centered { Text(state.error!!, color = MaterialTheme.colorScheme.error) }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.data!!.groups, key = { it.id }) { group ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    group.name ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (group.isDone) AviaTextSecondary else AviaDeepSkyBlue,
                                )
                                if (group.isDone) Text("  ✓ done", style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
                            }
                            group.tasks.forEach { task ->
                                Column(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                                    Text(task.name ?: "", style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        if (task.hasStartDate) {
                                            DateField(
                                                label = "Start",
                                                value = task.main?.dateStart,
                                                editable = task.canEditStart,
                                                onPick = { vm.setDate(task.id, "start", it) },
                                            )
                                        }
                                        DateField(
                                            label = "Finish",
                                            value = task.main?.dateFinish,
                                            editable = task.canEditFinish,
                                            onPick = { vm.setDate(task.id, "finish", it) },
                                        )
                                        task.main?.user?.name?.let {
                                            Text(it, style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary,
                                                modifier = Modifier.align(Alignment.CenterVertically))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ═══════════════════════════ Processes ═══════════════════════════ */

data class ProcessesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: ProcessesData? = null,
    val busy: Boolean = false,
    val actionError: String? = null,
)

@HiltViewModel
class ProcessesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: WorkorderRepository,
) : ViewModel() {

    private val woId: Int = checkNotNull(savedState["id"]).toString().toInt()

    val state: StateFlow<ProcessesUiState> get() = _state
    private val _state = MutableStateFlow(ProcessesUiState())

    init { reload() }

    fun reload() {
        _state.value = _state.value.copy(loading = _state.value.data == null, error = null)
        viewModelScope.launch {
            when (val res = repo.processes(woId)) {
                is ApiResult.Ok -> _state.value = _state.value.copy(loading = false, data = res.data)
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }

    fun setDate(processId: Int, field: String, date: String) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, actionError = null)
        viewModelScope.launch {
            when (val res = repo.updateProcessDate(processId, field, date)) {
                is ApiResult.Ok -> { _state.value = _state.value.copy(busy = false); reload() }
                is ApiResult.Error -> _state.value = _state.value.copy(busy = false, actionError = res.message)
            }
        }
    }
}

@Composable
fun ProcessesScreen(onGo: (String) -> Unit, vm: ProcessesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    ScreenScaffold(active = "process", busy = state.busy, actionError = state.actionError, onGo = onGo) {
        when {
            state.loading -> Centered { CircularProgressIndicator() }
            state.error != null -> Centered { Text(state.error!!, color = MaterialTheme.colorScheme.error) }
            state.data!!.components.isEmpty() -> Centered { Text("No processes", color = AviaTextSecondary) }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.data!!.components, key = { it.id }) { comp ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                listOfNotNull(comp.iplNum, comp.partNumber, comp.name).joinToString("  ·  "),
                                style = MaterialTheme.typography.titleSmall,
                                color = AviaDeepSkyBlue,
                            )
                            comp.processes.forEach { p ->
                                Column(Modifier.fillMaxWidth()) {
                                    Text(
                                        listOfNotNull(p.name, p.repairOrder?.let { "RO $it" }).joinToString("  ·  "),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    p.description?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = AviaTextSecondary)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        DateField("Start", p.dateStart, p.canEditStart) { vm.setDate(p.id, "start", it) }
                                        DateField("Finish", p.dateFinish, p.canEditFinish) { vm.setDate(p.id, "finish", it) }
                                        DateField("Promise", p.datePromise, p.canEditPromise) { vm.setDate(p.id, "promise", it) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ═══════════════════════════ shared ═══════════════════════════ */

@Composable
private fun ScreenScaffold(
    active: String,
    busy: Boolean,
    actionError: String?,
    onGo: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        com.aviatechnik.android.ui.components.WoMenuBar(active = active, onGo = onGo)
        if (busy) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
        actionError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        content()
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

/** "Label: 2026-07-21" — tap to pick a date when editable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: String?, editable: Boolean, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }

    Column(
        if (editable) Modifier.clickable { open = true } else Modifier,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
        Text(
            value ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = when {
                !editable -> MaterialTheme.colorScheme.onSurface
                value == null -> AviaDeepSkyBlue
                else -> AviaDeepSkyBlue
            },
        )
    }

    if (open) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        onPick(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

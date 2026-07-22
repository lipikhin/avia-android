package com.aviatechnik.android.ui.screens.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aviatechnik.android.data.api.MachiningData
import com.aviatechnik.android.data.api.MachiningDetailItemDto
import com.aviatechnik.android.data.api.MachiningWoData
import com.aviatechnik.android.data.repository.ApiResult
import com.aviatechnik.android.data.repository.ShopRepository
import com.aviatechnik.android.ui.screens.workorders.DateField
import com.aviatechnik.android.ui.theme.AviaDeepSkyBlue
import com.aviatechnik.android.ui.theme.AviaTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ── section: workorder queue list ─────────────────────────────── */

data class MachiningListState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: MachiningData? = null,
)

@HiltViewModel
class MachiningListViewModel @Inject constructor(
    private val repo: ShopRepository,
) : ViewModel() {

    val state: StateFlow<MachiningListState> get() = _state
    private val _state = MutableStateFlow(MachiningListState())
    var myWo = false
        private set

    init { load() }

    fun setMyWo(value: Boolean) {
        if (myWo == value) return
        myWo = value
        load()
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val res = repo.machining(myWo)) {
                is ApiResult.Ok -> _state.value = MachiningListState(loading = false, data = res.data)
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }
}

@Composable
fun MachiningSection(
    onOpen: (Int, Boolean) -> Unit,
    vm: MachiningListViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Checkbox(checked = vm.myWo, onCheckedChange = { vm.setMyWo(it) })
            Text("My WO", style = MaterialTheme.typography.bodySmall)
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.data!!.items, key = { it.workorder.id }) { item ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(item.workorder.id, vm.myWo) },
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            item.queueDisplay?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.titleMedium, color = AviaDeepSkyBlue)
                                Spacer(Modifier.size(10.dp))
                            }
                            Column {
                                Text(
                                    item.workorder.numberDisplay ?: item.workorder.number.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AviaDeepSkyBlue,
                                )
                                item.workorder.unit?.let { u ->
                                    Text(
                                        listOfNotNull(u.partNumber, u.name).joinToString(" — "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AviaTextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ── screen: workorder steps ───────────────────────────────────── */

data class MachiningWoState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: MachiningWoData? = null,
    val busy: Boolean = false,
    val actionError: String? = null,
)

@HiltViewModel
class MachiningWoViewModel @Inject constructor(
    private val repo: ShopRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val woId: Int = checkNotNull(savedStateHandle["id"]).toString().toInt()
    private val myWo: Boolean = (savedStateHandle.get<String>("myWo") ?: "0") == "1"

    val state: StateFlow<MachiningWoState> get() = _state
    private val _state = MutableStateFlow(MachiningWoState())

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = _state.value.data == null, error = null)
        viewModelScope.launch {
            when (val res = repo.machiningWorkorder(woId, myWo)) {
                is ApiResult.Ok -> _state.value = _state.value.copy(loading = false, data = res.data)
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }

    fun setStepDate(stepId: Int, field: String, date: String) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, actionError = null)
        viewModelScope.launch {
            val err = when (val res = repo.updateStepDate(stepId, field, date)) {
                is ApiResult.Ok -> { load(); null }
                is ApiResult.Error -> res.message
            }
            _state.value = _state.value.copy(busy = false, actionError = err)
        }
    }
}

@Composable
fun MachiningWoScreen(
    onBack: () -> Unit,
    vm: MachiningWoViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var hideClosed by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                state.data?.workorder?.let { it.numberDisplay ?: it.number.toString() } ?: "Machining",
                style = MaterialTheme.typography.titleMedium,
                color = AviaDeepSkyBlue,
                modifier = Modifier.weight(1f),
            )
            Checkbox(checked = hideClosed, onCheckedChange = { hideClosed = it })
            Text("Hide closed", style = MaterialTheme.typography.bodySmall)
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.actionError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> {
                val items = state.data!!.detailItems.filter { item ->
                    !hideClosed || !itemClosed(item)
                }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items) { item -> MachiningDetailCard(item, vm) }
                }
            }
        }
    }
}

/** A group/pending-steps card is closed when every step has a finish date. */
private fun itemClosed(item: MachiningDetailItemDto): Boolean {
    val steps = if (item.steps.isNotEmpty()) item.steps else listOf(item)
    return steps.all { it.step?.dateFinish != null }
}

@Composable
private fun MachiningDetailCard(item: MachiningDetailItemDto, vm: MachiningWoViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.detailLabel ?: item.detailName ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = AviaDeepSkyBlue,
            )
            item.detailSerial?.takeIf { it.isNotBlank() }?.let {
                Text("S/N $it", style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
            }
            item.processesLabel?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            item.dateParent?.let {
                Text("Received: $it", style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
            }
            val steps = if (item.steps.isNotEmpty()) item.steps else listOf(item)
            steps.forEachIndexed { i, stepItem ->
                if (i > 0) HorizontalDivider()
                StepRow(stepItem, vm)
            }
        }
    }
}

@Composable
private fun StepRow(item: MachiningDetailItemDto, vm: MachiningWoViewModel) {
    val step = item.step ?: return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                step.description?.takeIf { it.isNotBlank() } ?: "Step ${step.stepIndex + 1}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            item.displayMachinist?.name?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DateField("Start", step.dateStart ?: item.effectiveStepStart, item.canEdit) { date ->
                vm.setStepDate(step.id, "start", date)
            }
            DateField("Finish", step.dateFinish, item.canEdit) { date ->
                vm.setStepDate(step.id, "finish", date)
            }
        }
    }
}

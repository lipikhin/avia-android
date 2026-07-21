package com.aviatechnik.android.ui.screens.drafts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aviatechnik.android.data.api.DraftCreateRequest
import com.aviatechnik.android.data.api.NamedDto
import com.aviatechnik.android.data.api.UnitDto
import com.aviatechnik.android.data.api.WorkorderItemDto
import com.aviatechnik.android.data.repository.ApiResult
import com.aviatechnik.android.data.repository.DraftsRepository
import com.aviatechnik.android.data.repository.WorkorderRepository
import com.aviatechnik.android.ui.theme.AviaDeepSkyBlue
import com.aviatechnik.android.ui.theme.AviaTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ═════════════════════ Drafts list (Home section) ═════════════════════ */

data class DraftsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<WorkorderItemDto> = emptyList(),
)

@HiltViewModel
class DraftsViewModel @Inject constructor(
    private val repo: WorkorderRepository,
) : ViewModel() {

    val state: StateFlow<DraftsUiState> get() = _state
    private val _state = MutableStateFlow(DraftsUiState())

    init { reload() }

    fun reload() {
        _state.value = _state.value.copy(loading = _state.value.items.isEmpty(), error = null)
        viewModelScope.launch {
            when (val res = repo.list(scope = "draft", includeDone = true)) {
                is ApiResult.Ok -> _state.value = _state.value.copy(loading = false, items = res.data.items)
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }
}

@Composable
fun DraftsSection(
    onOpenWorkorder: (Int) -> Unit,
    onCreateDraft: () -> Unit,
    vm: DraftsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = onCreateDraft,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) { Text("+ New draft") }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No drafts", color = AviaTextSecondary)
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
                items(state.items, key = { it.id }) { wo ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenWorkorder(wo.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                wo.numberDisplay ?: wo.number.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = AviaDeepSkyBlue,
                            )
                            Spacer(Modifier.weight(1f))
                            wo.openAt?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary) }
                        }
                        val sub = listOfNotNull(wo.customer?.name, wo.unit?.partNumber).joinToString("  ·  ")
                        if (sub.isNotEmpty()) {
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = AviaTextSecondary)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                }
            }
        }
    }
}

/* ═════════════════════ Draft create ═════════════════════ */

data class DraftCreateUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val draftNumber: Long? = null,
    val units: List<UnitDto> = emptyList(),
    val customers: List<NamedDto> = emptyList(),
    val busy: Boolean = false,
    val actionError: String? = null,
)

@HiltViewModel
class DraftCreateViewModel @Inject constructor(
    private val repo: DraftsRepository,
) : ViewModel() {

    val state: StateFlow<DraftCreateUiState> get() = _state
    private val _state = MutableStateFlow(DraftCreateUiState())

    init {
        viewModelScope.launch {
            when (val res = repo.options()) {
                is ApiResult.Ok -> _state.value = _state.value.copy(
                    loading = false,
                    draftNumber = res.data.draftNumber,
                    units = res.data.units,
                    customers = res.data.customers,
                )
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }

    fun createUnit(partNumber: String, name: String?, onDone: (UnitDto) -> Unit) = action {
        when (val res = repo.createUnit(partNumber, name)) {
            is ApiResult.Ok -> {
                val unit = res.data.unit
                _state.value = _state.value.copy(units = listOf(unit) + _state.value.units)
                onDone(unit)
                null
            }
            is ApiResult.Error -> res.message
        }
    }

    fun create(req: DraftCreateRequest, onDone: (Int) -> Unit) = action {
        when (val res = repo.create(req)) {
            is ApiResult.Ok -> { onDone(res.data.workorder.id); null }
            is ApiResult.Error -> res.message
        }
    }

    private fun action(block: suspend () -> String?) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, actionError = null)
        viewModelScope.launch {
            val err = block()
            _state.value = _state.value.copy(busy = false, actionError = err)
        }
    }
}

private val ARRIVAL_STATUSES = listOf(
    null to "—",
    "ok" to "OK",
    "easy" to "Light repair",
    "medium" to "Medium repair",
    "hard" to "Hard repair",
    "replace" to "New box",
)

@Composable
fun DraftCreateScreen(
    onBack: () -> Unit,
    onCreated: (Int) -> Unit,
    vm: DraftCreateViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    var unit by remember { mutableStateOf<UnitDto?>(null) }
    var customer by remember { mutableStateOf<NamedDto?>(null) }
    var serial by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var customerPo by remember { mutableStateOf("") }
    var openAt by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    val flags = remember {
        mutableStateOf(
            mapOf(
                "external_damage" to false,
                "received_disassembly" to false,
                "disassembly_upon_arrival" to false,
                "nameplate_missing" to false,
                "extra_parts" to false,
            ),
        )
    }
    var rack by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var column by remember { mutableStateOf("") }
    var boxStatus by remember { mutableStateOf<String?>(null) }
    var boxNotes by remember { mutableStateOf("") }

    var pickUnit by remember { mutableStateOf(false) }
    var pickCustomer by remember { mutableStateOf(false) }
    var newUnitDialog by remember { mutableStateOf(false) }

    if (pickUnit) {
        SearchPickerDialog(
            title = "Unit (P/N)",
            items = state.units.map { it.id to listOfNotNull(it.partNumber, it.name).joinToString(" · ") },
            extraAction = "+ New unit" to { pickUnit = false; newUnitDialog = true },
            onPick = { id -> unit = state.units.firstOrNull { it.id == id }; pickUnit = false },
            onDismiss = { pickUnit = false },
        )
    }
    if (pickCustomer) {
        SearchPickerDialog(
            title = "Customer",
            items = state.customers.map { it.id to (it.name ?: "") },
            extraAction = null,
            onPick = { id -> customer = state.customers.firstOrNull { it.id == id }; pickCustomer = false },
            onDismiss = { pickCustomer = false },
        )
    }
    if (newUnitDialog) {
        NewUnitDialog(
            busy = state.busy,
            error = state.actionError,
            onSave = { pn, nm -> vm.createUnit(pn, nm) { created -> unit = created; newUnitDialog = false } },
            onDismiss = { newUnitDialog = false },
        )
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text("New draft", style = MaterialTheme.typography.titleLarge)
                state.draftNumber?.let {
                    Text("WO $it", style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
                }
            }
            if (state.busy) CircularProgressIndicator(Modifier.size(22.dp))
        }
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
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PickerRow("Unit (P/N)", unit?.let { listOfNotNull(it.partNumber, it.name).joinToString(" · ") }) { pickUnit = true }
                        PickerRow("Customer", customer?.name) { pickCustomer = true }
                        OutlinedTextField(value = serial, onValueChange = { serial = it },
                            label = { Text("S/N") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = description, onValueChange = { description = it },
                            label = { Text("Unit name / description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = customerPo, onValueChange = { customerPo = it },
                            label = { Text("Customer PO") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = openAt, onValueChange = { openAt = it },
                            label = { Text("Open at (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Arrival", style = MaterialTheme.typography.titleSmall, color = AviaDeepSkyBlue)
                        listOf(
                            "external_damage" to "External damage",
                            "received_disassembly" to "Received disassembled",
                            "disassembly_upon_arrival" to "Disassembly upon arrival",
                            "nameplate_missing" to "Nameplate missing",
                            "extra_parts" to "Extra parts",
                        ).forEach { (key, label) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = flags.value[key] == true,
                                    onCheckedChange = { v -> flags.value = flags.value + (key to v) },
                                )
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Storage", style = MaterialTheme.typography.titleSmall, color = AviaDeepSkyBlue)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumField("Rack", rack, { rack = it }, Modifier.weight(1f))
                            NumField("Level", level, { level = it }, Modifier.weight(1f))
                            NumField("Column", column, { column = it }, Modifier.weight(1f))
                        }
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Arrival box", style = MaterialTheme.typography.titleSmall, color = AviaDeepSkyBlue)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            ARRIVAL_STATUSES.forEach { (value, label) ->
                                FilterChip(
                                    selected = boxStatus == value,
                                    onClick = { boxStatus = value },
                                    label = { Text(label) },
                                )
                            }
                        }
                        OutlinedTextField(value = boxNotes, onValueChange = { boxNotes = it },
                            label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                    }
                }

                Button(
                    enabled = !state.busy && unit != null && customer != null,
                    onClick = {
                        vm.create(
                            DraftCreateRequest(
                                unitId = unit!!.id,
                                customerId = customer!!.id,
                                serialNumber = serial.ifBlank { null },
                                description = description.ifBlank { null },
                                openAt = openAt.ifBlank { null },
                                customerPo = customerPo.ifBlank { null },
                                externalDamage = flags.value["external_damage"] == true,
                                receivedDisassembly = flags.value["received_disassembly"] == true,
                                disassemblyUponArrival = flags.value["disassembly_upon_arrival"] == true,
                                nameplateMissing = flags.value["nameplate_missing"] == true,
                                extraParts = flags.value["extra_parts"] == true,
                                storageRack = rack.toIntOrNull(),
                                storageLevel = level.toIntOrNull(),
                                storageColumn = column.toIntOrNull(),
                                arrivalBoxStatus = boxStatus,
                                arrivalBoxNotes = boxNotes.ifBlank { null },
                            ),
                            onDone = onCreated,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.busy) "Creating…" else "Create draft") }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/* ── small shared pieces ───────────────────────────────────────── */

@Composable
private fun PickerRow(label: String, value: String?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
        Text(value ?: "— select —", style = MaterialTheme.typography.bodyMedium,
            color = if (value == null) AviaDeepSkyBlue else MaterialTheme.colorScheme.onSurface)
        HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(3)) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun SearchPickerDialog(
    title: String,
    items: List<Pair<Int, String>>,
    extraAction: Pair<String, () -> Unit>?,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                extraAction?.let { (label, act) ->
                    OutlinedButton(onClick = act, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text(label) }
                }
                val q = search.trim().lowercase()
                val shown = if (q.isEmpty()) items else items.filter { it.second.lowercase().contains(q) }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    items(shown, key = { it.first }) { (id, label) ->
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(id) }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NewUnitDialog(
    busy: Boolean,
    error: String?,
    onSave: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var pn by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New unit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = pn, onValueChange = { pn = it },
                    label = { Text("Part number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(enabled = !busy && pn.isNotBlank(), onClick = { onSave(pn.trim(), name.trim().ifBlank { null }) }) {
                Text(if (busy) "Saving…" else "Save")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

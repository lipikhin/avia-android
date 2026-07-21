package com.aviatechnik.android.ui.screens.components

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.aviatechnik.android.data.api.ApiUrls
import com.aviatechnik.android.data.api.AttachmentRequest
import com.aviatechnik.android.data.api.ComponentDto
import com.aviatechnik.android.data.api.ComponentsData
import com.aviatechnik.android.data.api.NamedDto
import com.aviatechnik.android.data.api.TdrAttachmentDto
import com.aviatechnik.android.data.repository.ApiResult
import com.aviatechnik.android.data.repository.ComponentsRepository
import com.aviatechnik.android.ui.theme.AviaDeepSkyBlue
import com.aviatechnik.android.ui.theme.AviaTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ComponentsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: ComponentsData? = null,
    val busy: Boolean = false,
    val actionError: String? = null,
)

@HiltViewModel
class ComponentsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: ComponentsRepository,
) : ViewModel() {

    private val woId: Int = checkNotNull(savedState["id"]).toString().toInt()

    val state: StateFlow<ComponentsUiState> get() = _state
    private val _state = MutableStateFlow(ComponentsUiState())

    init { reload() }

    fun reload() {
        _state.value = _state.value.copy(loading = _state.value.data == null, error = null)
        viewModelScope.launch {
            when (val res = repo.list(woId)) {
                is ApiResult.Ok -> _state.value = _state.value.copy(loading = false, data = res.data)
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }

    fun attach(body: AttachmentRequest, onDone: () -> Unit) = action(onDone) {
        when (val res = repo.attach(woId, body)) {
            is ApiResult.Ok -> { reload(); null }
            is ApiResult.Error -> res.message
        }
    }

    fun updateAttachment(tdrId: Int, body: AttachmentRequest, onDone: () -> Unit) = action(onDone) {
        when (val res = repo.updateAttachment(tdrId, body)) {
            is ApiResult.Ok -> { reload(); null }
            is ApiResult.Error -> res.message
        }
    }

    fun deleteAttachment(tdrId: Int) = action({}) {
        when (val res = repo.deleteAttachment(tdrId)) {
            is ApiResult.Ok -> { reload(); null }
            is ApiResult.Error -> res.message
        }
    }

    fun uploadPhoto(componentId: Int, file: File) = action({}) {
        when (val res = repo.uploadPhoto(componentId, file)) {
            is ApiResult.Ok -> { reload(); null }
            is ApiResult.Error -> res.message
        }
    }

    private fun action(onDone: () -> Unit, block: suspend () -> String?) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, actionError = null)
        viewModelScope.launch {
            val err = block()
            _state.value = _state.value.copy(busy = false, actionError = err)
            if (err == null) onDone()
        }
    }
}

@Composable
fun ComponentsScreen(onBack: () -> Unit, vm: ComponentsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // dialogs
    var pickingComponent by remember { mutableStateOf(false) }
    var attachTarget by remember { mutableStateOf<ComponentDto?>(null) }     // create for this component
    var editTarget by remember { mutableStateOf<TdrAttachmentDto?>(null) }   // edit this attachment
    var deleteTarget by remember { mutableStateOf<TdrAttachmentDto?>(null) }

    // component photo camera (survives process death — see WO detail notes)
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingComponentId by rememberSaveable { mutableStateOf<Int?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingPath
        val compId = pendingComponentId
        pendingPath = null
        if (ok && path != null && compId != null) {
            val file = File(path)
            if (file.exists() && file.length() > 0) vm.uploadPhoto(compId, file)
        }
    }
    fun launchCamera(componentId: Int) {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "comp_${System.currentTimeMillis()}.jpg")
        pendingPath = file.absolutePath
        pendingComponentId = componentId
        takePicture.launch(FileProvider.getUriForFile(context, "com.aviatechnik.android.fileprovider", file))
    }

    val data = state.data
    if (pickingComponent && data != null) {
        ComponentPickerDialog(
            components = data.manualComponents,
            onPick = { pickingComponent = false; attachTarget = it },
            onDismiss = { pickingComponent = false },
        )
    }
    attachTarget?.let { comp ->
        AttachmentFormDialog(
            title = "${comp.iplNum ?: ""} · ${comp.name ?: ""}",
            codes = data?.codes ?: emptyList(),
            necessaries = data?.necessaries ?: emptyList(),
            initial = null,
            busy = state.busy,
            error = state.actionError,
            onSave = { req -> vm.attach(req.copy(componentId = comp.id)) { attachTarget = null } },
            onDismiss = { attachTarget = null },
        )
    }
    editTarget?.let { tdr ->
        AttachmentFormDialog(
            title = "Edit attachment",
            codes = data?.codes ?: emptyList(),
            necessaries = data?.necessaries ?: emptyList(),
            initial = tdr,
            busy = state.busy,
            error = state.actionError,
            onSave = { req -> vm.updateAttachment(tdr.id, req) { editTarget = null } },
            onDismiss = { editTarget = null },
        )
    }
    deleteTarget?.let { tdr ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove attachment?") },
            text = { Text("${tdr.codeName ?: ""}${tdr.necessariesName?.let { " — $it" } ?: ""}") },
            confirmButton = {
                Button(onClick = { vm.deleteAttachment(tdr.id); deleteTarget = null }) { Text("Remove") }
            },
            dismissButton = { OutlinedButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
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
            Text("Components / TDR", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
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
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Button(onClick = { pickingComponent = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("+ Attach part")
                    }
                }
                if (data!!.attachedComponents.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text("No parts attached yet", color = AviaTextSecondary)
                        }
                    }
                }
                items(data.attachedComponents, key = { it.id }) { comp ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                comp.photo?.thumbUrl?.let {
                                    AsyncImage(
                                        model = ApiUrls.rebase(it),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                                    )
                                    Spacer(Modifier.padding(4.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        listOfNotNull(comp.iplNum, comp.partNumber).joinToString(" · "),
                                        style = MaterialTheme.typography.titleSmall, color = AviaDeepSkyBlue,
                                    )
                                    Text(comp.name ?: "", style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { launchCamera(comp.id) }) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Part photo", tint = AviaDeepSkyBlue)
                                }
                            }
                            comp.tdrs.forEach { tdr ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editTarget = tdr }
                                        .padding(vertical = 2.dp),
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            listOfNotNull(tdr.codeName, tdr.necessariesName).joinToString(" — "),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        val meta = listOfNotNull(
                                            tdr.qty?.let { "qty $it" },
                                            tdr.serialNumber?.takeIf { it.isNotBlank() }?.let { "S/N $it" },
                                        ).joinToString(" · ")
                                        if (meta.isNotEmpty()) {
                                            Text(meta, style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
                                        }
                                    }
                                    IconButton(onClick = { deleteTarget = tdr }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
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

/* ── dialogs ───────────────────────────────────────────────────── */

@Composable
private fun ComponentPickerDialog(
    components: List<ComponentDto>,
    onPick: (ComponentDto) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attach part") },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search IPL / P/N / name…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val q = search.trim().lowercase()
                val shown = if (q.isEmpty()) components else components.filter { c ->
                    listOfNotNull(c.iplNum, c.partNumber, c.name).any { it.lowercase().contains(q) }
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(shown, key = { it.id }) { c ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(c) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(
                                listOfNotNull(c.iplNum, c.partNumber).joinToString(" · "),
                                style = MaterialTheme.typography.bodyMedium, color = AviaDeepSkyBlue,
                            )
                            Text(c.name ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AttachmentFormDialog(
    title: String,
    codes: List<NamedDto>,
    necessaries: List<NamedDto>,
    initial: TdrAttachmentDto?,
    busy: Boolean,
    error: String?,
    onSave: (AttachmentRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    var codeId by remember { mutableStateOf(initial?.codeId) }
    var necessaryId by remember { mutableStateOf(initial?.necessariesId) }
    var qty by remember { mutableStateOf(initial?.qty?.toString() ?: "1") }
    var serial by remember { mutableStateOf(initial?.serialNumber ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = "Code (inspection)",
                    options = codes,
                    selectedId = codeId,
                    required = true,
                    onSelect = { codeId = it },
                )
                DropdownField(
                    label = "Necessary to do",
                    options = necessaries,
                    selectedId = necessaryId,
                    required = false,
                    onSelect = { necessaryId = it },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { v -> qty = v.filter { it.isDigit() }.take(4) },
                        label = { Text("Qty") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = serial,
                        onValueChange = { serial = it },
                        label = { Text("S/N") },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && codeId != null,
                onClick = {
                    onSave(
                        AttachmentRequest(
                            codeId = codeId!!,
                            necessariesId = necessaryId,
                            qty = qty.toIntOrNull(),
                            serialNumber = serial.ifBlank { null },
                        ),
                    )
                },
            ) { Text(if (busy) "Saving…" else "Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DropdownField(
    label: String,
    options: List<NamedDto>,
    selectedId: Int?,
    required: Boolean,
    onSelect: (Int?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.id == selectedId }?.name
        ?: if (required) "— select —" else "—"

    Box {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = AviaTextSecondary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (!required) {
                DropdownMenuItem(text = { Text("—") }, onClick = { onSelect(null); open = false })
            }
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.name ?: "") },
                    onClick = { onSelect(opt.id); open = false },
                )
            }
        }
    }
}

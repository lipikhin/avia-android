package com.aviatechnik.android.ui.screens.workorders

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import com.aviatechnik.android.data.api.WorkorderDetailDto
import com.aviatechnik.android.data.repository.ApiResult
import com.aviatechnik.android.data.repository.WorkorderRepository
import com.aviatechnik.android.ui.theme.AviaDeepSkyBlue
import com.aviatechnik.android.ui.theme.AviaTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Arrival box statuses — fixed server enum (see arrivalBoxStatusLabel). */
private val ARRIVAL_STATUSES = listOf(
    null to "—",
    "ok" to "OK",
    "easy" to "Light repair",
    "medium" to "Medium repair",
    "hard" to "Hard repair",
    "replace" to "New box",
)

data class WorkorderDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val wo: WorkorderDetailDto? = null,
    val busy: Boolean = false,        // a save/upload is in flight
    val actionError: String? = null,  // non-fatal error of the last action
)

@HiltViewModel
class WorkorderDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: WorkorderRepository,
) : ViewModel() {

    private val woId: Int = checkNotNull(savedState["id"]) { "wo id missing" }.toString().toInt()

    val state: StateFlow<WorkorderDetailUiState> get() = _state
    private val _state = MutableStateFlow(WorkorderDetailUiState())

    init { reload() }

    fun reload() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val res = repo.detail(woId)) {
                is ApiResult.Ok -> _state.value = WorkorderDetailUiState(loading = false, wo = res.data.workorder)
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }

    fun saveStorage(rack: Int?, level: Int?, column: Int?, onDone: () -> Unit) = action(onDone) {
        when (val res = repo.updateStorage(woId, rack, level, column)) {
            is ApiResult.Ok -> {
                val old = _state.value.wo
                _state.value = _state.value.copy(
                    wo = old?.copy(storage = res.data.storage.copy(canUpdate = old.storage?.canUpdate ?: false)),
                )
                null
            }
            is ApiResult.Error -> res.message
        }
    }

    fun saveArrivalBox(status: String?, notes: String?, onDone: () -> Unit) = action(onDone) {
        when (val res = repo.updateArrivalBox(woId, status, notes?.ifBlank { null })) {
            is ApiResult.Ok -> {
                _state.value = _state.value.copy(wo = _state.value.wo?.copy(arrivalBox = res.data.arrivalBox))
                null
            }
            is ApiResult.Error -> res.message
        }
    }

    fun uploadPhoto(category: String, file: File) = action(onDone = {}) {
        when (val res = repo.uploadPhoto(woId, category, file)) {
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
fun WorkorderDetailScreen(
    onBack: () -> Unit,
    onOpenTasks: () -> Unit = {},
    onOpenProcesses: () -> Unit = {},
    onOpenComponents: () -> Unit = {},
    vm: WorkorderDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            val wo = state.wo
            Column(Modifier.weight(1f)) {
                Text(
                    "WO ${wo?.numberDisplay ?: ""}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (wo?.isDone == true) AviaTextSecondary else AviaDeepSkyBlue,
                )
                wo?.let {
                    val status = when {
                        it.isDraft -> "draft"
                        it.isDone -> "done ${it.doneAt ?: ""}"
                        else -> "open ${it.openAt ?: ""}"
                    }
                    Text(status, style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
                }
            }
            if (state.busy) CircularProgressIndicator(Modifier.size(22.dp))
        }

        state.actionError?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> DetailBody(state.wo!!, vm, onOpenTasks, onOpenProcesses, onOpenComponents)
        }
    }
}

@Composable
private fun DetailBody(
    wo: WorkorderDetailDto,
    vm: WorkorderDetailViewModel,
    onOpenTasks: () -> Unit = {},
    onOpenProcesses: () -> Unit = {},
    onOpenComponents: () -> Unit = {},
) {
    val context = LocalContext.current

    // Camera hand-off: the system camera writes into our FileProvider cache
    // file. The OS may KILL our process while the camera is open — pending
    // state must survive process death, hence rememberSaveable (plain paths).
    var pendingPath by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCategory by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingPath
        val category = pendingCategory
        pendingPath = null
        if (ok && path != null && category != null) {
            val file = File(path)
            if (file.exists() && file.length() > 0) vm.uploadPhoto(category, file)
        }
    }
    fun launchCamera(category: String) {
        val file = newPhotoFile(context)
        pendingPath = file.absolutePath
        pendingCategory = category
        takePicture.launch(
            FileProvider.getUriForFile(context, "com.aviatechnik.android.fileprovider", file),
        )
    }

    // Full-screen photo viewer (tap a thumbnail); pinch to zoom, drag to pan
    var viewerUrl by remember { mutableStateOf<String?>(null) }
    viewerUrl?.let { url ->
        PhotoViewer(url = url, onClose = { viewerUrl = null })
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenTasks, modifier = Modifier.weight(1f)) { Text("Tasks") }
            OutlinedButton(onClick = onOpenProcesses, modifier = Modifier.weight(1f)) { Text("Processes") }
            OutlinedButton(onClick = onOpenComponents, modifier = Modifier.weight(1f)) { Text("Parts") }
        }

        SectionCard("Info") {
            InfoRow("Customer", wo.customer?.name)
            InfoRow("Customer PO", wo.customerPo)
            InfoRow("P/N", wo.unit?.partNumber)
            InfoRow("Unit", listOfNotNull(wo.unit?.name, wo.unit?.description).joinToString(" — ").ifEmpty { null })
            InfoRow("S/N", wo.serialNumber)
            InfoRow("Description", wo.description)
            InfoRow("Instruction", wo.instruction?.name)
            InfoRow("Owner", wo.owner?.name)
            if (wo.approved) InfoRow("Approved", listOfNotNull(wo.approveName, wo.approveAt).joinToString(" · "))
        }

        StorageSection(wo, vm)
        ArrivalBoxSection(wo, vm)

        // All groups are shown (not only non-empty) so the first photo of a
        // group can be taken right here.
        wo.mediaGroups.forEach { group ->
            SectionCard(
                title = "${group.label ?: group.key} (${group.count})",
                trailing = {
                    IconButton(onClick = { launchCamera(group.key) }) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "Take photo", tint = AviaDeepSkyBlue)
                    }
                },
            ) {
                if (group.media.isEmpty()) {
                    Text("No photos yet", style = MaterialTheme.typography.bodySmall, color = AviaTextSecondary)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(group.media, key = { it.id }) { m ->
                            AsyncImage(
                                model = ApiUrls.rebase(m.thumbUrl),
                                contentDescription = m.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewerUrl = ApiUrls.rebase(m.url ?: m.thumbUrl) },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StorageSection(wo: WorkorderDetailDto, vm: WorkorderDetailViewModel) {
    val s = wo.storage
    var editing by remember { mutableStateOf(false) }
    var rack by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var column by remember { mutableStateOf("") }

    SectionCard(
        title = "Storage",
        trailing = {
            if (s?.canUpdate == true && !editing) {
                OutlinedButton(onClick = {
                    rack = s.rack?.toString() ?: ""
                    level = s.level?.toString() ?: ""
                    column = s.column?.toString() ?: ""
                    editing = true
                }) { Text("Edit") }
            }
        },
    ) {
        if (!editing) {
            InfoRow("Location", s?.location ?: "—")
            InfoRow(
                "Rack / Level / Column",
                listOf(s?.rack, s?.level, s?.column).joinToString(" / ") { it?.toString() ?: "—" },
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Rack", rack, { rack = it }, Modifier.weight(1f))
                NumField("Level", level, { level = it }, Modifier.weight(1f))
                NumField("Column", column, { column = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Button(onClick = {
                    vm.saveStorage(rack.toIntOrNull(), level.toIntOrNull(), column.toIntOrNull()) { editing = false }
                }) { Text("Save") }
                OutlinedButton(onClick = { editing = false }) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun ArrivalBoxSection(wo: WorkorderDetailDto, vm: WorkorderDetailViewModel) {
    val b = wo.arrivalBox
    var editing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    SectionCard(
        title = "Arrival box",
        trailing = {
            if (b?.canUpdate == true && !editing) {
                OutlinedButton(onClick = {
                    status = b.status
                    notes = b.notes ?: ""
                    editing = true
                }) { Text("Edit") }
            }
        },
    ) {
        if (!editing) {
            InfoRow("Status", b?.statusLabel?.ifEmpty { null } ?: "—")
            InfoRow("Notes", b?.notes)
            val recorder = b?.recordedByName ?: b?.recordedBy?.let { "user #$it" }
            InfoRow("Recorded", listOfNotNull(recorder, b?.recordedAt?.take(10)).joinToString(" · ").ifEmpty { null })
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                ARRIVAL_STATUSES.forEach { (value, label) ->
                    FilterChip(
                        selected = status == value,
                        onClick = { status = value },
                        label = { Text(label) },
                    )
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Button(onClick = { vm.saveArrivalBox(status, notes) { editing = false } }) { Text("Save") }
                OutlinedButton(onClick = { editing = false }) { Text("Cancel") }
            }
        }
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
private fun SectionCard(title: String, trailing: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AviaDeepSkyBlue)
                Spacer(Modifier.weight(1f))
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AviaTextSecondary, modifier = Modifier.padding(end = 8.dp))
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun newPhotoFile(context: Context): File {
    val dir = File(context.cacheDir, "photos").apply { mkdirs() }
    return File(dir, "shot_${System.currentTimeMillis()}.jpg")
}

@Composable
private fun PhotoViewer(url: String, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
        Box(
            Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 6f)
                        offset = if (scale > 1f) offset + pan else androidx.compose.ui.geometry.Offset.Zero
                    }
                },
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale, scaleY = scale,
                        translationX = offset.x, translationY = offset.y,
                    ),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}

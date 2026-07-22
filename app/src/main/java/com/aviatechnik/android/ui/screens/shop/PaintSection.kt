package com.aviatechnik.android.ui.screens.shop

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.aviatechnik.android.data.api.ApiUrls
import com.aviatechnik.android.data.api.PaintData
import com.aviatechnik.android.data.api.PaintLostDto
import com.aviatechnik.android.data.api.PaintRowDto
import com.aviatechnik.android.data.repository.ApiResult
import com.aviatechnik.android.data.repository.ShopRepository
import com.aviatechnik.android.data.repository.WorkorderRepository
import com.aviatechnik.android.ui.screens.workorders.DateField
import com.aviatechnik.android.ui.theme.AviaDeepSkyBlue
import com.aviatechnik.android.ui.theme.AviaTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class PaintUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val data: PaintData? = null,
    val busy: Boolean = false,
    val actionError: String? = null,
    val message: String? = null,
)

@HiltViewModel
class PaintViewModel @Inject constructor(
    private val repo: ShopRepository,
    private val woRepo: WorkorderRepository,
) : ViewModel() {

    val state: StateFlow<PaintUiState> get() = _state
    private val _state = MutableStateFlow(PaintUiState())

    init { reload() }

    fun reload() {
        _state.value = _state.value.copy(loading = _state.value.data == null, error = null)
        viewModelScope.launch {
            when (val res = repo.paint()) {
                is ApiResult.Ok -> _state.value = _state.value.copy(loading = false, data = res.data)
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }

    fun setProcessDate(processId: Int, field: String, date: String) = action {
        when (val res = woRepo.updateProcessDate(processId, field, date, source = "paint")) {
            is ApiResult.Ok -> { reload(); null }
            is ApiResult.Error -> res.message
        }
    }

    fun addLost(pn: String, sn: String?, comment: String?, photo: File, onDone: () -> Unit) = action {
        when (val res = repo.addLost(pn, sn, comment, photo)) {
            is ApiResult.Ok -> { reload(); onDone(); null }
            is ApiResult.Error -> res.message
        }
    }

    fun deleteLost(id: Int) = action {
        when (val res = repo.deleteLost(id)) {
            is ApiResult.Ok -> { reload(); null }
            is ApiResult.Error -> res.message
        }
    }

    fun sendMessage(userId: Int, text: String, onDone: () -> Unit) = action {
        when (val res = repo.sendOwnerMessage(userId, text)) {
            is ApiResult.Ok -> {
                _state.value = _state.value.copy(message = "Message sent")
                onDone(); null
            }
            is ApiResult.Error -> res.message
        }
    }

    private fun action(block: suspend () -> String?) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, actionError = null, message = null)
        viewModelScope.launch {
            val err = block()
            _state.value = _state.value.copy(busy = false, actionError = err)
        }
    }
}

@Composable
fun PaintSection(vm: PaintViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var tab by rememberSaveable { mutableStateOf("wo") }
    var hideClosed by rememberSaveable { mutableStateOf(false) }
    var messageTarget by remember { mutableStateOf<PaintRowDto?>(null) }
    var addLostDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<PaintLostDto?>(null) }

    // lost-part photo (camera; pending state survives process death)
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    var shotPath by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val p = pendingPath
        pendingPath = null
        if (ok && p != null && File(p).length() > 0) {
            shotPath = p
            addLostDialog = true
        }
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "lost_${System.currentTimeMillis()}.jpg")
        pendingPath = file.absolutePath
        takePicture.launch(FileProvider.getUriForFile(context, "com.aviatechnik.android.fileprovider", file))
    }

    messageTarget?.let { row ->
        OwnerMessageDialog(
            ownerName = row.owner?.name ?: "",
            busy = state.busy,
            error = state.actionError,
            onSend = { text -> row.owner?.let { vm.sendMessage(it.id, text) { messageTarget = null } } },
            onDismiss = { messageTarget = null },
        )
    }
    if (addLostDialog) {
        AddLostDialog(
            photoPath = shotPath,
            busy = state.busy,
            error = state.actionError,
            onRetake = { addLostDialog = false; launchCamera() },
            onSave = { pn, sn, comment ->
                shotPath?.let { path ->
                    vm.addLost(pn, sn, comment, File(path)) { addLostDialog = false; shotPath = null }
                }
            },
            onDismiss = { addLostDialog = false; shotPath = null },
        )
    }
    deleteTarget?.let { lost ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete lost part?") },
            text = { Text(lost.partNumber ?: "") },
            confirmButton = {
                Button(onClick = { vm.deleteLost(lost.id); deleteTarget = null }) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }

    Column(Modifier.fillMaxSize()) {
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.actionError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        state.message?.let {
            Text(it, color = AviaDeepSkyBlue, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            FilterChip(selected = tab == "wo", onClick = { tab = "wo" }, label = { Text("WO") })
            FilterChip(selected = tab == "lost", onClick = { tab = "lost" }, label = { Text("Lost") })
            if (tab == "wo") {
                Spacer(Modifier.size(6.dp))
                Checkbox(checked = hideClosed, onCheckedChange = { hideClosed = it })
                Text("Hide closed", style = MaterialTheme.typography.bodySmall)
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            tab == "wo" -> {
                val rows = state.data!!.rows.filter { !hideClosed || !it.closed }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(rows, key = { "${it.workorder.id}-${it.editableProcessId ?: 0}-${it.detailLabel.hashCode()}" }) { row ->
                        PaintRow(row, vm) { messageTarget = row }
                    }
                }
            }
            else -> {
                Column(Modifier.fillMaxSize()) {
                    Button(
                        onClick = { launchCamera() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    ) { Text("+ Lost part (photo)") }
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.data!!.lostParts, key = { it.id }) { lost ->
                            LostRow(lost) { deleteTarget = lost }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaintRow(row: PaintRowDto, vm: PaintViewModel, onMessage: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                row.queueDisplay?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, color = AviaDeepSkyBlue)
                    Spacer(Modifier.size(8.dp))
                }
                Text(
                    row.workorder.numberDisplay ?: row.workorder.number.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (row.closed) AviaTextSecondary else AviaDeepSkyBlue,
                )
                Spacer(Modifier.weight(1f))
                if (row.owner != null) {
                    Text(row.owner.name ?: "", style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
                    IconButton(onClick = onMessage, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Message owner",
                            tint = AviaDeepSkyBlue, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(row.detailLabel ?: "—", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DateField("Start", row.startDate, row.editableProcessId != null) { date ->
                    row.editableProcessId?.let { vm.setProcessDate(it, "start", date) }
                }
                DateField("Finish", row.finishDate, row.editableProcessId != null) { date ->
                    row.editableProcessId?.let { vm.setProcessDate(it, "finish", date) }
                }
            }
        }
    }
}

@Composable
private fun LostRow(lost: PaintLostDto, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            lost.photo?.thumbUrl?.let {
                AsyncImage(
                    model = ApiUrls.rebase(it),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)),
                )
                Spacer(Modifier.size(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(lost.partNumber ?: "—", style = MaterialTheme.typography.titleSmall, color = AviaDeepSkyBlue)
                lost.serialNumber?.takeIf { it.isNotBlank() }?.let {
                    Text("S/N $it", style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
                }
                lost.comment?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                lost.owner?.name?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun OwnerMessageDialog(
    ownerName: String,
    busy: Boolean,
    error: String?,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Message to $ownerName") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 1000) text = it },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(enabled = !busy && text.isNotBlank(), onClick = { onSend(text.trim()) }) {
                Text(if (busy) "Sending…" else "Send")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddLostDialog(
    photoPath: String?,
    busy: Boolean,
    error: String?,
    onRetake: () -> Unit,
    onSave: (String, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var pn by rememberSaveable { mutableStateOf("") }
    var sn by rememberSaveable { mutableStateOf("") }
    var comment by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lost part") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                photoPath?.let {
                    AsyncImage(
                        model = File(it),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onRetake),
                    )
                }
                OutlinedTextField(value = pn, onValueChange = { pn = it },
                    label = { Text("Part number *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sn, onValueChange = { sn = it },
                    label = { Text("S/N") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = comment, onValueChange = { comment = it },
                    label = { Text("Comment") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && pn.isNotBlank() && photoPath != null,
                onClick = { onSave(pn.trim(), sn.trim().ifBlank { null }, comment.trim().ifBlank { null }) },
            ) { Text(if (busy) "Saving…" else "Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

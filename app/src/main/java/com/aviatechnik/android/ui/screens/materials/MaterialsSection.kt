package com.aviatechnik.android.ui.screens.materials

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.aviatechnik.android.data.api.MaterialDto
import com.aviatechnik.android.data.repository.ApiResult
import com.aviatechnik.android.data.repository.MaterialsRepository
import com.aviatechnik.android.ui.theme.AviaDeepSkyBlue
import com.aviatechnik.android.ui.theme.AviaTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Materials — parity with mobile web: list + only the description is editable. */
data class MaterialsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<MaterialDto> = emptyList(),
    val search: String = "",
    val busy: Boolean = false,
    val actionError: String? = null,
)

@HiltViewModel
class MaterialsViewModel @Inject constructor(
    private val repo: MaterialsRepository,
) : ViewModel() {

    val state: StateFlow<MaterialsUiState> get() = _state
    private val _state = MutableStateFlow(MaterialsUiState())

    init { reload() }

    fun onSearch(v: String) { _state.value = _state.value.copy(search = v) }

    fun reload() {
        _state.value = _state.value.copy(loading = _state.value.items.isEmpty(), error = null)
        viewModelScope.launch {
            when (val res = repo.list()) {
                is ApiResult.Ok -> _state.value = _state.value.copy(loading = false, items = res.data.items)
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }

    fun saveDescription(id: Int, description: String, onDone: () -> Unit) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, actionError = null)
        viewModelScope.launch {
            when (val res = repo.updateDescription(id, description)) {
                is ApiResult.Ok -> {
                    _state.value = _state.value.copy(
                        busy = false,
                        items = _state.value.items.map { if (it.id == id) res.data.material else it },
                    )
                    onDone()
                }
                is ApiResult.Error -> _state.value = _state.value.copy(busy = false, actionError = res.message)
            }
        }
    }
}

@Composable
fun MaterialsSection(vm: MaterialsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var editing by remember { mutableStateOf<MaterialDto?>(null) }

    editing?.let { m ->
        EditDescriptionDialog(
            material = m,
            busy = state.busy,
            error = state.actionError,
            onSave = { text -> vm.saveDescription(m.id, text) { editing = null } },
            onDismiss = { editing = null },
        )
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.search,
            onValueChange = vm::onSearch,
            placeholder = { Text("Search materials…") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> {
                val q = state.search.trim().lowercase()
                val shown = if (q.isEmpty()) state.items else state.items.filter { m ->
                    listOfNotNull(m.code, m.material, m.specification, m.description)
                        .any { it.lowercase().contains(q) }
                }
                if (shown.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No materials", color = AviaTextSecondary)
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
                    ) {
                        items(shown, key = { it.id }) { m ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { editing = m }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(m.code ?: "—", style = MaterialTheme.typography.titleMedium, color = AviaDeepSkyBlue)
                                    Spacer(Modifier.padding(4.dp))
                                    Text(m.material ?: "", style = MaterialTheme.typography.bodyMedium)
                                }
                                m.specification?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = AviaTextSecondary)
                                }
                                m.description?.takeIf { it.isNotBlank() }?.let {
                                    Spacer(Modifier.height(2.dp))
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditDescriptionDialog(
    material: MaterialDto,
    busy: Boolean,
    error: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(material.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${material.code ?: ""} — description") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(text) }, enabled = !busy) { Text(if (busy) "Saving…" else "Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

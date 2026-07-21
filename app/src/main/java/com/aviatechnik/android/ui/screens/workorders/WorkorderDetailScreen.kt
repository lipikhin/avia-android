package com.aviatechnik.android.ui.screens.workorders

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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
import javax.inject.Inject

data class WorkorderDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val wo: WorkorderDetailDto? = null,
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
}

@Composable
fun WorkorderDetailScreen(
    onBack: () -> Unit,
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
            Column {
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
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> DetailBody(state.wo!!)
        }
    }
}

@Composable
private fun DetailBody(wo: WorkorderDetailDto) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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

        SectionCard("Storage") {
            val s = wo.storage
            InfoRow("Location", s?.location ?: "—")
            InfoRow("Rack / Level / Column",
                listOf(s?.rack, s?.level, s?.column).joinToString(" / ") { it?.toString() ?: "—" })
            if (s?.canUpdate == true) {
                Text("Editing — next iteration", style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
            }
        }

        SectionCard("Arrival box") {
            val b = wo.arrivalBox
            InfoRow("Status", b?.statusLabel?.ifEmpty { null } ?: "—")
            InfoRow("Notes", b?.notes)
            InfoRow("Recorded", listOfNotNull(b?.recordedBy, b?.recordedAt?.take(10)).joinToString(" · ").ifEmpty { null })
        }

        wo.mediaGroups.filter { it.count > 0 }.forEach { group ->
            SectionCard("${group.label ?: group.key} (${group.count})") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(group.media, key = { it.id }) { m ->
                        AsyncImage(
                            model = ApiUrls.rebase(m.thumbUrl),
                            contentDescription = m.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = AviaDeepSkyBlue)
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

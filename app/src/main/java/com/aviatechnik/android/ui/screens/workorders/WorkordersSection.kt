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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aviatechnik.android.data.api.WorkorderItemDto
import com.aviatechnik.android.data.repository.ApiResult
import com.aviatechnik.android.data.repository.WorkorderRepository
import com.aviatechnik.android.ui.theme.AviaDeepSkyBlue
import com.aviatechnik.android.ui.theme.AviaTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Workorders list — parity with mobile web index.blade.php: search filters
 * client-side, "Done" reveals finished WOs (grey), active WOs are cyan.
 * "All" widens the scope from my WOs to everyone's (server-side scope).
 */
data class WorkordersUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<WorkorderItemDto> = emptyList(),
    val search: String = "",
    val showDone: Boolean = false,
    val scopeAll: Boolean = false,
)

@HiltViewModel
class WorkordersViewModel @Inject constructor(
    private val repo: WorkorderRepository,
) : ViewModel() {

    val state: StateFlow<WorkordersUiState> get() = _state
    private val _state = MutableStateFlow(WorkordersUiState())

    init { reload() }

    fun onSearch(v: String) { _state.value = _state.value.copy(search = v) }

    fun onShowDone(v: Boolean) {
        _state.value = _state.value.copy(showDone = v)
        reload()
    }

    fun onScopeAll(v: Boolean) {
        _state.value = _state.value.copy(scopeAll = v)
        reload()
    }

    fun reload() {
        val s = _state.value
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            val scope = if (_state.value.scopeAll) "all" else "my"
            when (val res = repo.list(scope = scope, includeDone = _state.value.showDone)) {
                is ApiResult.Ok -> _state.value = _state.value.copy(loading = false, items = res.data.items)
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }
}

@Composable
fun WorkordersSection(
    onOpenWorkorder: (Int) -> Unit = {},
    vm: WorkordersViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.search,
            onValueChange = vm::onSearch,
            placeholder = { Text("Search WO…") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            FilterChip(
                selected = state.scopeAll,
                onClick = { vm.onScopeAll(!state.scopeAll) },
                label = { Text(if (state.scopeAll) "All WO" else "My WO") },
            )
            Spacer(Modifier.padding(6.dp))
            Checkbox(checked = state.showDone, onCheckedChange = vm::onShowDone)
            Text("Done", style = MaterialTheme.typography.bodyMedium)
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> {
                val q = state.search.trim()
                val shown = if (q.isEmpty()) state.items
                else state.items.filter { (it.numberDisplay ?: it.number.toString()).replace(" ", "").contains(q.replace(" ", "")) }
                if (shown.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No workorders", color = AviaTextSecondary)
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(shown, key = { it.id }) { wo ->
                            WorkorderRow(wo, onClick = { onOpenWorkorder(wo.id) })
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkorderRow(wo: WorkorderItemDto, onClick: () -> Unit) {
    val numberColor = when {
        wo.isDraft -> AviaTextSecondary
        wo.isDone -> AviaTextSecondary
        else -> AviaDeepSkyBlue
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                wo.numberDisplay ?: wo.number.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = numberColor,
            )
            if (wo.isDraft) {
                Text("  draft", style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary)
            }
            Spacer(Modifier.weight(1f))
            wo.openAt?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = AviaTextSecondary) }
        }
        val sub = listOfNotNull(wo.customer?.name, wo.unit?.partNumber).joinToString("  ·  ")
        if (sub.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(sub, style = MaterialTheme.typography.bodySmall, color = AviaTextSecondary)
        }
    }
}

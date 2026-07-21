package com.aviatechnik.android.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aviatechnik.android.data.api.BootstrapData
import com.aviatechnik.android.data.repository.ApiResult
import com.aviatechnik.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Post-login shell stub: proves the token + bootstrap round-trip and shows
 * the server-driven menu modes. Real sections (workorders, tasks, …) land
 * here screen by screen — see the brief's implementation order.
 */
data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val bootstrap: BootstrapData? = null,
    val loggedOut: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> get() = _state
    private val _state = MutableStateFlow(HomeUiState())

    init {
        viewModelScope.launch {
            when (val res = auth.bootstrap()) {
                is ApiResult.Ok -> _state.value = HomeUiState(loading = false, bootstrap = res.data)
                is ApiResult.Error -> _state.value = HomeUiState(loading = false, error = res.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.logout()
            _state.value = _state.value.copy(loggedOut = true)
        }
    }
}

@Composable
fun HomeScreen(
    onLoggedOut: () -> Unit,
    onOpenWorkorder: (Int) -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    if (state.loggedOut) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onLoggedOut() }
    }

    when {
        state.loading -> Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator() }

        state.error != null -> Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onLoggedOut) { Text("Back to login") }
        }

        else -> {
            // Shell: header + current section. v1 opens on Workorders (parity
            // with mobile web); other sections join as they are implemented.
            val b = state.bootstrap!!
            Column(Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Workorders", style = MaterialTheme.typography.titleLarge)
                        Text(
                            b.user.name ?: b.user.email ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.aviatechnik.android.ui.theme.AviaTextSecondary,
                        )
                    }
                    Button(onClick = vm::logout) { Text("Logout") }
                }
                com.aviatechnik.android.ui.screens.workorders.WorkordersSection(
                    onOpenWorkorder = onOpenWorkorder,
                )
            }
        }
    }
}

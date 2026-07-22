package com.aviatechnik.android.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrecisionManufacturing
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
import androidx.compose.runtime.setValue
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
    onOpenProfile: () -> Unit = {},
    onCreateDraft: () -> Unit = {},
    onOpenMachiningWo: (Int, Boolean) -> Unit = { _, _ -> },
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
            // Shell: top menu bar (parity with mobile-menu.blade) + section.
            val b = state.bootstrap!!
            var section by androidx.compose.runtime.saveable.rememberSaveable {
                androidx.compose.runtime.mutableStateOf(
                    when (b.menuMode) {
                        "paint", "machining" -> b.menuMode
                        else -> "workorders"
                    },
                )
            }
            Column(Modifier.fillMaxSize()) {
                val items = buildList {
                    add(
                        com.aviatechnik.android.ui.components.MenuItem(
                            key = "wo", label = "WO",
                            icon = Icons.AutoMirrored.Filled.List,
                            active = section == "workorders",
                            onClick = { section = "workorders" },
                        ),
                    )
                    add(
                        com.aviatechnik.android.ui.components.MenuItem(
                            key = "materials", label = "Materials",
                            icon = Icons.Filled.Category,
                            active = section == "materials",
                            onClick = { section = "materials" },
                        ),
                    )
                    if (b.user.capabilities["can_use_paint"] == true) {
                        add(
                            com.aviatechnik.android.ui.components.MenuItem(
                                key = "paint", label = "Paint",
                                icon = Icons.Filled.FormatPaint,
                                active = section == "paint",
                                onClick = { section = "paint" },
                            ),
                        )
                    }
                    if (b.user.capabilities["can_use_machining"] == true) {
                        add(
                            com.aviatechnik.android.ui.components.MenuItem(
                                key = "machining", label = "Machining",
                                icon = Icons.Filled.PrecisionManufacturing,
                                active = section == "machining",
                                onClick = { section = "machining" },
                            ),
                        )
                    }
                    if (b.user.capabilities["can_create_draft"] == true) {
                        add(
                            com.aviatechnik.android.ui.components.MenuItem(
                                key = "drafts", label = "Drafts",
                                icon = Icons.Filled.Description,
                                active = section == "drafts",
                                onClick = { section = "drafts" },
                            ),
                        )
                    }
                    add(
                        com.aviatechnik.android.ui.components.MenuItem(
                            key = "profile",
                            label = b.user.name?.split(" ")?.firstOrNull() ?: "Profile",
                            icon = Icons.Filled.Person,
                            onClick = onOpenProfile,
                        ),
                    )
                    add(
                        com.aviatechnik.android.ui.components.MenuItem(
                            key = "logout", label = "Logout",
                            icon = Icons.AutoMirrored.Filled.Logout,
                            onClick = vm::logout,
                        ),
                    )
                }
                com.aviatechnik.android.ui.components.MobileMenuBar(items)

                when (section) {
                    "materials" -> com.aviatechnik.android.ui.screens.materials.MaterialsSection()
                    "paint" -> com.aviatechnik.android.ui.screens.shop.PaintSection()
                    "machining" -> com.aviatechnik.android.ui.screens.shop.MachiningSection(
                        onOpen = onOpenMachiningWo,
                    )
                    "drafts" -> com.aviatechnik.android.ui.screens.drafts.DraftsSection(
                        onOpenWorkorder = onOpenWorkorder,
                        onCreateDraft = onCreateDraft,
                    )
                    else -> com.aviatechnik.android.ui.screens.workorders.WorkordersSection(
                        onOpenWorkorder = onOpenWorkorder,
                    )
                }
            }
        }
    }
}

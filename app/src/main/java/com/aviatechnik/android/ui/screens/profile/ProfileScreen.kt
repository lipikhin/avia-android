package com.aviatechnik.android.ui.screens.profile

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.aviatechnik.android.data.api.ApiUrls
import com.aviatechnik.android.data.api.NamedDto
import com.aviatechnik.android.data.api.ProfileDto
import com.aviatechnik.android.data.api.ProfileUpdateRequest
import com.aviatechnik.android.data.repository.ApiResult
import com.aviatechnik.android.data.repository.ProfileRepository
import com.aviatechnik.android.ui.theme.AviaDeepSkyBlue
import com.aviatechnik.android.ui.theme.AviaTextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: ProfileDto? = null,
    val teams: List<NamedDto> = emptyList(),
    val busy: Boolean = false,
    val actionError: String? = null,
    val message: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: ProfileRepository,
) : ViewModel() {

    val state: StateFlow<ProfileUiState> get() = _state
    private val _state = MutableStateFlow(ProfileUiState())

    /** Avatar picked by camera, uploaded together with Save. */
    var pendingAvatar: File? = null

    init { reload() }

    fun reload() {
        _state.value = _state.value.copy(loading = _state.value.profile == null, error = null)
        viewModelScope.launch {
            when (val res = repo.load()) {
                is ApiResult.Ok -> _state.value = _state.value.copy(
                    loading = false, profile = res.data.profile, teams = res.data.teams,
                )
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }

    fun save(req: ProfileUpdateRequest, onDone: () -> Unit) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, actionError = null, message = null)
        viewModelScope.launch {
            when (val res = repo.update(req, pendingAvatar)) {
                is ApiResult.Ok -> {
                    pendingAvatar = null
                    _state.value = _state.value.copy(busy = false, profile = res.data.profile, message = "Saved")
                    onDone()
                }
                is ApiResult.Error -> _state.value = _state.value.copy(busy = false, actionError = res.message)
            }
        }
    }

    fun changePassword(old: String, new: String, confirm: String, onDone: () -> Unit) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, actionError = null, message = null)
        viewModelScope.launch {
            when (val res = repo.changePassword(old, new, confirm)) {
                is ApiResult.Ok -> {
                    _state.value = _state.value.copy(busy = false, message = "New password saved")
                    onDone()
                }
                is ApiResult.Error -> _state.value = _state.value.copy(busy = false, actionError = res.message)
            }
        }
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // editable fields — initialized from the loaded profile
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var birthday by rememberSaveable { mutableStateOf("") }
    var stamp by rememberSaveable { mutableStateOf("") }
    var teamId by rememberSaveable { mutableStateOf<Int?>(null) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var avatarPreview by remember { mutableStateOf<String?>(null) }
    var passwordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.profile) {
        val p = state.profile ?: return@LaunchedEffect
        if (!initialized) {
            name = p.name ?: ""
            phone = p.phone ?: ""
            birthday = p.birthday ?: ""
            stamp = p.stamp ?: ""
            teamId = p.team?.id
            initialized = true
        }
    }

    // avatar camera
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingPath
        pendingPath = null
        if (ok && path != null) {
            val f = File(path)
            if (f.exists() && f.length() > 0) {
                vm.pendingAvatar = f
                avatarPreview = path
            }
        }
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
        pendingPath = file.absolutePath
        takePicture.launch(FileProvider.getUriForFile(context, "com.aviatechnik.android.fileprovider", file))
    }

    if (passwordDialog) {
        PasswordDialog(
            busy = state.busy,
            error = state.actionError,
            onSave = { old, new, confirm -> vm.changePassword(old, new, confirm) { passwordDialog = false } },
            onDismiss = { passwordDialog = false },
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
            Text("Profile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (state.busy) CircularProgressIndicator(Modifier.size(22.dp))
        }
        state.actionError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        state.message?.let {
            Text(it, color = AviaDeepSkyBlue, style = MaterialTheme.typography.bodySmall,
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
                val p = state.profile!!
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // avatar
                    Box(Modifier.align(Alignment.CenterHorizontally)) {
                        val model = avatarPreview ?: p.avatar?.thumbUrl?.let { ApiUrls.rebase(it) }
                        if (model != null) {
                            AsyncImage(
                                model = model,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(96.dp).clip(CircleShape).clickable { launchCamera() },
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person, contentDescription = "Avatar",
                                tint = AviaTextSecondary,
                                modifier = Modifier.size(96.dp).clip(CircleShape).clickable { launchCamera() },
                            )
                        }
                        Icon(
                            Icons.Filled.PhotoCamera, contentDescription = null, tint = AviaDeepSkyBlue,
                            modifier = Modifier.align(Alignment.BottomEnd).size(22.dp),
                        )
                    }
                    Text(
                        p.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AviaTextSecondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = name, onValueChange = { name = it },
                                label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = phone, onValueChange = { phone = it },
                                label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = birthday, onValueChange = { birthday = it },
                                label = { Text("Birthday (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = stamp, onValueChange = { stamp = it },
                                label = { Text("Stamp") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            TeamDropdown(teams = state.teams, selectedId = teamId, onSelect = { teamId = it })

                            Button(
                                enabled = !state.busy && name.isNotBlank() && stamp.isNotBlank() && teamId != null,
                                onClick = {
                                    vm.save(
                                        ProfileUpdateRequest(
                                            name = name.trim(),
                                            phone = phone.trim().ifBlank { null },
                                            birthday = birthday.trim().ifBlank { null },
                                            stamp = stamp.trim(),
                                            teamId = teamId!!,
                                        ),
                                    ) { avatarPreview = null }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(if (state.busy) "Saving…" else "Save") }
                        }
                    }

                    OutlinedButton(onClick = { passwordDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Change password")
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TeamDropdown(teams: List<NamedDto>, selectedId: Int?, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selectedName = teams.firstOrNull { it.id == selectedId }?.name ?: "— select team —"

    Box {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Team") },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = AviaTextSecondary,
            ),
            modifier = Modifier.fillMaxWidth().clickable { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            teams.forEach { t ->
                DropdownMenuItem(text = { Text(t.name ?: "") }, onClick = { onSelect(t.id); open = false })
            }
        }
    }
}

@Composable
private fun PasswordDialog(
    busy: Boolean,
    error: String?,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = old, onValueChange = { old = it },
                    label = { Text("Current password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = new, onValueChange = { new = it },
                    label = { Text("New password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = confirm, onValueChange = { confirm = it },
                    label = { Text("Confirm new password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                if (new.isNotEmpty() && confirm.isNotEmpty() && new != confirm) {
                    Text("Passwords do not match", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && old.isNotEmpty() && new.isNotEmpty() && new == confirm,
                onClick = { onSave(old, new, confirm) },
            ) { Text(if (busy) "Saving…" else "Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

package com.aviatechnik.android.ui.screens.login

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aviatechnik.android.data.repository.ApiResult
import com.aviatechnik.android.data.repository.AuthRepository
import com.aviatechnik.android.ui.components.FrontShell
import com.aviatechnik.android.ui.components.assetUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    val state: StateFlow<LoginUiState> get() = _state
    private val _state = MutableStateFlow(LoginUiState())

    fun onEmail(v: String) = _state.value.let { _state.value = it.copy(email = v, error = null) }
    fun onPassword(v: String) = _state.value.let { _state.value = it.copy(password = v, error = null) }
    fun onRememberMe(v: Boolean) = _state.value.let { _state.value = it.copy(rememberMe = v) }

    fun submit() {
        val s = _state.value
        if (s.loading || s.email.isBlank() || s.password.isBlank()) return
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val res = auth.login(s.email.trim(), s.password, s.rememberMe)) {
                is ApiResult.Ok -> _state.value = _state.value.copy(loading = false, done = true)
                is ApiResult.Error -> _state.value = _state.value.copy(loading = false, error = res.message)
            }
        }
    }
}

private val InkText = Color(0xFF212529)
private val FieldBorder = Color(0xFFCED4DA)
private val LinkBlue = Color(0xFF0D6EFD)

/** Web-parity login: white modal card over the NODUS front background. */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    vm: LoginViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    if (state.done) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onLoggedIn() }
    }

    // web flow: title page -> hamburger reveals Login -> white modal card
    var menuOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var showCard by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    FrontShell(
        onHamburger = { menuOpen = !menuOpen },
        underBar = if (menuOpen && !showCard) {
            { com.aviatechnik.android.ui.components.FrontLoginLink { showCard = true; menuOpen = false } }
        } else null,
    ) {
        if (showCard) Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp, start = 14.dp, end = 14.dp)
                .verticalScroll(rememberScrollState())
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Login", color = InkText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    "✕",
                    color = InkText,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { showCard = false }
                        .padding(4.dp),
                )
            }
            androidx.compose.material3.HorizontalDivider(color = Color(0xFFE9ECEF))

            Text("Email Address", color = InkText, fontSize = 13.sp)
            LightField(
                value = state.email,
                onChange = vm::onEmail,
                password = false,
            )

            Text("Password", color = InkText, fontSize = 13.sp)
            LightField(
                value = state.password,
                onChange = vm::onPassword,
                password = true,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.rememberMe,
                    onCheckedChange = vm::onRememberMe,
                    colors = CheckboxDefaults.colors(
                        checkedColor = LinkBlue,
                        uncheckedColor = FieldBorder,
                        checkmarkColor = Color.White,
                    ),
                )
                Text("Remember Me", color = InkText, fontSize = 13.sp)
            }

            state.error?.let {
                Text(it, color = Color(0xFFDC3545), fontSize = 12.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = vm::submit,
                    enabled = !state.loading,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LinkBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), color = Color.White)
                    } else {
                        Text("Login")
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "Forgot Your Password?",
                    color = LinkBlue,
                    fontSize = 13.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        // web handoff, as the brief dictates
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(assetUrl("password/reset"))),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun LightField(value: String, onChange: (String) -> Unit, password: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation()
        else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor = InkText,
            unfocusedTextColor = InkText,
            cursorColor = LinkBlue,
            focusedBorderColor = Color(0xFF86B7FE),
            unfocusedBorderColor = FieldBorder,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    )
}

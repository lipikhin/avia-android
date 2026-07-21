package com.aviatechnik.android.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aviatechnik.android.data.repository.AuthRepository
import com.aviatechnik.android.ui.theme.AviaBlue
import com.aviatechnik.android.ui.theme.AviaDeepSkyBlue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    /** null = still deciding; true/false = has a usable session or not. */
    val hasSession: StateFlow<Boolean?> get() = _hasSession
    private val _hasSession = MutableStateFlow<Boolean?>(null)

    init {
        viewModelScope.launch {
            // App config is fetched for branding metadata; login screen falls
            // back to built-in defaults when the network is unavailable.
            auth.appConfig()
            _hasSession.value = auth.hasPersistedToken()
        }
    }
}

@Composable
fun SplashScreen(
    onReady: (hasSession: Boolean) -> Unit,
    vm: SplashViewModel = hiltViewModel(),
) {
    val hasSession by vm.hasSession.collectAsState()

    LaunchedEffect(hasSession) {
        hasSession?.let(onReady)
    }

    // Title page — parity with the web front page (black bar + NODUS hero)
    com.aviatechnik.android.ui.components.FrontShell(
        onHamburger = null,
    ) {
        CircularProgressIndicator(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

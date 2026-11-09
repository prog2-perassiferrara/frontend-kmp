package com.prog2.frontend

import androidx.compose.material.MaterialTheme
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog2.frontend.registration.data.RegistrationApi
import com.prog2.frontend.session.SessionManager
import com.prog2.frontend.session.SessionStatus
import com.prog2.frontend.ui.registration.RegistrationScreen
import com.prog2.frontend.ui.registration.RegistrationViewModel
import com.prog2.frontend.ui.session.AuthenticatedScreen
import com.prog2.frontend.ui.session.LoginScreen
import com.prog2.frontend.ui.session.SessionViewModel

/**
 * Conecta el estado de presentación con la pantalla, dentro del tema de la app.
 *
 * @param sessions Coordinador compartido durante la vida del proceso.
 * @param registrationApi Alta pública usada por el ViewModel de registro.
 */
class App(
    private val sessions: SessionManager,
    private val registrationApi: RegistrationApi,
    private val loginScreen: LoginScreen,
    private val registrationScreen: RegistrationScreen,
    private val authenticatedScreen: AuthenticatedScreen,
) {
    /** Obtiene los ViewModels del owner Android, conservándolos durante la rotación. */
    @Composable
    fun Content() {
        val sessionViewModel = viewModel { SessionViewModel(sessions) }
        val registrationViewModel = viewModel { RegistrationViewModel(registrationApi, sessions) }
        val state by sessionViewModel.state
        var showRegistration by rememberSaveable { mutableStateOf(false) }

        // Al autenticarse, el registro deja de estar visible para que un logout
        // posterior vuelva directamente al login.
        LaunchedEffect(state.session.status) {
            if (state.session.status == SessionStatus.SIGNED_IN) showRegistration = false
        }

        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier.widthIn(max = 480.dp).fillMaxWidth()
                            .verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when (state.session.status) {
                            SessionStatus.RESTORING -> {
                                CircularProgressIndicator()
                                Text("Restaurando sesión…")
                            }
                            SessionStatus.SIGNED_IN -> authenticatedScreen.Content(sessionViewModel::signOut)
                            SessionStatus.SIGNED_OUT -> if (showRegistration) {
                                registrationScreen.Content(
                                    registrationViewModel.state.value,
                                    registrationViewModel::changeLogin,
                                    registrationViewModel::changePassword,
                                    registrationViewModel::changeFirstName,
                                    registrationViewModel::changeLastName,
                                    registrationViewModel::changeEmail,
                                    registrationViewModel::submit,
                                    onBackToLogin = {
                                        registrationViewModel.reset()
                                        showRegistration = false
                                    },
                                )
                            } else {
                                loginScreen.Content(
                                    state, sessionViewModel::changeUsername, sessionViewModel::changePassword,
                                    sessionViewModel::changeRememberMe, sessionViewModel::signIn,
                                    onRegister = {
                                        registrationViewModel.reset()
                                        showRegistration = true
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

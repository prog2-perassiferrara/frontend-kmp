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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog2.frontend.session.SessionManager
import com.prog2.frontend.session.SessionStatus
import com.prog2.frontend.ui.session.AuthenticatedScreen
import com.prog2.frontend.ui.session.LoginScreen
import com.prog2.frontend.ui.session.SessionViewModel

/**
 * Conecta el estado de presentación con la pantalla, dentro del tema de la app.
 *
 * @param sessions Coordinador compartido durante la vida del proceso.
 */
class App(
    private val sessions: SessionManager,
    private val loginScreen: LoginScreen,
    private val authenticatedScreen: AuthenticatedScreen,
) {
    /** Obtiene el ViewModel del owner Android, conservándolo durante la rotación. */
    @Composable
    fun Content() {
        val sessionViewModel = viewModel { SessionViewModel(sessions) }
        val state by sessionViewModel.state
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
                            SessionStatus.SIGNED_OUT -> loginScreen.Content(
                                state, sessionViewModel::changeUsername, sessionViewModel::changePassword,
                                sessionViewModel::changeRememberMe, sessionViewModel::signIn,
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.prog2.frontend.ui.session

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionException
import com.prog2.frontend.session.SessionManager
import com.prog2.frontend.session.SessionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Procesa acciones de UI y conserva el formulario solo mientras viva el ViewModel.
 * La sesión compartida pertenece al coordinador, no a la Activity.
 */
class SessionViewModel(private val sessions: SessionManager) : ViewModel() {
    private val mutableState = mutableStateOf(SessionUiState(session = sessions.state.value))
    private var loginJob: Job? = null
    val state: State<SessionUiState> = mutableState

    init {
        viewModelScope.launch {
            sessions.state.collect { session ->
                updateState {
                    if (it.session.epoch != session.epoch) SessionUiState(session = session)
                    else it.copy(session = session)
                }
            }
        }
    }

    fun changeUsername(value: String) {
        if (canEdit()) updateState { it.copy(username = value, usernameError = null, error = null) }
    }

    fun changePassword(value: String) {
        if (canEdit()) updateState { it.copy(password = value, passwordError = null, error = null) }
    }

    fun changeRememberMe(value: Boolean) {
        if (canEdit()) updateState { it.copy(rememberMe = value) }
    }

    /** Valida el formulario y limita el login a un único intento vigente. */
    fun signIn() {
        if (!canEdit() || loginJob?.isActive == true) return
        val form = state.value
        val username = form.username.lowercase()
        val usernameError = when {
            username.isBlank() -> "Ingresá tu usuario o correo."
            username.length > 254 -> "El usuario o correo admite hasta 254 caracteres."
            else -> null
        }
        val passwordError = if (form.password.codePointCount() in 6..100) null else "La contraseña debe tener entre 6 y 100 caracteres."
        updateState { it.copy(usernameError = usernameError, passwordError = passwordError, error = null) }
        if (usernameError != null || passwordError != null) return
        updateState { it.copy(isSubmitting = true) }
        loginJob = viewModelScope.launch {
            try {
                sessions.signIn(username, form.password, form.rememberMe)
            } catch (failure: SessionException) {
                if (failure.error != SessionError.SESSION_CHANGED) {
                    updateState { it.copy(error = failure.error) }
                }
            } finally {
                updateState { it.copy(isSubmitting = false) }
            }
        }
    }

    fun signOut() {
        loginJob?.cancel()
        viewModelScope.launch { sessions.signOut() }
    }

    // Las acciones y viewModelScope se ejecutan en Main. La edición se refleja sin
    // un salto de colección adicional entre el IME y el estado consumido por Compose.
    private fun updateState(transform: (SessionUiState) -> SessionUiState) {
        mutableState.value = transform(mutableState.value)
    }

    private fun canEdit(): Boolean = state.value.let {
        it.session.status == SessionStatus.SIGNED_OUT && !it.isSubmitting
    }

    // Cuenta puntos de código, como la validación del productor: un par sustituto
    // Unicode representa un carácter y no dos.
    private fun String.codePointCount(): Int = count { !it.isLowSurrogate() }
}

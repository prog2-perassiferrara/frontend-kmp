package com.prog2.frontend.ui.registration

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prog2.frontend.registration.RegistrationError
import com.prog2.frontend.registration.RegistrationException
import com.prog2.frontend.registration.data.NewAccount
import com.prog2.frontend.registration.data.RegistrationApi
import com.prog2.frontend.session.SessionManager
import com.prog2.frontend.session.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Valida y envía el alta y reutiliza la sesión existente para el login automático.
 * La contraseña permanece solo en memoria mientras se edita.
 */
class RegistrationViewModel(
    private val registrationApi: RegistrationApi,
    private val sessions: SessionManager,
) : ViewModel() {
    private val mutableState = mutableStateOf(RegistrationUiState())
    val state: State<RegistrationUiState> = mutableState

    fun changeLogin(value: String) = edit {
        it.copy(login = value, loginError = null, formError = null)
    }

    fun changePassword(value: String) = edit {
        it.copy(password = value, passwordError = null, formError = null)
    }

    fun changeFirstName(value: String) = edit {
        it.copy(firstName = value, firstNameError = null, formError = null)
    }

    fun changeLastName(value: String) = edit {
        it.copy(lastName = value, lastNameError = null, formError = null)
    }

    fun changeEmail(value: String) = edit {
        it.copy(email = value, emailError = null, formError = null)
    }

    /** Descarta el formulario al salir; nunca conserva la contraseña. */
    fun reset() {
        if (state.value.isSubmitting) return
        mutableState.value = RegistrationUiState()
    }

    /** Valida el formulario y limita el alta a un único intento vigente. */
    fun submit() {
        val form = state.value
        if (form.isSubmitting || form.accountCreated) return
        val login = form.login.lowercase()
        val email = form.email.lowercase()
        val loginError = when {
            login.isBlank() -> "Ingresá un usuario."
            login.length > 50 -> "El usuario admite hasta 50 caracteres."
            !LOGIN_PATTERN.matches(login) -> "El usuario no es válido."
            else -> null
        }
        val passwordError = if (form.password.codePointCount() in 6..100) null
        else "La contraseña debe tener entre 6 y 100 caracteres."
        val firstNameError = nameError(form.firstName, "nombre")
        val lastNameError = nameError(form.lastName, "apellido")
        // El formato completo lo valida @Email en Turnos. Aquí solo se descartan
        // entradas sin una separación local/dominio reconocible, para no rechazar
        // correos Unicode o sin punto que el backend sí admite.
        val at = email.lastIndexOf('@')
        val emailError = when {
            email.isBlank() -> "Ingresá tu correo."
            email.length !in 5..254 -> "El correo debe tener entre 5 y 254 caracteres."
            at <= 0 || at == email.lastIndex -> "Ingresá un correo válido."
            else -> null
        }
        updateState {
            it.copy(
                loginError = loginError, passwordError = passwordError,
                firstNameError = firstNameError, lastNameError = lastNameError,
                emailError = emailError, formError = null,
            )
        }
        if (listOf(loginError, passwordError, firstNameError, lastNameError, emailError).any { it != null }) {
            return
        }
        updateState { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                registrationApi.register(
                    NewAccount(login, form.password, form.firstName, form.lastName, email),
                )
                signInAfterRegistration(login, form.password)
            } catch (failure: RegistrationException) {
                showFailure(failure)
            } finally {
                updateState { it.copy(isSubmitting = false) }
            }
        }
    }

    private suspend fun signInAfterRegistration(login: String, password: String) {
        try {
            sessions.signIn(login, password, rememberMe = false)
            updateState { RegistrationUiState() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (sessions.state.value.status == SessionStatus.SIGNED_IN) {
                updateState { RegistrationUiState() }
            } else {
                // La cuenta ya existe: no se repite el alta aunque el login falle.
                updateState { it.copy(password = "", accountCreated = true, formError = null) }
            }
        }
    }

    private fun showFailure(failure: RegistrationException) {
        when (failure.error) {
            RegistrationError.INVALID_INPUT -> updateState {
                val fields = failure.fieldErrors
                val knownField = FIELD_NAMES.any { it in fields }
                it.copy(
                    loginError = fieldError("login", fields) ?: it.loginError,
                    passwordError = fieldError("password", fields) ?: it.passwordError,
                    firstNameError = fieldError("firstName", fields) ?: it.firstNameError,
                    lastNameError = fieldError("lastName", fields) ?: it.lastNameError,
                    emailError = fieldError("email", fields) ?: it.emailError,
                    formError = if (knownField) null else failure.error.message,
                )
            }
            RegistrationError.IDENTIFIER_ALREADY_USED -> updateState {
                it.copy(formError = failure.error.message)
            }
            RegistrationError.UNCERTAIN -> updateState {
                // El resultado no es verificable; la contraseña no se conserva.
                it.copy(password = "", formError = failure.error.message)
            }
        }
    }

    // Las acciones y viewModelScope se ejecutan en Main. La edición se refleja sin
    // un salto de colección adicional entre el IME y el estado consumido por Compose.
    private fun updateState(transform: (RegistrationUiState) -> RegistrationUiState) {
        mutableState.value = transform(mutableState.value)
    }

    private fun edit(transform: (RegistrationUiState) -> RegistrationUiState) {
        val current = state.value
        if (current.isSubmitting || current.accountCreated) return
        updateState(transform)
    }

    private fun nameError(value: String, label: String): String? = when {
        value.isBlank() -> "Ingresá tu $label."
        value.length !in 2..50 -> "El $label debe tener entre 2 y 50 caracteres."
        else -> null
    }

    private fun fieldError(field: String, fields: Set<String>): String? =
        if (field !in fields) null else when (field) {
            "login" -> "El usuario no es válido."
            "password" -> "La contraseña debe tener entre 6 y 100 caracteres."
            "firstName" -> "El nombre debe tener entre 2 y 50 caracteres."
            "lastName" -> "El apellido debe tener entre 2 y 50 caracteres."
            "email" -> "El correo no es válido."
            else -> null
        }

    // Cuenta puntos de código, como la validación del productor: un par sustituto
    // Unicode representa un carácter y no dos.
    private fun String.codePointCount(): Int = count { !it.isLowSurrogate() }

    private companion object {
        // Equivalente sin grupos atómicos al patrón LOGIN_REGEX de JHipster; el
        // backend normaliza a minúsculas antes de validar.
        val LOGIN_PATTERN = Regex(
            "^(?:[A-Za-z0-9!$&*+=?^_`{|}~.-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*|[_.@A-Za-z0-9-]+)$",
        )
        val FIELD_NAMES = setOf("login", "password", "firstName", "lastName", "email")
    }
}

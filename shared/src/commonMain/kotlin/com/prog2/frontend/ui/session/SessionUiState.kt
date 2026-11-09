package com.prog2.frontend.ui.session

import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionState

/** Estado temporal de presentación. La contraseña no se persiste ni se imprime. */
data class SessionUiState(
    val session: SessionState = SessionState(),
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isSubmitting: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val error: SessionError? = null,
) {
    override fun toString(): String = "SessionUiState(status=${session.status}, credentials=REDACTED)"
}

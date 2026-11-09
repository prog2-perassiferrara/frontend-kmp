package com.prog2.frontend.ui.registration

/** Estado temporal del formulario; la contraseña no se persiste ni se imprime. */
data class RegistrationUiState(
    val login: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val isSubmitting: Boolean = false,
    val accountCreated: Boolean = false,
    val loginError: String? = null,
    val passwordError: String? = null,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val formError: String? = null,
) {
    override fun toString(): String = "RegistrationUiState(accountCreated=$accountCreated, credentials=REDACTED)"
}

package com.prog2.frontend.ui.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Presenta el formulario de alta; toda validación y operación pertenece al ViewModel. */
class RegistrationScreen {
    @Composable
    fun Content(
        state: RegistrationUiState,
        onLogin: (String) -> Unit,
        onPassword: (String) -> Unit,
        onFirstName: (String) -> Unit,
        onLastName: (String) -> Unit,
        onEmail: (String) -> Unit,
        onSubmit: () -> Unit,
        onBackToLogin: () -> Unit,
    ) {
        val enabled = !state.isSubmitting && !state.accountCreated
        Text("Crear cuenta", style = MaterialTheme.typography.h5)
        OutlinedTextField(
            value = state.login,
            onValueChange = onLogin,
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = state.loginError != null,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
        )
        state.loginError?.let { FieldError(it) }
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmail,
            label = { Text("Correo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = state.emailError != null,
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )
        state.emailError?.let { FieldError(it) }
        OutlinedTextField(
            value = state.firstName,
            onValueChange = onFirstName,
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = state.firstNameError != null,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
        )
        state.firstNameError?.let { FieldError(it) }
        OutlinedTextField(
            value = state.lastName,
            onValueChange = onLastName,
            label = { Text("Apellido") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = state.lastNameError != null,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
        )
        state.lastNameError?.let { FieldError(it) }
        OutlinedTextField(
            value = state.password,
            onValueChange = onPassword,
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = enabled,
            isError = state.passwordError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (enabled) onSubmit() }),
        )
        state.passwordError?.let { FieldError(it) }
        state.formError?.let { FieldError(it) }
        if (state.accountCreated) {
            Text(
                "Cuenta creada. No pudimos iniciar sesión automáticamente; volvé a ingresar con tus credenciales.",
                style = MaterialTheme.typography.body2,
            )
        }
        if (!state.accountCreated) {
            Button(onClick = onSubmit, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isSubmitting) "Creando cuenta…" else "Crear cuenta")
            }
            if (state.isSubmitting) CircularProgressIndicator()
        }
        TextButton(
            onClick = onBackToLogin,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.accountCreated) "Ir a iniciar sesión" else "Ya tengo cuenta")
        }
    }
}

@Composable
private fun FieldError(message: String) {
    Text(
        message,
        color = MaterialTheme.colors.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Preview
@Composable
private fun RegistrationScreenPreview() {
    MaterialTheme {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RegistrationScreen().Content(
                RegistrationUiState(login = "patient", email = "patient@example.test"),
                onLogin = {}, onPassword = {}, onFirstName = {}, onLastName = {},
                onEmail = {}, onSubmit = {}, onBackToLogin = {},
            )
        }
    }
}

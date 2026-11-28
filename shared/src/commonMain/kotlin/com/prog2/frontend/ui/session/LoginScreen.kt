package com.prog2.frontend.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.prog2.frontend.ui.components.FieldError
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.prog2.frontend.session.SessionState
import com.prog2.frontend.session.SessionStatus

/** Presenta el formulario recibido; toda validación y operación pertenece al ViewModel. */
class LoginScreen {
    @Composable
    fun Content(
        state: SessionUiState,
        onUsername: (String) -> Unit,
        onPassword: (String) -> Unit,
        onRememberMe: (Boolean) -> Unit,
        onSubmit: () -> Unit,
        onRegister: () -> Unit,
    ) {
        val enabled = !state.isSubmitting
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = state.username,
            onValueChange = onUsername,
            label = { Text("Usuario o correo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = state.usernameError != null,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
        )
        state.usernameError?.let { FieldError(it) }
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
        Row(
            modifier = Modifier.fillMaxWidth()
                .toggleable(state.rememberMe, enabled = enabled, role = Role.Checkbox, onValueChange = onRememberMe)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = state.rememberMe, onCheckedChange = null, enabled = enabled)
            Text("Recordarme", modifier = Modifier.padding(start = 8.dp))
        }
        Text("Recordarme amplía la sesión de 24 horas a 30 días.", style = MaterialTheme.typography.bodyMedium)
        (state.error ?: state.session.error)?.let { FieldError(it.message) }
        Button(onClick = onSubmit, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isSubmitting) "Iniciando sesión…" else "Iniciar sesión")
        }
        if (state.isSubmitting) CircularProgressIndicator()
        TextButton(onClick = onRegister, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("Crear cuenta")
        }
    }
}

package com.prog2.frontend

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog2.frontend.ui.setup.SetupScreen
import com.prog2.frontend.ui.setup.SetupViewModel

/**
 * Conecta el estado de presentación con la pantalla, dentro del tema de la app.
 *
 * @param setupScreen Vista que recibe el estado y las acciones de la demo.
 */
class App(private val setupScreen: SetupScreen) {
    /** Obtiene el ViewModel del owner Android, conservándolo durante la rotación. */
    @Composable
    fun Content() {
        val setupViewModel = viewModel { SetupViewModel() }
        MaterialTheme {
            setupScreen.Content(
                isContentVisible = setupViewModel.isContentVisible.value,
                onToggleContent = setupViewModel::toggleContent,
            )
        }
    }
}

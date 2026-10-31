package com.prog2.frontend.ui.setup

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

/**
 * Conserva el estado temporal de la demo y procesa sus acciones de presentación.
 * Sobrevive a cambios de configuración; un nuevo proceso comienza con el contenido oculto.
 */
class SetupViewModel : ViewModel() {
    private val contentVisible = mutableStateOf(false)

    /** Estado observable de solo lectura para los consumidores de la presentación. */
    val isContentVisible: State<Boolean> = contentVisible

    /** Alterna la visibilidad ante una acción de la UI, ejecutada en su hilo principal. */
    fun toggleContent() {
        contentVisible.value = !contentVisible.value
    }
}

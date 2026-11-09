package com.prog2.frontend.ui.components

import androidx.compose.runtime.Composable

/**
 * Selector de fecha nativo de la plataforma.
 *
 * Se compone solo mientras debe mostrarse. Al elegir una fecha informa sus
 * componentes; cancelar invoca [onDismiss] sin modificar la fecha.
 *
 * @param initialDate fecha inicial en formato `AAAA-MM-DD`; si está vacía o no
 *   es válida se usa la fecha actual.
 * @param allowPastDates permite elegir días anteriores a hoy. La disponibilidad
 *   de un profesional exige desde hoy; el filtro de búsqueda no lo requiere.
 * @param onDateSelected año, mes (1-12) y día elegidos.
 * @param onDismiss cancelación del diálogo.
 */
@Composable
expect fun DatePickerModal(
    initialDate: String,
    allowPastDates: Boolean = false,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit,
)

/** Convierte los componentes elegidos al formato `AAAA-MM-DD` que usa el contrato. */
internal fun isoDate(year: Int, month: Int, day: Int): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-" +
        day.toString().padStart(2, '0')

/** Presenta una fecha ISO como `DD/MM/AAAA`, sin cambiar el valor que se envía. */
internal fun displayDate(value: String): String {
    val parts = value.split("-")
    if (parts.size != 3 || parts[0].length != 4 || parts[1].length != 2 || parts[2].length != 2) {
        return value
    }
    return "${parts[2]}/${parts[1]}/${parts[0]}"
}

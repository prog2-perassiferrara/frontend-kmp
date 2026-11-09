package com.prog2.frontend.ui.components

import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Implementación Android: muestra `DatePickerDialog` y, salvo que se permitan
 * fechas pasadas, limita la selección al día actual. La fecha se entrega por
 * componentes para que el llamador conserve el formato `AAAA-MM-DD` del contrato.
 */
@Composable
actual fun DatePickerModal(
    initialDate: String,
    allowPastDates: Boolean,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val selected by rememberUpdatedState(onDateSelected)
    val dismissed by rememberUpdatedState(onDismiss)
    val initial = remember(initialDate, allowPastDates) { initialCalendar(initialDate, allowPastDates) }

    DisposableEffect(Unit) {
        val dialog = DatePickerDialog(
            context,
            { _, year, month, day -> selected(year, month + 1, day) },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH),
        )
        if (!allowPastDates) dialog.datePicker.minDate = startOfToday()
        dialog.setOnCancelListener { dismissed() }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}

/**
 * Usa la fecha recibida o hoy. Con fechas pasadas habilitadas no corrige el valor;
 * sin ellas, nunca abre el calendario antes de hoy.
 */
private fun initialCalendar(value: String, allowPastDates: Boolean): Calendar {
    val today = if (allowPastDates) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } else {
        startOfToday()
    }
    val parsed = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
    }.getOrNull()
    return Calendar.getInstance().apply {
        timeInMillis = parsed?.time ?: today
        if (!allowPastDates && timeInMillis < today) timeInMillis = today
    }
}

/** Fecha actual de atención expresada como medianoche local para `DatePickerDialog`. */
internal fun startOfToday(now: Long = System.currentTimeMillis()): Long {
    val attention = Calendar.getInstance(TimeZone.getTimeZone(ATTENTION_TIME_ZONE)).apply {
        timeInMillis = now
    }
    return Calendar.getInstance().apply {
        clear()
        set(attention.get(Calendar.YEAR), attention.get(Calendar.MONTH), attention.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

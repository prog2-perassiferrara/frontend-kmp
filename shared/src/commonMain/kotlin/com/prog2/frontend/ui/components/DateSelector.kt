package com.prog2.frontend.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Presenta días consecutivos. Cada pantalla decide cómo aplicar la selección. */
internal class DateSelector {
    @Composable
    fun Content(
        date: String,
        onDate: (String) -> Unit,
        label: String = "Elegí una fecha",
        enabled: Boolean = true,
        onClear: (() -> Unit)? = null,
    ) {
        var firstDay by rememberSaveable { mutableStateOf(date) }
        val days = remember(firstDay) { availabilityDays(firstDay) }
        val first = days.first()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    if (onClear != null && date.isNotEmpty()) {
                        Text(displayDate(date), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (onClear != null && date.isNotEmpty()) {
                    TextButton(onClick = onClear, enabled = enabled) { Text("Quitar fecha") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEach { day ->
                    val isSelected = day.date == date
                    OutlinedButton(
                        onClick = { onDate(day.date) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).semantics { selected = isSelected },
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.label, style = MaterialTheme.typography.bodyMedium)
                            Text(displayDate(day.date).take(5), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                if (first.label != "Hoy") {
                    TextButton(onClick = { firstDay = availabilityDays(first.date, -days.size).first().date }) {
                        Text("‹ Anteriores")
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { firstDay = availabilityDays(first.date, days.size).first().date }) {
                    Text("Más fechas ›")
                }
            }
        }
    }
}

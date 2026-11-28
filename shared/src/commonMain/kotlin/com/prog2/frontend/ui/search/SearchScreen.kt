package com.prog2.frontend.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prog2.frontend.catalog.data.Professional
import com.prog2.frontend.ui.components.DateSelector
import com.prog2.frontend.ui.components.FieldError
import com.prog2.frontend.ui.components.PageControls

/**
 * Presenta los filtros, resultados y paginación de CU-04. Toda validación,
 * conservación de estado y consulta pertenece al ViewModel.
 */
class SearchScreen {
    private val dateSelector = DateSelector()

    @Composable
    fun Content(
        state: SearchUiState,
        onName: (String) -> Unit,
        onCategory: (Long?) -> Unit,
        onEnabled: (Boolean?) -> Unit,
        onAvailableOn: (String) -> Unit,
        onSearch: () -> Unit,
        onRetry: () -> Unit,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onSelect: (Professional) -> Unit,
        onLoad: () -> Unit,
    ) {
        LaunchedEffect(Unit) { onLoad() }
        val enabled = !state.isSearching

        Text("Buscar profesionales", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = state.name,
            onValueChange = onName,
            label = { Text("Nombre o apellido") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = state.nameError != null,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
        )
        state.nameError?.let { FieldError(it) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { CategoryFilter(state, enabled, onCategory) }
            Box(Modifier.weight(1f)) { EnabledFilter(state.enabled, enabled, onEnabled) }
        }
        if (state.categoriesUnavailable) {
            FieldError("No se pudieron cargar las categorías; la búsqueda sigue disponible.")
        }

        dateSelector.Content(
            date = state.availableOn,
            onDate = onAvailableOn,
            label = "Fecha opcional",
            enabled = enabled,
            onClear = { onAvailableOn("") },
        )
        Text(
            "Filtra por día de atención. Los horarios libres se ven en la ficha.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(onClick = onSearch, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isSearching) "Buscando…" else "Buscar")
        }
        if (state.isSearching) CircularProgressIndicator()

        state.error?.let {
            FieldError(it)
            if (state.retryable) Button(onClick = onRetry, enabled = enabled) { Text("Reintentar") }
        }

        if (state.hasResult) {
            if (state.catalogOutdated) {
                Text(
                    "Aviso: la información de profesionales puede estar desactualizada; " +
                        "se muestran los últimos datos disponibles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.isEmptyResult) {
                Text("No se encontraron profesionales con esos filtros.")
            }
            state.results.forEach { professional ->
                ProfessionalRow(professional, enabled, onSelect)
                HorizontalDivider()
            }
            PageControls(
                state.page, state.totalPages, state.totalElements,
                if (state.totalElements == 1L) "resultado" else "resultados",
                enabled && state.canGoBack, enabled && state.canGoForward, onPrevious, onNext,
            )
        }
    }

    @Composable
    private fun CategoryFilter(state: SearchUiState, enabled: Boolean, onCategory: (Long?) -> Unit) {
        val options: List<Pair<Long?, String>> = buildList {
            add(null to "Todas")
            state.categories.forEach { add(it.id to it.name) }
        }
        val selected = state.categories.firstOrNull { it.id == state.categoryId }?.name ?: "Todas"
        FilterDropdown(
            label = "Categoría",
            selected = selected,
            enabled = enabled && state.categories.isNotEmpty(),
            options = options,
            onSelect = onCategory,
        )
    }

    @Composable
    private fun EnabledFilter(enabled: Boolean?, editable: Boolean, onEnabled: (Boolean?) -> Unit) {
        val selected = when (enabled) {
            true -> "Habilitados"
            false -> "Deshabilitados"
            null -> "Todos"
        }
        FilterDropdown(
            label = "Estado",
            selected = selected,
            enabled = editable,
            options = listOf(null to "Todos", true to "Habilitados", false to "Deshabilitados"),
            onSelect = onEnabled,
        )
    }

    @Composable
    private fun <T> FilterDropdown(
        label: String,
        selected: String,
        enabled: Boolean,
        options: List<Pair<T, String>>,
        onSelect: (T) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text(selected, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (value, text) ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = {
                                expanded = false
                                onSelect(value)
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ProfessionalRow(
        professional: Professional,
        enabled: Boolean,
        onSelect: (Professional) -> Unit,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .clickable(enabled = enabled) { onSelect(professional) }
                .padding(vertical = 8.dp),
        ) {
            Text(
                "${professional.firstName} ${professional.lastName}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(professional.category.name, style = MaterialTheme.typography.bodyMedium)
            if (!professional.category.enabled) {
                Text("Categoría deshabilitada", style = MaterialTheme.typography.bodyMedium)
            }
            if (!professional.enabled) {
                Text(
                    "Profesional deshabilitado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                "Ver disponibilidad ›",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

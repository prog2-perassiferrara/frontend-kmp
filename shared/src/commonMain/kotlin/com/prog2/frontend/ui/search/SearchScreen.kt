package com.prog2.frontend.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prog2.frontend.catalog.data.CatalogCategory
import com.prog2.frontend.catalog.data.Professional

/**
 * Presenta los filtros, resultados y paginación de CU-04. Toda validación,
 * conservación de estado y consulta pertenece al ViewModel.
 */
class SearchScreen {
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
        onOpenReservations: () -> Unit,
        onSignOut: () -> Unit,
        onLoad: () -> Unit,
    ) {
        LaunchedEffect(Unit) { onLoad() }
        val enabled = !state.isSearching

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Buscar profesionales", style = MaterialTheme.typography.h5)
            Row {
                TextButton(onClick = onOpenReservations) { Text("Mis reservas") }
                TextButton(onClick = onSignOut) { Text("Salir") }
            }
        }

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

        CategoryFilter(state, enabled, onCategory)
        if (state.categoriesUnavailable) {
            FieldError("No se pudieron cargar las categorías; la búsqueda sigue disponible.")
        }

        EnabledFilter(state.enabled, enabled, onEnabled)

        OutlinedTextField(
            value = state.availableOn,
            onValueChange = onAvailableOn,
            label = { Text("Disponible el (AAAA-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            isError = state.availableOnError != null,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (enabled) onSearch() }),
        )
        state.availableOnError?.let { FieldError(it) }
        Text(
            "Filtra por agenda habilitada para ese día de la semana; no garantiza horarios libres.",
            style = MaterialTheme.typography.body2,
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
                    "Aviso: el catálogo puede estar desactualizado; se muestran los últimos datos disponibles.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                )
            }
            if (state.isEmptyResult) {
                Text("No se encontraron profesionales con esos filtros.")
            }
            state.results.forEach { professional ->
                ProfessionalRow(professional, enabled, onSelect)
                Divider()
            }
            Pagination(state, enabled, onPrevious, onNext)
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
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled) {
                Text("$label: $selected")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onSelect(value)
                        },
                    ) { Text(text) }
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
                style = MaterialTheme.typography.subtitle1,
            )
            Text(professional.category.name, style = MaterialTheme.typography.body2)
            if (!professional.category.enabled) {
                Text("Categoría deshabilitada", style = MaterialTheme.typography.body2)
            }
            if (!professional.enabled) {
                Text(
                    "Profesional deshabilitado",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                )
            }
            Text(
                "Ver disponibilidad ›",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.primary,
            )
        }
    }

    @Composable
    private fun Pagination(state: SearchUiState, enabled: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
        if (state.totalPages <= 0) return
        Text(
            "Página ${state.page + 1} de ${state.totalPages} · ${state.totalElements} resultado(s)",
            style = MaterialTheme.typography.body2,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onPrevious, enabled = enabled && state.canGoBack) { Text("Anterior") }
            TextButton(onClick = onNext, enabled = enabled && state.canGoForward) { Text("Siguiente") }
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
}

@Preview
@Composable
private fun SearchScreenPreview() {
    MaterialTheme {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SearchScreen().Content(
                state = SearchUiState(
                    categories = listOf(CatalogCategory(1, "Clínica", true)),
                    hasResult = true,
                    totalPages = 1,
                    totalElements = 1,
                    results = listOf(
                        Professional(15, "Ana", "Gómez", true, CatalogCategory(1, "Clínica", true)),
                    ),
                ),
                onName = {}, onCategory = {}, onEnabled = {}, onAvailableOn = {},
                onSearch = {}, onRetry = {}, onPrevious = {}, onNext = {}, onSelect = {},
                onOpenReservations = {}, onSignOut = {}, onLoad = {},
            )
        }
    }
}

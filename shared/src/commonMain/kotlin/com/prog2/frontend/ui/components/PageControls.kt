package com.prog2.frontend.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun PageControls(
    page: Int,
    totalPages: Int,
    totalElements: Long,
    itemLabel: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    if (totalPages <= 0) return
    Text("Página ${page + 1} de $totalPages · $totalElements $itemLabel", style = MaterialTheme.typography.body2)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onPrevious, enabled = canGoBack) { Text("Anterior") }
        TextButton(onClick = onNext, enabled = canGoForward) { Text("Siguiente") }
    }
}

package com.prog2.frontend.ui.components

/** Presenta una fecha ISO como DD/MM/AAAA. */
internal fun displayDate(value: String): String {
    val parts = value.split("-")
    if (parts.size != 3 || parts[0].length != 4 || parts[1].length != 2 || parts[2].length != 2) {
        return value
    }
    return "${parts[2]}/${parts[1]}/${parts[0]}"
}

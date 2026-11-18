package com.tvpro.app.ui.components.detail

fun formatRuntime(runtime: String?): String {
    if (runtime.isNullOrBlank()) return "1h 55m"
    val mins = runtime.toDoubleOrNull()?.toInt() ?: runtime.toIntOrNull() ?: return runtime
    if (mins <= 0) return "1h 55m"
    val hours = mins / 60
    val remMins = mins % 60
    return if (hours > 0) {
        if (remMins > 0) "${hours}h ${remMins}m" else "${hours}h"
    } else {
        "${remMins}m"
    }
}

fun formatRemainingTime(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return "En progreso"
    val remMins = ((durationMs - positionMs).coerceAtLeast(0L) / 60_000L).toInt()
    if (remMins <= 0) return "Menos de 1 min restante"
    val h = remMins / 60
    val m = remMins % 60
    return if (h > 0) "Quedan ${h}h ${m}m" else "Quedan ${m} min"
}

enum class DetailTab(val label: String) {
    EPISODES("EPISODIOS"),
    SIMILAR("MÁS COMO ESTO"),
    EXTRAS("ESCENAS Y DETALLES")
}

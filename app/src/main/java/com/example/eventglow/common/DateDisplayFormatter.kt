package com.example.eventglow.common

import java.text.SimpleDateFormat
import java.util.Locale

private val inputPatterns = listOf("dd/MM/yyyy", "d/M/yyyy", "dd/M/yyyy", "d/MM/yyyy")

fun formatDisplayDate(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return raw

    for (pattern in inputPatterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }
            val date = parser.parse(value) ?: continue
            val output = SimpleDateFormat("MMM d yyyy", Locale.ENGLISH)
            return output.format(date)
        } catch (_: Exception) {
            // Continue trying alternative patterns.
        }
    }
    return raw
}


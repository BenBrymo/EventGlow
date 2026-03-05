package com.example.eventglow.common

import com.example.eventglow.dataClass.Event
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class EventTimelineBucket {
    ENDED,
    LIVE,
    TODAY,
    UPCOMING,
    UNKNOWN
}

data class EventWindow(
    val start: Date,
    val end: Date,
    val startDay: Date
)

object EventDateTimeUtils {
    private val datePatterns = listOf("dd/MM/yyyy", "d/M/yyyy", "dd/M/yyyy", "d/MM/yyyy", "yyyy-MM-dd")
    private val timePatterns = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm", "h a", "ha")

    fun resolveEventWindow(event: Event): EventWindow? {
        val startDate = parseDateFlexible(event.startDate) ?: return null
        val startTime = parseTime(event.eventTime)
        val startDateTime = mergeDateAndTime(
            date = startDate,
            hour = startTime?.first ?: 0,
            minute = startTime?.second ?: 0
        )

        val endDateOnly = parseDateFlexible(
            if (event.isMultiDayEvent) event.endDate.ifBlank { event.startDate } else event.startDate
        ) ?: startDate

        val endDateTime = when {
            event.isMultiDayEvent -> mergeDateAndTime(endDateOnly, 23, 59)
            event.durationMinutes > 0 -> Date(startDateTime.time + event.durationMinutes * 60_000L)
            else -> mergeDateAndTime(endDateOnly, 23, 59)
        }

        val normalizedEnd = if (endDateTime.before(startDateTime)) startDateTime else endDateTime
        return EventWindow(
            start = startDateTime,
            end = normalizedEnd,
            startDay = normalizeDate(startDate)
        )
    }

    fun classifyEventBucket(event: Event, now: Date = Date()): EventTimelineBucket {
        val window = resolveEventWindow(event) ?: return EventTimelineBucket.UNKNOWN
        val today = normalizeDate(now)
        return when {
            now.after(window.end) -> EventTimelineBucket.ENDED
            !now.before(window.start) && !now.after(window.end) -> EventTimelineBucket.LIVE
            window.startDay == today && window.start.after(now) -> EventTimelineBucket.TODAY
            window.startDay.after(today) -> EventTimelineBucket.UPCOMING
            else -> EventTimelineBucket.UNKNOWN
        }
    }

    fun normalizeDate(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    fun parseDateFlexible(value: String): Date? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        for (pattern in datePatterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }
                val parsed = parser.parse(trimmed)
                if (parsed != null) return parsed
            } catch (_: Exception) {
                // continue
            }
        }
        return null
    }

    fun parseTime(value: String): Pair<Int, Int>? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        for (pattern in timePatterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }
                val parsed = parser.parse(trimmed) ?: continue
                val calendar = Calendar.getInstance().apply { time = parsed }
                return calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
            } catch (_: Exception) {
                // continue
            }
        }
        return null
    }

    private fun mergeDateAndTime(date: Date, hour: Int, minute: Int): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}

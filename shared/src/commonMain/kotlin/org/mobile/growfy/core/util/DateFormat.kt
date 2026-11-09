package org.mobile.growfy.core.util

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

// Hand-rolled English formatting; swap for platform formatters when the app is localised.

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun nowLocal(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

val DayOfWeek.shortName: String get() = longName.take(3)
val DayOfWeek.longName: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
val Month.longName: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
val Month.shortName: String get() = longName.take(3)

/** "now", "5m", "2h", "1d", or "Oct 22" for anything older than a week. */
fun Instant.relativeTo(now: Instant): String {
    val elapsed = now - this
    return when {
        elapsed.inWholeMinutes < 1 -> "now"
        elapsed.inWholeHours < 1 -> "${elapsed.inWholeMinutes}m"
        elapsed.inWholeDays < 1 -> "${elapsed.inWholeHours}h"
        elapsed.inWholeDays < 7 -> "${elapsed.inWholeDays}d"
        else -> toLocalDateTime(TimeZone.currentSystemDefault()).date.monthDay()
    }
}

/** "Oct 22" */
fun LocalDate.monthDay(): String = "${month.shortName} $day"

/** "Tuesday, Oct 27" */
fun LocalDate.longDay(): String = "${dayOfWeek.longName}, ${monthDay()}"

/** "10:00" */
fun LocalDateTime.time24(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

/** "10:00 AM" */
fun LocalDateTime.time12(): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "$h:${minute.toString().padStart(2, '0')} ${if (hour < 12) "AM" else "PM"}"
}

/** "Tue, Oct 27 · 10:00 AM" */
fun LocalDateTime.scheduleLabel(): String = "${date.dayOfWeek.shortName}, ${date.monthDay()} · ${time12()}"

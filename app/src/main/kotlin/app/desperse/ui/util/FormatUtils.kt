package app.desperse.ui.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Format count with abbreviated suffixes (1.2k, 1.5M, etc.)
 */
fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
        count >= 1_000 -> "%.1fk".format(count / 1_000.0)
        else -> count.toString()
    }
}

/**
 * Format ISO timestamp to relative time string.
 * Returns "now", "2m", "3h", "5d", "2w", or "MMM d" for older dates.
 */
fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val now = Instant.now()
        val duration = Duration.between(instant, now)

        when {
            duration.seconds < 60 -> "now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m"
            duration.toHours() < 24 -> "${duration.toHours()}h"
            duration.toDays() < 7 -> "${duration.toDays()}d"
            duration.toDays() < 30 -> "${duration.toDays() / 7}w"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d")
                val zonedDateTime = instant.atZone(ZoneId.systemDefault())
                formatter.format(zonedDateTime)
            }
        }
    } catch (e: DateTimeParseException) {
        ""
    }
}

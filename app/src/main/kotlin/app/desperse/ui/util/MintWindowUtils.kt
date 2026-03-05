package app.desperse.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Phases of a timed edition mint window.
 */
sealed class MintWindowPhase {
    /** Display label for this phase (e.g. "Starts in 2d 3h", "45m left", "Ended"). */
    abstract val label: String

    data object None : MintWindowPhase() {
        override val label: String = ""
    }
    data class Scheduled(override val label: String, val startsAt: Long) : MintWindowPhase()
    data class Active(override val label: String, val endsAt: Long) : MintWindowPhase()
    data class EndingSoon(override val label: String, val endsAt: Long) : MintWindowPhase()
    data class Ended(override val label: String) : MintWindowPhase()
}

object MintWindowUtils {

    /** Duration preset hours */
    val DURATION_PRESETS = listOf(1.0, 24.0, 48.0, 72.0, 168.0)

    /** Human labels for duration presets */
    val DURATION_PRESET_LABELS = listOf("1h", "24h", "48h", "72h", "1 week")

    private const val ENDING_SOON_THRESHOLD_MS = 3600_000L // 1 hour

    /**
     * Determine the current phase of a mint window.
     */
    fun getMintWindowPhase(start: String?, end: String?): MintWindowPhase {
        if (start == null || end == null) return MintWindowPhase.None

        val now = System.currentTimeMillis()
        val startMs = parseIsoToEpochMs(start) ?: return MintWindowPhase.None
        val endMs = parseIsoToEpochMs(end) ?: return MintWindowPhase.None

        return when {
            now < startMs -> {
                val label = "Starts in ${formatDuration(startMs - now)}"
                MintWindowPhase.Scheduled(label, startMs)
            }
            now >= endMs -> {
                MintWindowPhase.Ended("Ended")
            }
            endMs - now <= ENDING_SOON_THRESHOLD_MS -> {
                val label = "${formatDuration(endMs - now)} left"
                MintWindowPhase.EndingSoon(label, endMs)
            }
            else -> {
                val label = "${formatDuration(endMs - now)} left"
                MintWindowPhase.Active(label, endMs)
            }
        }
    }

    /**
     * Format a duration in milliseconds to a compact string.
     * Examples: "2d 3h", "45m", "30s"
     */
    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0s"

        val totalSeconds = ms / 1000
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            days > 0 && hours > 0 -> "${days}d ${hours}h"
            days > 0 -> "${days}d"
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    /**
     * Format an ISO timestamp to a user-friendly date string.
     * Example: "Mar 5, 2:30 PM"
     */
    fun formatDateTime(isoString: String): String {
        return try {
            val instant = Instant.parse(isoString)
            val zoned = instant.atZone(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
            zoned.format(formatter)
        } catch (e: Exception) {
            isoString
        }
    }

    /**
     * Format epoch millis to a user-friendly date string.
     */
    fun formatDateTime(epochMs: Long): String {
        return try {
            val instant = Instant.ofEpochMilli(epochMs)
            val zoned = instant.atZone(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
            zoned.format(formatter)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Convert epoch millis to ISO-8601 string.
     */
    fun epochMsToIso(epochMs: Long): String {
        return Instant.ofEpochMilli(epochMs).toString()
    }

    fun parseIsoToEpochMs(iso: String): Long? {
        return try {
            Instant.parse(iso).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
}

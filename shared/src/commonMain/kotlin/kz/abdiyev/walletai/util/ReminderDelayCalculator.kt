package kz.abdiyev.walletai.util

/**
 * Calculates initial delay until the next reminder time in local civil time.
 */
object ReminderDelayCalculator {
    private const val HOUR_MS = 3_600_000L
    private const val MINUTE_MS = 60_000L
    private const val DAY_MS = 86_400_000L

    fun initialDelayMillis(
        nowMillis: Long,
        hour: Int,
        minute: Int,
        timeZoneOffsetMs: Long = 0L
    ): Long {
        require(hour in 0..23) { "hour must be 0..23" }
        require(minute in 0..59) { "minute must be 0..59" }

        val localNow = nowMillis + timeZoneOffsetMs
        val today = CalendarMillis.fromEpochMillis(localNow)
        var targetLocal = CalendarMillis.toStartOfDay(today) + hour * HOUR_MS + minute * MINUTE_MS
        if (targetLocal <= localNow) {
            targetLocal += DAY_MS
        }
        val targetUtc = targetLocal - timeZoneOffsetMs
        return (targetUtc - nowMillis).coerceAtLeast(0L)
    }
}

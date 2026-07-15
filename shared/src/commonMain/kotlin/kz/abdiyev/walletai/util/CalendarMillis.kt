package kz.abdiyev.walletai.util

import kz.abdiyev.walletai.domain.model.Period

data class CivilDate(val year: Int, val month: Int, val day: Int)

/**
 * KMP-safe civil calendar helpers (UTC-based). Pass [timeZoneOffsetMs] from the platform
 * when filtering user-facing local dates.
 */
object CalendarMillis {
    private const val DAY_MS = 86_400_000L

    fun fromEpochMillis(millis: Long): CivilDate {
        var remainingDays = millis / DAY_MS
        var year = 1970
        while (true) {
            val daysInYear = if (isLeapYear(year)) 366 else 365
            if (remainingDays < daysInYear) break
            remainingDays -= daysInYear
            year++
        }
        var month = 1
        while (month <= 12) {
            val daysInMonth = daysInMonth(year, month)
            if (remainingDays < daysInMonth) break
            remainingDays -= daysInMonth
            month++
        }
        return CivilDate(year, month, (remainingDays + 1).toInt())
    }

    fun toStartOfDay(date: CivilDate): Long =
        StatementDate.toEpochMillis(date.year, date.month, date.day)

    fun addDays(date: CivilDate, days: Int): CivilDate =
        fromEpochMillis(toStartOfDay(date) + days * DAY_MS)

    fun startOfWeek(date: CivilDate): CivilDate {
        val days = toStartOfDay(date) / DAY_MS
        val mondayOffset = ((days + 3) % 7).toInt()
        return fromEpochMillis((days - mondayOffset) * DAY_MS)
    }

    fun startOfMonth(date: CivilDate): CivilDate =
        CivilDate(date.year, date.month, 1)

    fun toLocalStartOfDay(epochMillis: Long, timeZoneOffsetMs: Long): Long {
        val localMillis = epochMillis + timeZoneOffsetMs
        val date = fromEpochMillis(localMillis)
        return toStartOfDay(date) - timeZoneOffsetMs
    }

    fun toLocalEndExclusive(epochMillis: Long, timeZoneOffsetMs: Long): Long {
        val localMillis = epochMillis + timeZoneOffsetMs
        val date = fromEpochMillis(localMillis)
        val nextDay = addDays(date, 1)
        return toStartOfDay(nextDay) - timeZoneOffsetMs
    }

    fun periodStartMillis(period: Period, nowMillis: Long, timeZoneOffsetMs: Long = 0L): Long {
        if (period == Period.ALL || period == Period.CUSTOM) return 0L
        val localMillis = nowMillis + timeZoneOffsetMs
        val today = fromEpochMillis(localMillis)
        val startDate = when (period) {
            Period.WEEK -> addDays(today, -6)
            Period.MONTH -> addDays(today, -29)
            else -> return 0L
        }
        return toStartOfDay(startDate) - timeZoneOffsetMs
    }

    fun endOfWeek(date: CivilDate): CivilDate = addDays(startOfWeek(date), 6)

    fun weekEndInclusiveMillis(nowMillis: Long, timeZoneOffsetMs: Long): Long {
        val localMillis = nowMillis + timeZoneOffsetMs
        val today = fromEpochMillis(localMillis)
        val sunday = endOfWeek(today)
        val sundayNoonUtc = toStartOfDay(sunday) - timeZoneOffsetMs + 12 * 3_600_000L
        return toLocalEndExclusive(sundayNoonUtc, timeZoneOffsetMs) - 1
    }

    fun periodEndExclusiveMillis(period: Period, nowMillis: Long, timeZoneOffsetMs: Long = 0L): Long? {
        if (period == Period.ALL) return null
        if (period == Period.CUSTOM) return null
        return nowMillis + 1
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        val monthDays = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        return monthDays[month] + if (month == 2 && isLeapYear(year)) 1 else 0
    }

    private fun isLeapYear(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}

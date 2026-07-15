package kz.abdiyev.walletai.util

/**
 * Converts civil dates to UTC midnight epoch millis without java.time (KMP-safe).
 */
object StatementDate {
    fun kaspiDate(day: Int, month: Int, yearTwoDigits: Int): Long =
        toEpochMillis(2000 + yearTwoDigits, month, day)

    fun freedomDate(day: Int, month: Int, year: Int): Long =
        toEpochMillis(year, month, day)

    fun toEpochMillis(year: Int, month: Int, day: Int): Long {
        require(month in 1..12) { "Invalid month: $month" }
        require(day in 1..31) { "Invalid day: $day" }
        val monthDays = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var totalDays = 0L
        for (y in 1970 until year) {
            totalDays += if (isLeapYear(y)) 366 else 365
        }
        for (m in 1 until month) {
            totalDays += monthDays[m] + if (m == 2 && isLeapYear(year)) 1 else 0
        }
        totalDays += day - 1
        return totalDays * 86_400_000L
    }

    private fun isLeapYear(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}

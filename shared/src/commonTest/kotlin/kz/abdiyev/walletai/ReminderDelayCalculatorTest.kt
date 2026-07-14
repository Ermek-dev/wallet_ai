package kz.abdiyev.walletai

import kz.abdiyev.walletai.util.ReminderDelayCalculator
import kz.abdiyev.walletai.util.StatementDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderDelayCalculatorTest {

    @Test
    fun initialDelayMillis_sameDayBeforeTarget_returnsRemainingTime() {
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 18 * 3_600_000L
        val delay = ReminderDelayCalculator.initialDelayMillis(now, hour = 21, minute = 0, timeZoneOffsetMs = 0L)
        assertEquals(3 * 3_600_000L, delay)
    }

    @Test
    fun initialDelayMillis_afterTarget_schedulesNextDay() {
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 22 * 3_600_000L
        val delay = ReminderDelayCalculator.initialDelayMillis(now, hour = 21, minute = 0, timeZoneOffsetMs = 0L)
        assertEquals(23 * 3_600_000L, delay)
    }

    @Test
    fun initialDelayMillis_respectsMinutes() {
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 20 * 3_600_000L + 15 * 60_000L
        val delay = ReminderDelayCalculator.initialDelayMillis(now, hour = 20, minute = 30, timeZoneOffsetMs = 0L)
        assertEquals(15 * 60_000L, delay)
    }

    @Test
    fun initialDelayMillis_isNeverNegative() {
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 20 * 3_600_000L + 30 * 60_000L
        val delay = ReminderDelayCalculator.initialDelayMillis(now, hour = 20, minute = 30, timeZoneOffsetMs = 0L)
        assertTrue(delay >= 0L)
    }
}

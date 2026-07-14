package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.PeriodRange
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.util.CalendarMillis
import kz.abdiyev.walletai.util.PeriodFilter
import kz.abdiyev.walletai.util.StatementDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeriodFilterTest {

    @Test
    fun filter_week_excludesBeforeRollingWindowStart() {
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 12 * 3_600_000L
        val weekStart = PeriodFilter.periodStartMillis(Period.WEEK, now, 0L)
        val txs = listOf(
            tx(weekStart + 1_000L, "inside"),
            tx(weekStart - 1_000L, "outside")
        )
        val filtered = PeriodFilter.filter(txs, Period.WEEK, now, 0L)
        assertEquals(1, filtered.size)
        assertEquals("inside", filtered.first().description)
    }

    @Test
    fun filter_week_includesSixDaysAgo_excludesSevenDaysAgo() {
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 12 * 3_600_000L
        val sixDaysAgo = StatementDate.toEpochMillis(2026, 7, 7) + 12 * 3_600_000L
        val sevenDaysAgo = StatementDate.toEpochMillis(2026, 7, 6) + 12 * 3_600_000L
        val txs = listOf(
            tx(sixDaysAgo, "sixDays"),
            tx(sevenDaysAgo, "sevenDays")
        )
        val filtered = PeriodFilter.filter(txs, Period.WEEK, now, 0L)
        assertEquals(1, filtered.size)
        assertEquals("sixDays", filtered.first().description)
    }

    @Test
    fun filter_week_includesLateLocalTimeOnSixthDayAgo_withTimeZoneOffset() {
        val offset = 6 * 3_600_000L
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 10 * 3_600_000L
        val todayLocal = CalendarMillis.fromEpochMillis(now + offset)
        val sixDaysAgoLocal = CalendarMillis.addDays(todayLocal, -6)
        val sixDaysAgoStart = CalendarMillis.toStartOfDay(sixDaysAgoLocal) - offset
        val lateSixDaysAgo = sixDaysAgoStart + 23 * 3_600_000L + 59 * 60_000L
        val sevenDaysAgoLocal = CalendarMillis.addDays(todayLocal, -7)
        val sevenDaysAgoStart = CalendarMillis.toStartOfDay(sevenDaysAgoLocal) - offset
        val lateSevenDaysAgo = sevenDaysAgoStart + 23 * 3_600_000L + 59 * 60_000L
        val txs = listOf(
            tx(lateSixDaysAgo, "inside"),
            tx(lateSevenDaysAgo, "outside")
        )
        val filtered = PeriodFilter.filter(txs, Period.WEEK, now, offset)
        assertEquals(1, filtered.size)
        assertEquals("inside", filtered.first().description)
    }

    @Test
    fun filter_week_excludesFutureTransactions() {
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 12 * 3_600_000L
        val txs = listOf(
            tx(now - 1_000L, "past"),
            tx(now + 86_400_000L, "future")
        )
        val filtered = PeriodFilter.filter(txs, Period.WEEK, now, 0L)
        assertEquals(1, filtered.size)
        assertEquals("past", filtered.first().description)
    }

    @Test
    fun filter_all_returnsEverything() {
        val now = StatementDate.toEpochMillis(2026, 7, 13)
        val txs = listOf(tx(StatementDate.toEpochMillis(2020, 1, 1), "ancient"))
        assertEquals(1, PeriodFilter.filter(txs, Period.ALL, now).size)
    }

    @Test
    fun periodStartMillis_week_isSixDaysBeforeToday() {
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 12 * 3_600_000L
        val start = PeriodFilter.periodStartMillis(Period.WEEK, now, 0L)
        assertEquals(StatementDate.toEpochMillis(2026, 7, 7), start)
    }

    @Test
    fun periodStartMillis_month_isTwentyNineDaysBeforeToday() {
        val now = StatementDate.toEpochMillis(2026, 7, 30)
        val start = PeriodFilter.periodStartMillis(Period.MONTH, now, 0L)
        assertEquals(StatementDate.toEpochMillis(2026, 7, 1), start)
    }

    @Test
    fun filter_month_includesTwentyNineDaysAgo_excludesThirtyDaysAgo() {
        val now = StatementDate.toEpochMillis(2026, 7, 30) + 12 * 3_600_000L
        val twentyNineDaysAgo = StatementDate.toEpochMillis(2026, 7, 1) + 12 * 3_600_000L
        val thirtyDaysAgo = StatementDate.toEpochMillis(2026, 6, 30) + 12 * 3_600_000L
        val txs = listOf(
            tx(twentyNineDaysAgo, "twentyNine"),
            tx(thirtyDaysAgo, "thirty")
        )
        val filtered = PeriodFilter.filter(txs, Period.MONTH, now, 0L)
        assertEquals(1, filtered.size)
        assertEquals("twentyNine", filtered.first().description)
    }

    @Test
    fun filter_custom_sameDay_includesWholeDay() {
        val dayStart = StatementDate.toEpochMillis(2026, 7, 10)
        val range = PeriodFilter.customRangeFromLocalDays(dayStart, dayStart, 0L)
        val txs = listOf(
            tx(dayStart + 1_000L, "inside"),
            tx(dayStart + 86_399_000L, "late"),
            tx(dayStart + 86_400_000L, "nextDay")
        )
        val filtered = PeriodFilter.filter(txs, Period.CUSTOM, dayStart, 0L, range)
        assertEquals(2, filtered.size)
        assertTrue(filtered.none { it.description == "nextDay" })
    }

    @Test
    fun filter_custom_excludesDayAfterEndDate() {
        val start = StatementDate.toEpochMillis(2026, 7, 10)
        val end = StatementDate.toEpochMillis(2026, 7, 12)
        val range = PeriodFilter.customRangeFromLocalDays(start, end, 0L)
        val nextDay = StatementDate.toEpochMillis(2026, 7, 13)
        val txs = listOf(tx(end + 12 * 3_600_000L, "lastDay"), tx(nextDay, "after"))
        val filtered = PeriodFilter.filter(txs, Period.CUSTOM, end, 0L, range)
        assertEquals(1, filtered.size)
        assertEquals("lastDay", filtered.first().description)
    }

    @Test
    fun customRangeFromLocalDays_swapsWhenStartAfterEnd() {
        val later = StatementDate.toEpochMillis(2026, 7, 15)
        val earlier = StatementDate.toEpochMillis(2026, 7, 10)
        val range = PeriodFilter.customRangeFromLocalDays(later, earlier, 0L)
        assertEquals(StatementDate.toEpochMillis(2026, 7, 10), range.startInclusive)
        assertEquals(StatementDate.toEpochMillis(2026, 7, 16), range.endExclusive)
    }

    @Test
    fun bounds_all_returnsNull() {
        assertNull(PeriodFilter.bounds(Period.ALL, 0L, 0L))
    }

    @Test
    fun calendarMillis_weekEndInclusive_isSunday() {
        val monday = StatementDate.toEpochMillis(2026, 7, 13) + 12 * 3_600_000L
        val endInclusive = CalendarMillis.weekEndInclusiveMillis(monday, 0L)
        val endDate = CalendarMillis.fromEpochMillis(endInclusive)
        assertEquals(19, endDate.day)
        assertEquals(7, endDate.month)
    }

    @Test
    fun filter_month_excludesTransactionsOlderThanThirtyDays() {
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 12 * 3_600_000L
        val olderThanWindow = StatementDate.toEpochMillis(2026, 6, 1)
        val withinWindow = StatementDate.toEpochMillis(2026, 7, 13) + 3_600_000L
        val txs = listOf(
            tx(olderThanWindow, "old"),
            tx(withinWindow, "recent")
        )
        val month = PeriodFilter.filter(txs, Period.MONTH, now, 0L)
        assertEquals(1, month.size)
        assertEquals("recent", month.first().description)

        val all = PeriodFilter.filter(txs, Period.ALL, now, 0L)
        assertEquals(2, all.size)
    }

    @Test
    fun filter_month_includesStatementDateOnUtcMidnight() {
        val now = StatementDate.toEpochMillis(2026, 7, 15) + 15 * 3_600_000L
        val julyImported = StatementDate.toEpochMillis(2026, 7, 10)
        val txs = listOf(tx(julyImported, "importedJuly"))
        assertEquals(1, PeriodFilter.filter(txs, Period.MONTH, now, 0L).size)
    }

    @Test
    fun outsideCount_month_countsTransactionsOlderThanThirtyDays() {
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 12 * 3_600_000L
        val olderThanWindow = StatementDate.toEpochMillis(2026, 6, 1)
        val withinWindow = StatementDate.toEpochMillis(2026, 7, 13) + 3_600_000L
        val txs = listOf(tx(olderThanWindow, "old"), tx(withinWindow, "recent"))
        assertEquals(1, PeriodFilter.outsideCount(txs, Period.MONTH, now, 0L))
        assertEquals(0, PeriodFilter.outsideCount(txs, Period.ALL, now, 0L))
    }

    @Test
    fun outsideCount_week_countsTransactionsOutsideRollingWindow() {
        val now = StatementDate.toEpochMillis(2026, 7, 13) + 12 * 3_600_000L
        val weekStart = PeriodFilter.periodStartMillis(Period.WEEK, now, 0L)
        val txs = listOf(
            tx(weekStart + 1_000L, "thisWeek"),
            tx(weekStart - 86_400_000L, "lastWeek")
        )
        assertEquals(1, PeriodFilter.outsideCount(txs, Period.WEEK, now, 0L))
    }

    @Test
    fun calendarMillis_startOfWeek_isMonday() {
        val wednesday = CalendarMillis.fromEpochMillis(StatementDate.toEpochMillis(2026, 7, 15))
        val monday = CalendarMillis.startOfWeek(wednesday)
        assertEquals(13, monday.day)
        assertEquals(7, monday.month)
    }
}

private fun tx(date: Long, desc: String) = TransactionData(
    money = Money.fromMajor(100.0, Currency.KZT),
    type = TransactionType.EXPENSE,
    categoryId = 1,
    description = desc,
    date = date
)

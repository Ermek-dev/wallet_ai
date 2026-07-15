package kz.abdiyev.walletai.util

import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.PeriodRange
import kz.abdiyev.walletai.domain.model.TransactionData

object PeriodFilter {
    fun filter(
        transactions: List<TransactionData>,
        period: Period,
        nowMillis: Long,
        timeZoneOffsetMs: Long = 0L,
        customRange: PeriodRange? = null
    ): List<TransactionData> {
        if (transactions.isEmpty()) return transactions
        val bounds = bounds(period, nowMillis, timeZoneOffsetMs, customRange) ?: return transactions
        return transactions.filter { tx ->
            val txDay = CalendarMillis.toLocalStartOfDay(tx.date, timeZoneOffsetMs)
            txDay >= bounds.startInclusive && txDay < bounds.endExclusive
        }
    }

    fun bounds(
        period: Period,
        nowMillis: Long,
        timeZoneOffsetMs: Long = 0L,
        customRange: PeriodRange? = null
    ): PeriodRange? {
        if (period == Period.ALL) return null
        if (period == Period.CUSTOM) return customRange
        val start = periodStartMillis(period, nowMillis, timeZoneOffsetMs)
        val end = CalendarMillis.toLocalEndExclusive(nowMillis, timeZoneOffsetMs)
        return PeriodRange(startInclusive = start, endExclusive = end)
    }

    fun periodStartMillis(period: Period, nowMillis: Long, timeZoneOffsetMs: Long = 0L): Long =
        CalendarMillis.periodStartMillis(period, nowMillis, timeZoneOffsetMs)

    fun customRangeFromLocalDays(
        startDayMillis: Long,
        endDayMillis: Long,
        timeZoneOffsetMs: Long
    ): PeriodRange {
        val firstDay = minOf(startDayMillis, endDayMillis)
        val lastDay = maxOf(startDayMillis, endDayMillis)
        return PeriodRange(
            startInclusive = CalendarMillis.toLocalStartOfDay(firstDay, timeZoneOffsetMs),
            endExclusive = CalendarMillis.toLocalEndExclusive(lastDay, timeZoneOffsetMs)
        )
    }

    fun outsideCount(
        transactions: List<TransactionData>,
        period: Period,
        nowMillis: Long,
        timeZoneOffsetMs: Long = 0L,
        customRange: PeriodRange? = null
    ): Int = if (period == Period.ALL) {
        0
    } else {
        transactions.size - filter(transactions, period, nowMillis, timeZoneOffsetMs, customRange).size
    }
}

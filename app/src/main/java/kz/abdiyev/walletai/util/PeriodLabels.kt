package kz.abdiyev.walletai.util

import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.PeriodRange
import kz.abdiyev.walletai.util.CalendarMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PeriodLabels {
    private val ru = Locale("ru")
    private val dayMonth = SimpleDateFormat("d MMM", ru)
    private val dayMonthYear = SimpleDateFormat("d MMM yyyy", ru)
    private val monthYear = SimpleDateFormat("LLLL yyyy", ru)

    fun title(
        period: Period,
        nowMillis: Long = System.currentTimeMillis(),
        customRange: PeriodRange? = null
    ): String = when (period) {
        Period.CUSTOM -> if (customRange != null) "Период" else "Выберите период"
        Period.WEEK -> "Эта неделя"
        Period.MONTH -> monthYear.format(Date(nowMillis)).replaceFirstChar { it.titlecase(ru) }
        Period.ALL -> "Всё"
    }

    fun rangeLabel(
        period: Period,
        nowMillis: Long = System.currentTimeMillis(),
        customRange: PeriodRange? = null
    ): String {
        if (period == Period.ALL) return "За всё время"
        if (period == Period.CUSTOM && customRange != null) {
            return formatCustomRange(customRange)
        }
        val offset = TimeZone.getDefault().getOffset(nowMillis).toLong()
        val start = PeriodFilter.periodStartMillis(period, nowMillis, offset)
        val endInclusive = when (period) {
            Period.WEEK -> CalendarMillis.weekEndInclusiveMillis(nowMillis, offset)
            else -> CalendarMillis.toLocalEndExclusive(nowMillis, offset) - 1
        }
        return formatInclusiveRange(start, endInclusive)
    }

    fun analyticsHeading(
        period: Period,
        chartModeExpense: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        customRange: PeriodRange? = null
    ): String {
        val kind = if (chartModeExpense) "Расходы" else "Доходы"
        return when (period) {
            Period.MONTH -> "$kind за ${monthYear.format(Date(nowMillis)).replaceFirstChar { it.titlecase(ru) }}"
            Period.CUSTOM -> if (customRange != null) "$kind · ${formatCustomRange(customRange)}" else "$kind · Период"
            else -> "$kind · ${title(period, nowMillis, customRange)}"
        }
    }

    fun exportLabel(
        period: Period,
        nowMillis: Long = System.currentTimeMillis(),
        customRange: PeriodRange? = null
    ): String = when {
        period == Period.ALL -> "Все операции"
        period == Period.CUSTOM && customRange != null -> formatCustomRange(customRange)
        else -> title(period, nowMillis, customRange)
    }

    private fun formatInclusiveRange(startInclusive: Long, endInclusive: Long): String {
        val start = Date(startInclusive)
        val end = Date(endInclusive)
        val startYear = dayMonthYear.format(start).substringAfterLast(' ')
        val endYear = dayMonthYear.format(end).substringAfterLast(' ')
        val sameDay = dayMonthYear.format(start) == dayMonthYear.format(end)
        return when {
            sameDay -> dayMonthYear.format(start)
            startYear == endYear -> "${dayMonth.format(start)} — ${dayMonth.format(end)} $startYear"
            else -> "${dayMonthYear.format(start)} — ${dayMonthYear.format(end)}"
        }
    }

    private fun formatCustomRange(range: PeriodRange): String =
        formatInclusiveRange(range.startInclusive, range.endExclusive - 1)
}

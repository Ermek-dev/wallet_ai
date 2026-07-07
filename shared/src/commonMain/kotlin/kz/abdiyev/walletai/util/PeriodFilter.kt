package kz.abdiyev.walletai.util

import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.TransactionData

object PeriodFilter {
    fun filter(
        transactions: List<TransactionData>,
        period: Period,
        nowMillis: Long
    ): List<TransactionData> {
        if (period == Period.ALL || transactions.isEmpty()) return transactions
        val startTime = periodStartMillis(period, nowMillis)
        return transactions.filter { it.date >= startTime }
    }

    fun periodStartMillis(period: Period, nowMillis: Long): Long {
        val dayMs = 24L * 60 * 60 * 1000
        return when (period) {
            Period.WEEK -> nowMillis - 7 * dayMs
            Period.MONTH -> nowMillis - 30 * dayMs
            Period.THREE_MONTHS -> nowMillis - 90 * dayMs
            Period.ALL -> 0L
        }
    }
}

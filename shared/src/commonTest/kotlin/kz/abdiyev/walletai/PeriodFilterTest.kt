package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.util.PeriodFilter
import kotlin.test.Test
import kotlin.test.assertEquals

class PeriodFilterTest {

    private val dayMs = 24L * 60 * 60 * 1000

    @Test
    fun filter_week_excludesOlderThanSevenDays() {
        val now = 1_000_000L
        val txs = listOf(
            tx(now - 2 * dayMs, "recent"),
            tx(now - 10 * dayMs, "old")
        )
        val filtered = PeriodFilter.filter(txs, Period.WEEK, now)
        assertEquals(1, filtered.size)
        assertEquals("recent", filtered.first().description)
    }

    @Test
    fun filter_all_returnsEverything() {
        val now = 1_000_000L
        val txs = listOf(tx(now - 365 * dayMs, "ancient"))
        assertEquals(1, PeriodFilter.filter(txs, Period.ALL, now).size)
    }

    @Test
    fun periodStartMillis_month_isThirtyDays() {
        val now = 90L * dayMs
        assertEquals(60L * dayMs, PeriodFilter.periodStartMillis(Period.MONTH, now))
    }

    private fun tx(date: Long, desc: String) = TransactionData(
        money = Money.fromMajor(100.0, Currency.KZT),
        type = TransactionType.EXPENSE,
        categoryId = 1,
        description = desc,
        date = date
    )
}

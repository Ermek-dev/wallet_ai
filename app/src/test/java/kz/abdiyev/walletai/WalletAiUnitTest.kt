package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.util.MoneyFormatter
import kz.abdiyev.walletai.util.PeriodFilter
import kz.abdiyev.walletai.util.StatementDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletAiUnitTest {

    @Test
    fun moneyFormatter_formatsKzt() {
        val formatted = MoneyFormatter.format(Money.fromMajor(12500.0, Currency.KZT))
        assertEquals("12 500 ₸", formatted)
    }

    @Test
    fun periodFilter_week_returnsRecentOnly() {
        val now = StatementDate.toEpochMillis(2026, 7, 15) + 12 * 3_600_000L
        val weekStart = PeriodFilter.periodStartMillis(Period.WEEK, now, 0L)
        val txs = listOf(
            tx(weekStart + 1_000L, "recent"),
            tx(weekStart - 1_000L, "old")
        )
        val filtered = PeriodFilter.filter(txs, Period.WEEK, now, 0L)
        assertEquals(1, filtered.size)
        assertEquals("recent", filtered.first().description)
    }

    @Test
    fun bankStatementParser_parsesTengeLine() {
        val cats = listOf(
            kz.abdiyev.walletai.domain.model.CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart")
        )
        val result = BankStatementParser().parse("Magnum 3400 тг", cats, null, Currency.KZT, 0L).lines
        assertTrue(result.isNotEmpty())
        assertEquals(3400.0, result.first().money.majorAmount, 0.01)
    }

    private fun tx(date: Long, desc: String) = TransactionData(
        money = Money.fromMajor(100.0),
        type = TransactionType.EXPENSE,
        categoryId = 1,
        description = desc,
        date = date
    )

    companion object {
    }
}

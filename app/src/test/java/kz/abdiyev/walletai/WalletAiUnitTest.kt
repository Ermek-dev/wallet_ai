package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.util.MoneyFormatter
import kz.abdiyev.walletai.util.PeriodFilter
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
        val now = 1_000_000L
        val txs = listOf(
            tx(now - 2 * DAY, "recent"),
            tx(now - 10 * DAY, "old")
        )
        val filtered = PeriodFilter.filter(txs, Period.WEEK, now)
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
        private const val DAY = 24L * 60 * 60 * 1000
    }
}

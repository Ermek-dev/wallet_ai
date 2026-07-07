package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MoneyTest {

    @Test
    fun fromMajor_convertsKztCorrectly() {
        val money = Money.fromMajor(3400.0, Currency.KZT)
        assertEquals(340_000L, money.amountMinor)
        assertEquals(3400.0, money.majorAmount)
    }

    @Test
    fun fromMajorString_acceptsCommaDecimal() {
        val money = Money.fromMajorString("1250,50", Currency.KZT)
        assertEquals(125_050L, money?.amountMinor)
    }

    @Test
    fun fromMajorString_rejectsZeroAndNegative() {
        assertNull(Money.fromMajorString("0", Currency.KZT))
        assertNull(Money.fromMajorString("-100", Currency.KZT))
    }

    @Test
    fun plusAndMinus_requireSameCurrency() {
        val a = Money.fromMajor(100.0, Currency.KZT)
        val b = Money.fromMajor(40.0, Currency.KZT)
        assertEquals(140.0, (a + b).majorAmount)
        assertEquals(60.0, (a - b).majorAmount)
    }

    @Test
    fun plus_failsOnDifferentCurrencies() {
        val kzt = Money.fromMajor(100.0, Currency.KZT)
        val usd = Money.fromMajor(100.0, Currency.USD)
        assertFailsWith<IllegalArgumentException> { kzt + usd }
    }
}

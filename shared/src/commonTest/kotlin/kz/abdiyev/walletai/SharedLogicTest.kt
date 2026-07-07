package kz.abdiyev.walletai.ml

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BankStatementParserTest {
    private val categories = listOf(
        kz.abdiyev.walletai.domain.model.CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart"),
        kz.abdiyev.walletai.domain.model.CategoryData(8, "Зарплата", TransactionType.INCOME, "#9CCC65", "payments")
    )

    @Test
    fun parsesKztAmount() {
        val parser = BankStatementParser()
        val result = parser.parse(
            "Magnum продукты 3400 тг",
            categories,
            null,
            Currency.KZT
        )
        assertEquals(1, result.size)
        assertEquals(3400.0, result.first().money.majorAmount)
    }

    @Test
    fun detectsIncomeFromKeyword() {
        val parser = BankStatementParser()
        val result = parser.parse(
            "Зачисление зарплата +450000 ₸",
            categories,
            null,
            Currency.KZT
        )
        assertEquals(TransactionType.INCOME, result.first().type)
    }
}

class VoiceInputParserTest {
    @Test
    fun parsesExpensePhrase() {
        val parser = VoiceInputParser()
        val result = parser.parse("потратил 3400 в Сели Поели", Currency.KZT)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertNotNull(result.money)
        assertEquals(3400.0, result.money!!.majorAmount)
    }

    @Test
    fun keepsMerchantInDescription() {
        val parser = VoiceInputParser()
        val result = parser.parse("потратил 3400 в Сели Поели", Currency.KZT)
        assertEquals("Сели поели", result.description)
    }

    @Test
    fun parsesWordNumberThousands() {
        val parser = VoiceInputParser()
        val result = parser.parse("потратил три тысячи четыреста в магнуме", Currency.KZT)
        assertNotNull(result.money)
        assertEquals(3400.0, result.money!!.majorAmount)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test
    fun parsesWordNumberIncome() {
        val parser = VoiceInputParser()
        val result = parser.parse("получил зарплату сто пятьдесят тысяч", Currency.KZT)
        assertNotNull(result.money)
        assertEquals(150000.0, result.money!!.majorAmount)
        assertEquals(TransactionType.INCOME, result.type)
    }

    @Test
    fun parsesSmallWordNumber() {
        val parser = VoiceInputParser()
        val result = parser.parse("кофе двести пятьдесят", Currency.KZT)
        assertNotNull(result.money)
        assertEquals(250.0, result.money!!.majorAmount)
    }

    @Test
    fun stripsNumberWordsFromDescription() {
        val parser = VoiceInputParser()
        val result = parser.parse("потратил три тысячи четыреста в магнуме", Currency.KZT)
        assertEquals("Магнуме", result.description)
    }
}

class MoneyParsingTest {
    @Test
    fun parsesPlainAmount() {
        assertEquals(3400_00L, Money.fromMajorString("3400", Currency.KZT)?.amountMinor)
    }

    @Test
    fun parsesAmountWithCommaDecimal() {
        assertEquals(3400_50L, Money.fromMajorString("3400,50", Currency.KZT)?.amountMinor)
    }

    @Test
    fun parsesAmountWithGroupingSpaces() {
        assertEquals(3400_00L, Money.fromMajorString("3 400", Currency.KZT)?.amountMinor)
    }

    @Test
    fun parsesAmountWithNonBreakingSpaces() {
        assertEquals(12500_00L, Money.fromMajorString("12\u00A0500", Currency.KZT)?.amountMinor)
    }

    @Test
    fun rejectsInvalidInput() {
        assertEquals(null, Money.fromMajorString("", Currency.KZT))
        assertEquals(null, Money.fromMajorString("abc", Currency.KZT))
        assertEquals(null, Money.fromMajorString("0", Currency.KZT))
        assertEquals(null, Money.fromMajorString("-500", Currency.KZT))
    }
}

class MoneyFormatterTest {
    @Test
    fun formatsKztWithSpaces() {
        val formatted = kz.abdiyev.walletai.util.MoneyFormatter.format(
            Money.fromMajor(12500.0, Currency.KZT)
        )
        assertEquals("12 500 ₸", formatted)
    }
}

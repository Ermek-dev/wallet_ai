package kz.abdiyev.walletai.statement

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.statement.MerchantDictionary
import kz.abdiyev.walletai.ml.statement.RawStatementLine
import kz.abdiyev.walletai.ml.statement.StatementCategorizer
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MerchantDictionaryTest {
    @Test
    fun mapsMagnumToGroceries() {
        assertEquals("Продукты", MerchantDictionary.categoryNameFor("MAGNUM.AF50.KASSA-5", "Покупка"))
    }

    @Test
    fun mapsWhooshToTransport() {
        assertEquals("Транспорт", MerchantDictionary.categoryNameFor("FRPAY WHOOSH ALMATY KZ", "Сумма в обработке"))
    }

    @Test
    fun mapsSalaryKeyword() {
        assertEquals("Зарплата", MerchantDictionary.categoryNameFor("Зарплата", "Пополнение"))
    }

    @Test
    fun unknownMerchantReturnsNull() {
        assertNull(MerchantDictionary.categoryNameFor("Random shop", "Покупка"))
    }
}

class StatementCategorizerTest {
    private val categories = listOf(
        CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart"),
        CategoryData(2, "Кофе и Рестораны", TransactionType.EXPENSE, "#FFA726", "restaurant"),
        CategoryData(3, "Транспорт", TransactionType.EXPENSE, "#26A69A", "commute"),
        CategoryData(10, "Переводы", TransactionType.EXPENSE, "#B0BEC5", "swap_horiz", isSystem = true)
    )
    private val categorizer = StatementCategorizer()

    @Test
    fun usesDictionaryBeforeTransfersFallback() {
        val raw = RawStatementLine(
            money = Money.fromMajor(100.0, Currency.KZT),
            type = TransactionType.EXPENSE,
            description = "MAGNUM SHOP",
            rawOperation = "Покупка",
            date = 0L,
            isTransfer = false
        )
        assertEquals(1L, categorizer.categoryIdFor(raw, categories, null, transfersCategoryId = 10L))
    }

    @Test
    fun fallsBackToTransfersCategoryForBareTransfer() {
        val raw = RawStatementLine(
            money = Money.fromMajor(100.0, Currency.KZT),
            type = TransactionType.EXPENSE,
            description = "Сұлтан С.",
            rawOperation = "Перевод",
            date = 0L,
            isTransfer = true
        )
        assertEquals(10L, categorizer.categoryIdFor(raw, categories, null, transfersCategoryId = 10L))
    }
}

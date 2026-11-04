package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.*
import kz.abdiyev.walletai.domain.usecase.ImportStatementResult
import kz.abdiyev.walletai.domain.usecase.ImportStatementUseCase
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.test.FakeMerchantRuleStore
import kz.abdiyev.walletai.test.FakeTransactionStore
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ImportMemoryTest {
    private val store = FakeTransactionStore()
    private val useCase = ImportStatementUseCase(store, FakeMerchantRuleStore(), BankStatementParser())
    private val categories = listOf(
        CategoryData(1, "Продукты", TransactionType.EXPENSE, "#000000", "cart"),
        CategoryData(2, "Другое", TransactionType.EXPENSE, "#000000", "cart")
    )
    private val line = "29.06.26 - 150,00 ₸ Покупка ИП Сергеев"

    private suspend fun parse(lines: String = line, account: String = "") =
        (useCase.execute("www.kaspi.kz\n$lines", categories, Currency.KZT, null, 0L, account)
            as ImportStatementResult.Success).previews

    @Test
    fun reimportPreservesManualEditsAndMatchesOriginalIdentity() = runTest {
        val preview = parse().single()
        store.insert(TransactionData(
            money = Money(900L, Currency.KZT), type = preview.type, categoryId = 2,
            description = "Арзан", date = preview.date + 12345,
            sourceKey = preview.sourceKey, originalDescription = preview.originalDescription
        ))
        assertTrue(parse().single().isDuplicate)
        assertEquals("Арзан", store.getAll().single().description)
        assertEquals(2L, store.getAll().single().categoryId)
    }

    @Test
    fun identicalPurchasesAreMatchedByCount() = runTest {
        val first = parse().single()
        store.insert(TransactionData(money = first.money, type = first.type, categoryId = 1,
            description = first.description, date = first.date, sourceKey = first.sourceKey))
        val previews = parse("$line\n$line")
        assertEquals(1, previews.count { it.isDuplicate })
        assertEquals(1, previews.count { !it.isDuplicate })
    }

    @Test
    fun differentAccountsDoNotMatch() = runTest {
        val first = parse(account = "Карта 1").single()
        store.insert(TransactionData(money = first.money, type = first.type, categoryId = 1,
            description = first.description, date = first.date, sourceKey = first.sourceKey))
        assertFalse(parse(account = "Карта 2").single().isDuplicate)
    }

    @Test
    fun legacyTransactionsOfDifferentCurrencyDoNotMatch() = runTest {
        val first = parse().single()
        store.insert(TransactionData(money = Money(first.money.amountMinor, Currency.USD),
            type = first.type, categoryId = 1, description = first.description, date = first.date))
        assertFalse(parse().single().isDuplicate)
    }

    @Test
    fun unknownMerchantRequiresReview() = runTest {
        assertTrue(parse().single().needsReview)
        assertEquals(2L, parse().single().predictedCategoryId)
    }

    @Test
    fun classifierDoesNotLearnFromUnconfirmedImports() {
        val classifier = kz.abdiyev.walletai.ml.TransactionClassifier()
        val model = classifier.train(listOf(TransactionData(
            money = Money(100, Currency.KZT), type = TransactionType.EXPENSE,
            categoryId = 1, description = "ИП Сергеев", date = 0, isAutoCategorized = true
        )), categories)
        assertTrue(model.categoryPriors.isEmpty())
    }

    @Test
    fun consistentConfirmedHistorySuggestsCategoryForNextPurchase() = runTest {
        val first = parse().single()
        repeat(3) { index ->
            store.insert(TransactionData(money = first.money, type = first.type, categoryId = 1,
                description = "Арзан", date = first.date - (index + 1) * 86400000L,
                merchantKey = first.merchantKey))
        }
        val next = parse().single()
        assertEquals(1L, next.predictedCategoryId)
        assertFalse(next.needsReview)
        assertFalse(next.categoryConfirmed)
    }
}

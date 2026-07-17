package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.usecase.ImportStatementResult
import kz.abdiyev.walletai.domain.usecase.ImportStatementUseCase
import kz.abdiyev.walletai.domain.usecase.SaveTransactionRequest
import kz.abdiyev.walletai.domain.usecase.SaveTransactionResult
import kz.abdiyev.walletai.domain.usecase.SaveTransactionUseCase
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.statement.StatementFixtures
import kz.abdiyev.walletai.test.FakeMerchantRuleStore
import kz.abdiyev.walletai.test.FakeTransactionStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaveTransactionUseCaseTest {

    private val transactions = FakeTransactionStore()
    private val merchantRules = FakeMerchantRuleStore()
    private val useCase = SaveTransactionUseCase(transactions, merchantRules, TransactionClassifier())

    @Test
    fun rejectsNonPositiveAmount() = runTest {
        val result = useCase.execute(
            SaveTransactionRequest(
                money = Money.fromMajor(0.0, Currency.KZT),
                type = TransactionType.EXPENSE,
                categoryId = 1,
                description = "test",
                date = 1_000L
            )
        )
        assertEquals(SaveTransactionResult.InvalidAmount, result)
    }

    @Test
    fun persistsMerchantRuleWhenRequested() = runTest {
        useCase.execute(
            SaveTransactionRequest(
                money = Money.fromMajor(100.0, Currency.KZT),
                type = TransactionType.EXPENSE,
                categoryId = 2,
                description = "Magnum",
                date = 1_000L,
                persistMerchantRule = true
            )
        )
        assertEquals(2L, merchantRules.savedRules["magnum"])
    }
}

class ImportStatementUseCaseTest {

    private val transactions = FakeTransactionStore()
    private val merchantRules = FakeMerchantRuleStore()
    private val useCase = ImportStatementUseCase(transactions, merchantRules, BankStatementParser())

    private val categories = listOf(
        CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "cart"),
        CategoryData(10, "Переводы", TransactionType.EXPENSE, "#B0BEC5", "swap", isSystem = true)
    )

    @Test
    fun returnsEmptyForBlankText() = runTest {
        val result = useCase.execute("", categories, Currency.KZT, null, 1_000L)
        assertEquals(ImportStatementResult.Empty, result)
    }

    @Test
    fun parsesKaspiFixture() = runTest {
        val result = useCase.execute(
            text = StatementFixtures.KASPI_RAW,
            categories = categories,
            currency = Currency.KZT,
            trainedModel = null,
            defaultDateMillis = 1_780_000_000_000L
        )
        assertTrue(result is ImportStatementResult.Success)
        assertTrue((result as ImportStatementResult.Success).previews.isNotEmpty())
    }
}

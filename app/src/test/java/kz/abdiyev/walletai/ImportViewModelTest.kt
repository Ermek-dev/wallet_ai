package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.usecase.ImportStatementUseCase
import kz.abdiyev.walletai.domain.usecase.SaveTransactionUseCase
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.test.FakeCategoryRepository
import kz.abdiyev.walletai.test.FakeMerchantRuleRepository
import kz.abdiyev.walletai.test.FakeSettingsRepository
import kz.abdiyev.walletai.test.FakeTransactionRepository
import kz.abdiyev.walletai.ui.viewmodel.ImportUiState
import kz.abdiyev.walletai.ui.viewmodel.ImportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var transactionRepo: FakeTransactionRepository
    private lateinit var viewModel: ImportViewModel

    private val categories = listOf(
        kz.abdiyev.walletai.domain.model.CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "cart"),
        kz.abdiyev.walletai.domain.model.CategoryData(10, "Переводы", TransactionType.EXPENSE, "#B0BEC5", "swap", isSystem = true)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        transactionRepo = FakeTransactionRepository()
        viewModel = ImportViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = FakeCategoryRepository(categories),
            settingsRepository = FakeSettingsRepository(),
            merchantRuleRepository = FakeMerchantRuleRepository(),
            classifier = TransactionClassifier(),
            importStatementUseCase = ImportStatementUseCase(
                transactionRepo,
                FakeMerchantRuleRepository(),
                BankStatementParser()
            ),
            saveTransactionUseCase = SaveTransactionUseCase(
                transactionRepo,
                FakeMerchantRuleRepository(),
                TransactionClassifier()
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun marksDuplicateAgainstExistingTransactions() = runTest {
        transactionRepo.insert(
            TransactionData(
                money = Money.fromMajor(470.0, Currency.KZT),
                type = TransactionType.INCOME,
                categoryId = 1,
                description = "Сұлтан С.",
                date = kz.abdiyev.walletai.util.StatementDate.kaspiDate(30, 6, 26)
            )
        )
        advanceUntilIdle()

        viewModel.parseBankStatement(
            """
            www.kaspi.kz
            30.06.26 + 470,00 ₸ Пополнение Сұлтан С.
            30.06.26 - 15,00 ₸ Покупка Живая Вода
            """.trimIndent()
        )
        advanceUntilIdle()

        val previews = viewModel.parsedPreviews.value
        assertEquals(2, previews.size)
        assertTrue(previews.first { it.description.contains("Сұлтан") }.isDuplicate)
        assertEquals(1, (viewModel.uiState.value as ImportUiState.Preview).summary.duplicateCount)
    }

    @Test
    fun skipTransfersExcludesInternalTransfersFromCommit() = runTest {
        viewModel.parseBankStatement(
            """
            www.kaspi.kz
            30.06.26 + 265,00 ₸ Пополнение С карты другого банка
            29.06.26 - 15,00 ₸ Покупка Живая Вода
            """.trimIndent()
        )
        advanceUntilIdle()

        viewModel.setSkipTransfers(true)
        val committable = viewModel.committablePreviews()
        assertEquals(1, committable.size)
        assertTrue(committable.first().description.contains("Живая Вода"))
    }

    @Test
    fun commitPreviews_emitsSuccessWithSavedCount() = runTest {
        viewModel.parseBankStatement(
            """
            www.kaspi.kz
            29.06.26 - 15,00 ₸ Покупка Живая Вода
            """.trimIndent()
        )
        advanceUntilIdle()

        viewModel.commitPreviews()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ImportUiState.Success)
        assertEquals(1, (state as ImportUiState.Success).savedCount)
        assertTrue(viewModel.parsedPreviews.value.isEmpty())
        assertEquals(1, transactionRepo.getAll().size)
    }

    @Test
    fun acknowledgeSuccess_returnsToIdle() = runTest {
        viewModel.parseBankStatement(
            """
            www.kaspi.kz
            29.06.26 - 15,00 ₸ Покупка Живая Вода
            """.trimIndent()
        )
        advanceUntilIdle()
        viewModel.commitPreviews()
        advanceUntilIdle()

        viewModel.acknowledgeSuccess()
        assertEquals(ImportUiState.Idle, viewModel.uiState.value)
    }
}

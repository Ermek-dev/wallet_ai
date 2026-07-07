package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.ml.VoiceInputParser
import kz.abdiyev.walletai.test.FakeCategoryRepository
import kz.abdiyev.walletai.test.FakeMerchantRuleRepository
import kz.abdiyev.walletai.test.FakeSettingsRepository
import kz.abdiyev.walletai.test.FakeTransactionRepository
import kz.abdiyev.walletai.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transactionRepo: FakeTransactionRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        transactionRepo = FakeTransactionRepository()
        viewModel = DashboardViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = FakeCategoryRepository(),
            settingsRepository = FakeSettingsRepository(),
            merchantRuleRepository = FakeMerchantRuleRepository(),
            classifier = TransactionClassifier(),
            bankStatementParser = BankStatementParser(),
            voiceInputParser = VoiceInputParser()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addTransaction_insertsIntoRepository() = runTest {
        advanceUntilIdle()
        viewModel.addTransaction(
            money = Money.fromMajor(1500.0, Currency.KZT),
            type = TransactionType.EXPENSE,
            categoryId = 1,
            description = "Magnum",
            date = 1_000L
        )
        advanceUntilIdle()
        val saved = transactionRepo.getAll()
        assertEquals(1, saved.size)
        assertEquals("Magnum", saved.first().description)
    }

    @Test
    fun deleteThenUndo_restoresTransaction() = runTest {
        advanceUntilIdle()
        val tx = TransactionData(
            id = 1,
            money = Money.fromMajor(500.0, Currency.KZT),
            type = TransactionType.EXPENSE,
            categoryId = 1,
            description = "Test",
            date = 1_000L
        )
        transactionRepo.insert(tx)
        advanceUntilIdle()

        viewModel.deleteTransaction(tx)
        advanceUntilIdle()
        assertEquals(0, transactionRepo.getAll().size)

        viewModel.undoDelete()
        advanceUntilIdle()
        assertEquals(1, transactionRepo.getAll().size)
        assertNotNull(transactionRepo.getAll().first().id)
    }

    @Test
    fun defaultCategoryId_returnsLastUsedCategory() = runTest {
        advanceUntilIdle()
        val categories = FakeCategoryRepository().getAll()
        viewModel.addTransaction(
            money = Money.fromMajor(100.0, Currency.KZT),
            type = TransactionType.EXPENSE,
            categoryId = 1,
            description = "A",
            date = 1_000L
        )
        advanceUntilIdle()
        assertEquals(1L, viewModel.defaultCategoryId(TransactionType.EXPENSE, categories))
    }
}

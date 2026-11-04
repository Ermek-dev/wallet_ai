package kz.abdiyev.walletai

import kz.abdiyev.walletai.ai.AdviceResult
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.test.FakeAdviceRepository
import kz.abdiyev.walletai.test.FakeCategoryRepository
import kz.abdiyev.walletai.test.FakeSettingsRepository
import kz.abdiyev.walletai.test.FakeTransactionRepository
import kz.abdiyev.walletai.ui.viewmodel.AdvisorStats
import kz.abdiyev.walletai.ui.viewmodel.AdvisorUiState
import kz.abdiyev.walletai.ui.viewmodel.AdvisorViewModel
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdvisorViewModelTest {

    @Test
    fun loadAiFinancialAdvice_emitsAiResult() = runTest {
        val viewModel = AdvisorViewModel(
            adviceRepository = FakeAdviceRepository(
                generateResult = AdviceResult.Ai("AI response")
            ),
            transactionRepository = FakeTransactionRepository(),
            categoryRepository = FakeCategoryRepository(),
            settingsRepository = FakeSettingsRepository()
        )

        viewModel.advisorState.test {
            assertEquals(AdvisorUiState.Idle, awaitItem())
            viewModel.loadAiFinancialAdvice()
            assertEquals(AdvisorUiState.Loading, awaitItem())
            val success = awaitItem()
            assertTrue(success is AdvisorUiState.Success)
            assertEquals(
                AdviceResult.Ai("AI response"),
                (success as AdvisorUiState.Success).result
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadLocalFinancialAdvice_emitsFallbackResult() = runTest {
        val viewModel = AdvisorViewModel(
            adviceRepository = FakeAdviceRepository(fallbackReason = "Ручной запуск"),
            transactionRepository = FakeTransactionRepository(),
            categoryRepository = FakeCategoryRepository(),
            settingsRepository = FakeSettingsRepository()
        )

        viewModel.advisorState.test {
            assertEquals(AdvisorUiState.Idle, awaitItem())
            viewModel.loadLocalFinancialAdvice()
            assertEquals(AdvisorUiState.Loading, awaitItem())
            val success = awaitItem()
            assertTrue(success is AdvisorUiState.Success)
            val result = (success as AdvisorUiState.Success).result
            assertTrue(result is AdviceResult.Fallback)
            assertEquals("Ручной запуск", (result as AdviceResult.Fallback).reason)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadAiFinancialAdvice_ignoresDuplicateCallWhileLoading() = runTest {
        val viewModel = AdvisorViewModel(
            adviceRepository = FakeAdviceRepository(
                generateResult = AdviceResult.Ai("AI response")
            ),
            transactionRepository = FakeTransactionRepository(),
            categoryRepository = FakeCategoryRepository(),
            settingsRepository = FakeSettingsRepository()
        )

        viewModel.advisorState.test {
            assertEquals(AdvisorUiState.Idle, awaitItem())
            viewModel.loadAiFinancialAdvice()
            assertEquals(AdvisorUiState.Loading, awaitItem())
            viewModel.loadAiFinancialAdvice()
            val success = awaitItem()
            assertTrue(success is AdvisorUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun computeStats_incomeGreaterThanExpense_returnsSavingsRate() {
        val currency = Currency.KZT
        val stats = AdvisorStats.compute(
            listOf(
                TransactionData(
                    money = Money.fromMajor(5000.0, currency),
                    type = TransactionType.INCOME,
                    categoryId = 1L,
                    description = "Salary",
                    date = 0L
                ),
                TransactionData(
                    money = Money.fromMajor(500.0, currency),
                    type = TransactionType.EXPENSE,
                    categoryId = 2L,
                    description = "Food",
                    date = 0L
                )
            ),
            currency
        )
        assertEquals(90, stats.savingsRatePercent)
    }

    @Test
    fun computeStats_zeroIncome_returnsZeroSavingsRate() {
        val currency = Currency.KZT
        val stats = AdvisorStats.compute(
            listOf(
                TransactionData(
                    money = Money.fromMajor(500.0, currency),
                    type = TransactionType.EXPENSE,
                    categoryId = 1L,
                    description = "Food",
                    date = 0L
                )
            ),
            currency
        )
        assertEquals(0, stats.savingsRatePercent)
    }

    @Test
    fun computeStats_expenseGreaterThanIncome_returnsZeroSavingsRate() {
        val currency = Currency.KZT
        val stats = AdvisorStats.compute(
            listOf(
                TransactionData(
                    money = Money.fromMajor(1000.0, currency),
                    type = TransactionType.INCOME,
                    categoryId = 1L,
                    description = "Salary",
                    date = 0L
                ),
                TransactionData(
                    money = Money.fromMajor(2000.0, currency),
                    type = TransactionType.EXPENSE,
                    categoryId = 2L,
                    description = "Rent",
                    date = 0L
                )
            ),
            currency
        )
        assertEquals(0, stats.savingsRatePercent)
    }

    @Test
    fun statsState_reflectsRepositoryTransactions() = runTest {
        val currency = Currency.KZT
        val transactionRepository = FakeTransactionRepository()
        transactionRepository.insert(
            TransactionData(
                money = Money.fromMajor(5000.0, currency),
                type = TransactionType.INCOME,
                categoryId = 1L,
                description = "Salary",
                date = 0L
            )
        )
        transactionRepository.insert(
            TransactionData(
                money = Money.fromMajor(500.0, currency),
                type = TransactionType.EXPENSE,
                categoryId = 2L,
                description = "Food",
                date = 0L
            )
        )
        val viewModel = AdvisorViewModel(
            adviceRepository = FakeAdviceRepository(),
            transactionRepository = transactionRepository,
            categoryRepository = FakeCategoryRepository(),
            settingsRepository = FakeSettingsRepository()
        )

        viewModel.statsState.test {
            skipItems(1) // initial empty snapshot from stateIn
            val stats = awaitItem()
            assertEquals(90, stats.savingsRatePercent)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

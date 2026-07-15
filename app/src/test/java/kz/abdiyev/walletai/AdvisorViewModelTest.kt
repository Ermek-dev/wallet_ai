package kz.abdiyev.walletai

import kz.abdiyev.walletai.ai.AdviceResult
import kz.abdiyev.walletai.test.FakeAdviceRepository
import kz.abdiyev.walletai.test.FakeCategoryRepository
import kz.abdiyev.walletai.test.FakeSettingsRepository
import kz.abdiyev.walletai.test.FakeTransactionRepository
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
}

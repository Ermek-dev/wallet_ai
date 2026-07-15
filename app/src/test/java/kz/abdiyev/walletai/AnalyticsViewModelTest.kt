package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.test.FakeCategoryRepository
import kz.abdiyev.walletai.test.FakeSettingsRepository
import kz.abdiyev.walletai.test.FakeTransactionRepository
import kz.abdiyev.walletai.ui.viewmodel.AnalyticsViewModel
import kz.abdiyev.walletai.ui.viewmodel.CategoryShareUi
import kz.abdiyev.walletai.util.CalendarMillis
import kz.abdiyev.walletai.util.Exporter
import kz.abdiyev.walletai.util.PeriodFilter
import kz.abdiyev.walletai.util.StatementDate
import kz.abdiyev.walletai.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var transactionRepo: FakeTransactionRepository
    private lateinit var categoryRepo: FakeCategoryRepository
    private lateinit var viewModel: AnalyticsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        transactionRepo = FakeTransactionRepository()
        categoryRepo = FakeCategoryRepository(
            listOf(
                CategoryData(1, "Еда", TransactionType.EXPENSE, "#EF5350", "restaurant"),
                CategoryData(2, "Дом", TransactionType.EXPENSE, "#42A5F5", "home")
            )
        )
        viewModel = AnalyticsViewModel(
            transactionRepository = transactionRepo,
            categoryRepository = categoryRepo,
            settingsRepository = FakeSettingsRepository(),
            exporter = Exporter()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aggregateTopShares_groupsRemainderIntoOther() {
        val shares = (1..10).map { index ->
            CategoryShareUi(
                categoryId = index.toLong(),
                name = "Cat$index",
                colorHex = "#FFFFFF",
                iconName = "paid",
                amountMinor = (11 - index) * 100L,
                percentage = (11 - index).toFloat()
            )
        }
        val aggregated = AnalyticsViewModel.aggregateTopShares(shares, maxVisible = 8)
        assertEquals(8, aggregated.size)
        assertEquals("Прочее", aggregated.last().name)
        assertEquals("", aggregated.last().iconName)
        assertEquals(600L, aggregated.last().amountMinor)
    }

    @Test
    fun categoryShares_includeIconNameFromCategory() = runTest {
        backgroundScope.launch { viewModel.uiState.collect { } }
        val july10 = StatementDate.toEpochMillis(2026, 7, 10)
        val offset = TimeUtils.localOffsetMillis(july10)
        val inRange = CalendarMillis.toLocalStartOfDay(july10, offset) + 3_600_000L
        val outOfRange = CalendarMillis.toLocalStartOfDay(StatementDate.toEpochMillis(2026, 6, 1), offset)
        transactionRepo.insert(
            TransactionData(
                money = Money.fromMajor(500.0, Currency.KZT),
                type = TransactionType.EXPENSE,
                categoryId = 1,
                description = "lunch",
                date = inRange
            )
        )
        transactionRepo.insert(
            TransactionData(
                money = Money.fromMajor(300.0, Currency.KZT),
                type = TransactionType.EXPENSE,
                categoryId = 2,
                description = "rent",
                date = outOfRange
            )
        )
        val range = PeriodFilter.customRangeFromLocalDays(
            july10,
            StatementDate.toEpochMillis(2026, 7, 12),
            offset
        )
        viewModel.applyCustomRange(range)
        advanceUntilIdle()

        val shares = viewModel.uiState.value.categoryShares
        assertEquals(1, shares.size)
        assertEquals("restaurant", shares.first().iconName)
        assertEquals(Period.CUSTOM, viewModel.uiState.value.period)
    }

    @Test
    fun applyCustomRange_updatesExportLabel() = runTest {
        backgroundScope.launch { viewModel.uiState.collect { } }
        val range = PeriodFilter.customRangeFromLocalDays(
            StatementDate.toEpochMillis(2026, 7, 10),
            StatementDate.toEpochMillis(2026, 7, 12),
            0L
        )
        viewModel.applyCustomRange(range)
        advanceUntilIdle()

        val label = viewModel.exportPeriodLabel()
        assertTrue(label.contains("2026"))
        assertTrue(label.contains("—"))
    }

    @Test
    fun setPeriod_clearsCustomRange() = runTest {
        backgroundScope.launch { viewModel.uiState.collect { } }
        val range = PeriodFilter.customRangeFromLocalDays(
            StatementDate.toEpochMillis(2026, 7, 10),
            StatementDate.toEpochMillis(2026, 7, 12),
            0L
        )
        viewModel.applyCustomRange(range)
        advanceUntilIdle()
        viewModel.setPeriod(Period.MONTH)
        advanceUntilIdle()

        assertEquals(Period.MONTH, viewModel.uiState.value.period)
        assertEquals(null, viewModel.uiState.value.customRange)
    }
}

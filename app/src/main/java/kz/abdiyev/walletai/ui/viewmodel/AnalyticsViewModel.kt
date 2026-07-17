package kz.abdiyev.walletai.ui.viewmodel

import androidx.compose.runtime.Immutable
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.domain.category.CategoryRollup
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.PeriodRange
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.util.Clock
import kz.abdiyev.walletai.util.Exporter
import kz.abdiyev.walletai.util.PeriodFilter
import kz.abdiyev.walletai.util.PeriodLabels
import kz.abdiyev.walletai.util.SystemClock
import kz.abdiyev.walletai.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class AnalyticsChartMode {
    EXPENSE,
    INCOME
}

@Immutable
data class CategoryShareUi(
    val categoryId: Long,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val amountMinor: Long,
    val percentage: Float,
    val isOther: Boolean = false,
    val subShares: List<CategoryShareUi> = emptyList()
)

@Immutable
data class AnalyticsUiState(
    val period: Period = Period.MONTH,
    val customRange: PeriodRange? = null,
    val chartMode: AnalyticsChartMode = AnalyticsChartMode.EXPENSE,
    val periodLabel: String = "",
    val periodRangeLabel: String = "",
    val heading: String = "",
    val totalExpenseMinor: Long = 0L,
    val totalIncomeMinor: Long = 0L,
    val balanceMinor: Long = 0L,
    val savingsRate: Float = 0f,
    val activeTotalMinor: Long = 0L,
    val categoryShares: List<CategoryShareUi> = emptyList(),
    val currencyCode: String = Currency.KZT.code,
    val hiddenByPeriodCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val exporter: Exporter,
    private val clock: Clock = SystemClock
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod.asStateFlow()

    private val _customRange = MutableStateFlow<PeriodRange?>(null)
    val customRange: StateFlow<PeriodRange?> = _customRange.asStateFlow()

    private val _chartMode = MutableStateFlow(AnalyticsChartMode.EXPENSE)
    val chartMode: StateFlow<AnalyticsChartMode> = _chartMode.asStateFlow()

    val uiState: StateFlow<AnalyticsUiState> = combine(
        transactionRepository.transactions,
        categoryRepository.categories,
        settingsRepository.settings,
        combine(_selectedPeriod, _customRange, _chartMode, ::Triple)
    ) { _, categories, settings, (period, customRange, chartMode) ->
        AnalyticsQuery(categories, settings, period, customRange, chartMode)
    }.mapLatest { query ->
        buildAnalyticsUiState(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun setPeriod(period: Period) {
        _selectedPeriod.value = period
        if (period != Period.CUSTOM) {
            _customRange.value = null
        }
    }

    fun applyCustomRange(range: PeriodRange) {
        _customRange.value = range
        _selectedPeriod.value = Period.CUSTOM
    }

    fun setChartMode(mode: AnalyticsChartMode) {
        _chartMode.value = mode
    }

    fun exportToCsv(context: Context, onComplete: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = uiState.value
            val txs = filteredTransactionsForPeriod(state.period, transactionRepository.getAll(), state.customRange)
            val file = exporter.exportToCsv(
                context,
                txs,
                categoryRepository.getAll(),
                Currency.fromCode(state.currencyCode)
            )
            onComplete(file)
        }
    }

    fun exportToPdf(context: Context, onComplete: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = uiState.value
            val txs = filteredTransactionsForPeriod(state.period, transactionRepository.getAll(), state.customRange)
            val file = exporter.exportToPdf(context, txs, categoryRepository.getAll())
            onComplete(file)
        }
    }

    fun exportPeriodLabel(): String {
        val state = uiState.value
        return PeriodLabels.exportLabel(state.period, customRange = state.customRange)
    }

    private fun filteredTransactionsForPeriod(
        period: Period,
        allTransactions: List<kz.abdiyev.walletai.domain.model.TransactionData>,
        customRange: PeriodRange? = _customRange.value
    ) = PeriodFilter.filter(
        allTransactions,
        period,
        clock.nowMillis(),
        TimeUtils.localOffsetMillis(clock.nowMillis()),
        customRange
    )

    private suspend fun buildAnalyticsUiState(query: AnalyticsQuery): AnalyticsUiState {
        val (categories, settings, period, customRange, chartMode) = query
        val now = clock.nowMillis()
        val offset = TimeUtils.localOffsetMillis(now)
        val bounds = PeriodFilter.bounds(period, now, offset, customRange)
        val startInclusive = bounds?.startInclusive
        val endExclusive = bounds?.endExclusive

        val totalExpenseMinor = transactionRepository.sumByType(
            TransactionType.EXPENSE,
            startInclusive,
            endExclusive
        )
        val totalIncomeMinor = transactionRepository.sumByType(
            TransactionType.INCOME,
            startInclusive,
            endExclusive
        )
        val balanceMinor = totalIncomeMinor - totalExpenseMinor
        val savingsRate = if (totalIncomeMinor > 0) {
            balanceMinor.toFloat() / totalIncomeMinor.toFloat() * 100f
        } else {
            0f
        }

        val activeType = when (chartMode) {
            AnalyticsChartMode.EXPENSE -> TransactionType.EXPENSE
            AnalyticsChartMode.INCOME -> TransactionType.INCOME
        }
        val activeTotalMinor = transactionRepository.sumByType(activeType, startInclusive, endExclusive)
        val amountsByCategory = transactionRepository.sumByCategoryGrouped(
            activeType,
            startInclusive,
            endExclusive
        )

        val rawShares = if (activeTotalMinor == 0L) {
            emptyList()
        } else {
            CategoryRollup.rollup(amountsByCategory, categories).map { rolled ->
                val category = categories.find { it.id == rolled.rootCategoryId }
                    ?: CategoryData(
                        id = rolled.rootCategoryId,
                        name = "Другое",
                        type = activeType,
                        colorHex = "#757575",
                        iconName = ""
                    )
                val subShares = rolled.subShares.mapNotNull { sub ->
                    if (sub.categoryId == rolled.rootCategoryId && rolled.subShares.size == 1) return@mapNotNull null
                    val subCategory = categories.find { it.id == sub.categoryId } ?: return@mapNotNull null
                    CategoryShareUi(
                        categoryId = subCategory.id,
                        name = subCategory.name,
                        colorHex = subCategory.colorHex,
                        iconName = subCategory.iconName,
                        amountMinor = sub.amountMinor,
                        percentage = sub.amountMinor.toFloat() / activeTotalMinor.toFloat() * 100f
                    )
                }
                CategoryShareUi(
                    categoryId = category.id,
                    name = category.name,
                    colorHex = category.colorHex,
                    iconName = category.iconName,
                    amountMinor = rolled.amountMinor,
                    percentage = rolled.amountMinor.toFloat() / activeTotalMinor.toFloat() * 100f,
                    subShares = subShares
                )
            }
        }

        val hiddenByPeriodCount = if (period == Period.ALL || bounds == null) {
            0
        } else {
            transactionRepository.countAll() -
                transactionRepository.countInPeriod(bounds.startInclusive, bounds.endExclusive)
        }

        return AnalyticsUiState(
            period = period,
            customRange = customRange,
            chartMode = chartMode,
            periodLabel = PeriodLabels.title(period, now, customRange),
            periodRangeLabel = PeriodLabels.rangeLabel(period, now, customRange),
            heading = PeriodLabels.analyticsHeading(period, chartMode == AnalyticsChartMode.EXPENSE, now, customRange),
            totalExpenseMinor = totalExpenseMinor,
            totalIncomeMinor = totalIncomeMinor,
            balanceMinor = balanceMinor,
            savingsRate = savingsRate,
            activeTotalMinor = activeTotalMinor,
            categoryShares = aggregateTopShares(rawShares),
            currencyCode = settings.currencyCode,
            hiddenByPeriodCount = hiddenByPeriodCount
        )
    }

    private data class AnalyticsQuery(
        val categories: List<CategoryData>,
        val settings: Settings,
        val period: Period,
        val customRange: PeriodRange?,
        val chartMode: AnalyticsChartMode
    )

    companion object {
        private const val MAX_VISIBLE_CATEGORIES = 8

        fun aggregateTopShares(shares: List<CategoryShareUi>, maxVisible: Int = MAX_VISIBLE_CATEGORIES): List<CategoryShareUi> {
            if (shares.size <= maxVisible) return shares
            val top = shares.take(maxVisible - 1)
            val remainder = shares.drop(maxVisible - 1)
            val otherAmount = remainder.sumOf { it.amountMinor }
            val otherPct = remainder.sumOf { it.percentage.toDouble() }.toFloat()
            return top + CategoryShareUi(
                categoryId = 0L,
                name = "Прочее",
                colorHex = "#9E9E9E",
                iconName = "",
                amountMinor = otherAmount,
                percentage = otherPct,
                isOther = true
            )
        }
    }
}

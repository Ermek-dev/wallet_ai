package kz.abdiyev.walletai.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.PeriodRange
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.util.Exporter
import kz.abdiyev.walletai.util.PeriodFilter
import kz.abdiyev.walletai.util.PeriodLabels
import kz.abdiyev.walletai.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class AnalyticsChartMode {
    EXPENSE,
    INCOME
}

data class CategoryShareUi(
    val categoryId: Long,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val amountMinor: Long,
    val percentage: Float,
    val isOther: Boolean = false
)

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

class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val exporter: Exporter
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod.asStateFlow()

    private val _customRange = MutableStateFlow<PeriodRange?>(null)
    val customRange: StateFlow<PeriodRange?> = _customRange.asStateFlow()

    private val _chartMode = MutableStateFlow(AnalyticsChartMode.EXPENSE)
    val chartMode: StateFlow<AnalyticsChartMode> = _chartMode.asStateFlow()

    val uiState: StateFlow<AnalyticsUiState> = combine(
        combine(
            transactionRepository.transactions,
            categoryRepository.categories,
            settingsRepository.settings,
            ::Triple
        ),
        combine(_selectedPeriod, _customRange, _chartMode, ::Triple)
    ) { (txs, categories, settings), (period, customRange, chartMode) ->
        buildAnalyticsUiState(txs, categories, settings, period, customRange, chartMode)
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
        System.currentTimeMillis(),
        TimeUtils.localOffsetMillis(),
        customRange
    )

    private fun buildAnalyticsUiState(
        transactions: List<kz.abdiyev.walletai.domain.model.TransactionData>,
        categories: List<CategoryData>,
        settings: Settings,
        period: Period,
        customRange: PeriodRange?,
        chartMode: AnalyticsChartMode
    ): AnalyticsUiState {
        val now = System.currentTimeMillis()
        val offset = TimeUtils.localOffsetMillis(now)
        val filtered = PeriodFilter.filter(transactions, period, now, offset, customRange)
        val expenses = filtered.filter { it.type == TransactionType.EXPENSE }
        val incomes = filtered.filter { it.type == TransactionType.INCOME }
        val totalExpenseMinor = expenses.sumOf { it.money.amountMinor }
        val totalIncomeMinor = incomes.sumOf { it.money.amountMinor }
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
        val activeTransactions = filtered.filter { it.type == activeType }
        val activeTotalMinor = activeTransactions.sumOf { it.money.amountMinor }
        val rawShares = if (activeTotalMinor == 0L) {
            emptyList()
        } else {
            activeTransactions.groupBy { it.categoryId }.map { (catId, txs) ->
                val category = categories.find { it.id == catId }
                    ?: CategoryData(id = catId, name = "Другое", type = activeType, colorHex = "#757575", iconName = "")
                val sumMinor = txs.sumOf { it.money.amountMinor }
                CategoryShareUi(
                    categoryId = category.id,
                    name = category.name,
                    colorHex = category.colorHex,
                    iconName = category.iconName,
                    amountMinor = sumMinor,
                    percentage = sumMinor.toFloat() / activeTotalMinor.toFloat() * 100f
                )
            }.sortedByDescending { it.amountMinor }
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
            hiddenByPeriodCount = PeriodFilter.outsideCount(transactions, period, now, offset, customRange)
        )
    }

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

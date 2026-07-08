package kz.abdiyev.walletai.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.util.Exporter
import kz.abdiyev.walletai.util.PeriodFilter
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

class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val exporter: Exporter
) : ViewModel() {

    val categories = categoryRepository.categories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val transactions = transactionRepository.transactions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Lazily, Settings())

    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod.asStateFlow()

    private val _chartMode = MutableStateFlow(AnalyticsChartMode.EXPENSE)
    val chartMode: StateFlow<AnalyticsChartMode> = _chartMode.asStateFlow()

    val filteredTransactions = combine(transactions, _selectedPeriod) { txs, period ->
        PeriodFilter.filter(txs, period, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setPeriod(period: Period) {
        _selectedPeriod.value = period
    }

    fun setChartMode(mode: AnalyticsChartMode) {
        _chartMode.value = mode
    }

    fun exportToCsv(context: Context, onComplete: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = exporter.exportToCsv(
                context,
                transactionRepository.getAll(),
                categoryRepository.getAll(),
                Currency.fromCode(settings.value.currencyCode)
            )
            onComplete(file)
        }
    }

    fun exportToPdf(context: Context, onComplete: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = exporter.exportToPdf(context, transactionRepository.getAll(), categoryRepository.getAll())
            onComplete(file)
        }
    }
}

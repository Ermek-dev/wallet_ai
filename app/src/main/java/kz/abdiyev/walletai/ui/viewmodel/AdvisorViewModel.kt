package kz.abdiyev.walletai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.ai.AdviceResult
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.repository.AdviceRepository
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdvisorStats(
    val income: Money,
    val expense: Money,
    val savingsRatePercent: Int
) {
    companion object {
        fun compute(transactions: List<TransactionData>, currency: Currency): AdvisorStats {
            val incomeMinor = transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.money.amountMinor }
            val expenseMinor = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.money.amountMinor }
            val savingsRatePercent = if (incomeMinor > 0) {
                ((incomeMinor - expenseMinor).coerceAtLeast(0) * 100 / incomeMinor).toInt()
            } else {
                0
            }
            return AdvisorStats(
                income = Money(incomeMinor, currency),
                expense = Money(expenseMinor, currency),
                savingsRatePercent = savingsRatePercent
            )
        }
    }
}

sealed class AdvisorUiState {
    data object Idle : AdvisorUiState()
    data object Loading : AdvisorUiState()
    data class Success(val result: AdviceResult) : AdvisorUiState()
    data class Error(val message: String) : AdvisorUiState()
}

class AdvisorViewModel(
    private val adviceRepository: AdviceRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Lazily, Settings())
    private val _advisorState = MutableStateFlow<AdvisorUiState>(AdvisorUiState.Idle)
    val advisorState: StateFlow<AdvisorUiState> = _advisorState.asStateFlow()

    val statsState: StateFlow<AdvisorStats> = combine(
        transactionRepository.transactions,
        settingsRepository.settings
    ) { transactions, settings ->
        AdvisorStats.compute(transactions, Currency.fromCode(settings.currencyCode))
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AdvisorStats.compute(emptyList(), Currency.KZT)
    )

    fun loadAiFinancialAdvice(forceRefresh: Boolean = false) {
        if (_advisorState.value is AdvisorUiState.Loading) return
        _advisorState.value = AdvisorUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = adviceRepository.generateAdvice(
                    transactionRepository.getAll(),
                    categoryRepository.getAll(),
                    settings.value.currencyCode,
                    forceRefresh = forceRefresh
                )
                _advisorState.value = AdvisorUiState.Success(result)
            } catch (e: Exception) {
                _advisorState.value = AdvisorUiState.Error(e.message ?: "Ошибка")
            }
        }
    }

    fun refreshAiFinancialAdvice() {
        loadAiFinancialAdvice(forceRefresh = true)
    }

    fun loadLocalFinancialAdvice() {
        if (_advisorState.value is AdvisorUiState.Loading) return
        _advisorState.value = AdvisorUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val result = adviceRepository.localAdvice(
                transactionRepository.getAll(),
                categoryRepository.getAll(),
                settings.value.currencyCode,
                "Ручной запуск"
            )
            _advisorState.value = AdvisorUiState.Success(result)
        }
    }
}

package kz.abdiyev.walletai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.domain.repository.AdviceRepository
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AdvisorUiState {
    data object Idle : AdvisorUiState()
    data object Loading : AdvisorUiState()
    data class Success(val advice: String) : AdvisorUiState()
    data class Error(val message: String) : AdvisorUiState()
}

class AdvisorViewModel(
    private val adviceRepository: AdviceRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val currencyCode = settingsRepository.currencyCode.stateIn(viewModelScope, SharingStarted.Lazily, "KZT")
    private val _advisorState = MutableStateFlow<AdvisorUiState>(AdvisorUiState.Idle)
    val advisorState: StateFlow<AdvisorUiState> = _advisorState.asStateFlow()

    fun loadAiFinancialAdvice() {
        _advisorState.value = AdvisorUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val advice = adviceRepository.generateAdvice(
                    transactionRepository.getAll(),
                    categoryRepository.getAll(),
                    currencyCode.value
                )
                _advisorState.value = AdvisorUiState.Success(advice)
            } catch (e: Exception) {
                _advisorState.value = AdvisorUiState.Error(e.message ?: "Ошибка")
            }
        }
    }

    fun loadLocalFinancialAdvice() {
        _advisorState.value = AdvisorUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val advice = adviceRepository.getFallbackAdvice(
                transactionRepository.getAll(),
                categoryRepository.getAll(),
                currencyCode.value,
                "Ручной запуск"
            )
            _advisorState.value = AdvisorUiState.Success(advice)
        }
    }
}

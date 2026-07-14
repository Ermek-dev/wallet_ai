package kz.abdiyev.walletai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.ai.AdviceResult
import kz.abdiyev.walletai.domain.model.Settings
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

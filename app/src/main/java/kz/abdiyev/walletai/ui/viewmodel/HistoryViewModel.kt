package kz.abdiyev.walletai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val categories = categoryRepository.categories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Lazily, Settings())

    private val _historyQuery = MutableStateFlow("")
    val historyQuery: StateFlow<String> = _historyQuery.asStateFlow()

    val searchResults = _historyQuery
        .flatMapLatest { query -> transactionRepository.search(query) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setHistoryQuery(query: String) {
        _historyQuery.value = query
    }
}

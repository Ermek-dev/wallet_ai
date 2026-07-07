package kz.abdiyev.walletai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.data.database.AppDatabase
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.notification.ReminderScheduler
import kz.abdiyev.walletai.util.MockDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val database: AppDatabase
) : ViewModel() {

    val currencyCode = settingsRepository.currencyCode.stateIn(viewModelScope, SharingStarted.Lazily, Currency.KZT.code)
    val userName = settingsRepository.userName.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val themeMode = settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.Lazily, "system")
    val notificationsEnabled = settingsRepository.notificationsEnabled.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val notificationHour = settingsRepository.notificationHour.stateIn(viewModelScope, SharingStarted.Lazily, 21)
    val biometricEnabled = settingsRepository.biometricEnabled.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val categories = categoryRepository.categories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _historyQuery = MutableStateFlow("")
    val historyQuery: StateFlow<String> = _historyQuery.asStateFlow()

    val searchResults = _historyQuery
        .flatMapLatest { query -> transactionRepository.search(query) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setHistoryQuery(query: String) {
        _historyQuery.value = query
    }

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    fun clearSaveMessage() {
        _saveMessage.value = null
    }

    fun setCurrency(code: String) {
        Timber.d("setCurrency: %s", code)
        viewModelScope.launch {
            try {
                settingsRepository.setCurrency(code)
                _saveMessage.value = "saved"
            } catch (e: Exception) {
                Timber.e(e, "Failed to set currency")
            }
        }
    }

    fun setUserName(name: String) {
        Timber.d("setUserName: %s", name)
        viewModelScope.launch {
            try {
                settingsRepository.setUserName(name)
                _saveMessage.value = "saved"
            } catch (e: Exception) {
                Timber.e(e, "Failed to set user name")
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean, scheduler: ReminderScheduler) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            if (enabled) scheduler.scheduleDailyReminder(settingsRepository.notificationHour.stateIn(viewModelScope).value)
            else scheduler.cancel()
        }
    }

    fun setNotificationHour(hour: Int, scheduler: ReminderScheduler) {
        viewModelScope.launch {
            settingsRepository.setNotificationHour(hour)
            if (settingsRepository.notificationsEnabled.stateIn(viewModelScope).value) {
                scheduler.scheduleDailyReminder(hour)
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricEnabled(enabled) }
    }

    fun saveCategory(category: CategoryData) {
        viewModelScope.launch(Dispatchers.IO) {
            if (category.id == 0L) categoryRepository.insert(category)
            else categoryRepository.update(category)
        }
    }

    fun deleteCategory(category: CategoryData) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.delete(category)
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            MockDataProvider.loadDemoTransactions(database.categoryDao(), database.transactionDao())
            settingsRepository.setDemoLoaded(true)
        }
    }

    fun deleteAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.deleteAll()
        }
    }
}

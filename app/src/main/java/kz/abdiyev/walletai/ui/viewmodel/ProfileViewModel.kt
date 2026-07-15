package kz.abdiyev.walletai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.ThemeMode
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.MaintenanceRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.notification.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class ProfileViewModel(
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val maintenanceRepository: MaintenanceRepository
) : ViewModel() {

    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Lazily, Settings())
    val categories = categoryRepository.categories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    fun clearSaveMessage() {
        _saveMessage.value = null
    }

    fun setCurrency(code: String) {
        Timber.d("setCurrency: %s", code)
        viewModelScope.launch {
            try {
                settingsRepository.update { it.copy(currencyCode = code) }
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
                settingsRepository.update { it.copy(userName = name) }
                _saveMessage.value = "saved"
            } catch (e: Exception) {
                Timber.e(e, "Failed to set user name")
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.update { it.copy(themeMode = mode) } }
    }

    fun setNotificationsEnabled(enabled: Boolean, scheduler: ReminderScheduler) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                if (enabled) {
                    scheduler.scheduleDailyReminder(current.notificationHour, current.notificationMinute)
                } else {
                    scheduler.cancel()
                }
                current.copy(notificationsEnabled = enabled)
            }
        }
    }

    fun setNotificationTime(hour: Int, minute: Int, scheduler: ReminderScheduler) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                val updated = current.copy(notificationHour = hour, notificationMinute = minute)
                if (updated.notificationsEnabled) {
                    scheduler.scheduleDailyReminder(hour, minute)
                }
                updated
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.update { it.copy(biometricEnabled = enabled) } }
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
            maintenanceRepository.loadDemoData()
            settingsRepository.update { it.copy(demoLoaded = true) }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            maintenanceRepository.wipeAllData()
        }
    }
}

package kz.abdiyev.walletai.test

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.MerchantRuleRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTransactionRepository : TransactionRepository {
    private val items = mutableListOf<TransactionData>()
    private val flow = MutableStateFlow<List<TransactionData>>(emptyList())

    override val transactions: Flow<List<TransactionData>> = flow

    override suspend fun getAll(): List<TransactionData> = items.toList()

    override fun search(query: String): Flow<List<TransactionData>> =
        flow.map { list -> list.filter { it.description.contains(query, ignoreCase = true) } }

    override suspend fun insert(transaction: TransactionData): Long {
        val id = if (transaction.id == 0L) (items.maxOfOrNull { it.id } ?: 0L) + 1 else transaction.id
        val saved = transaction.copy(id = id)
        items.add(saved)
        flow.value = items.sortedByDescending { it.date }
        return id
    }

    override suspend fun insertAll(transactions: List<TransactionData>) {
        transactions.forEach { insert(it) }
    }

    override suspend fun update(transaction: TransactionData) {
        val index = items.indexOfFirst { it.id == transaction.id }
        if (index >= 0) {
            items[index] = transaction
            flow.value = items.sortedByDescending { it.date }
        }
    }

    override suspend fun delete(transaction: TransactionData) {
        items.removeAll { it.id == transaction.id }
        flow.value = items.sortedByDescending { it.date }
    }

    override suspend fun deleteAll() {
        items.clear()
        flow.value = emptyList()
    }
}

class FakeCategoryRepository(
    initial: List<CategoryData> = listOf(
        CategoryData(1, "Продукты", kz.abdiyev.walletai.domain.model.TransactionType.EXPENSE, "#EF5350", "shopping_cart")
    )
) : CategoryRepository {
    private val items = initial.toMutableList()
    private val flow = MutableStateFlow(items.toList())

    override val categories: Flow<List<CategoryData>> = flow

    override suspend fun getAll(): List<CategoryData> = items.toList()

    override suspend fun ensureDefaultCategories() = Unit

    override suspend fun insert(category: CategoryData): Long {
        val id = (items.maxOfOrNull { it.id } ?: 0L) + 1
        items.add(category.copy(id = id))
        flow.value = items.toList()
        return id
    }

    override suspend fun update(category: CategoryData) = Unit

    override suspend fun delete(category: CategoryData) = Unit

    override suspend fun getOtherCategory(): CategoryData? = null
}

class FakeSettingsRepository : SettingsRepository {
    private val _currency = MutableStateFlow("KZT")
    private val _userName = MutableStateFlow("")
    private val _theme = MutableStateFlow("system")
    private val _onboarding = MutableStateFlow(true)
    private val _demoLoaded = MutableStateFlow(false)
    private val _notifications = MutableStateFlow(false)
    private val _notificationHour = MutableStateFlow(21)
    private val _biometric = MutableStateFlow(false)

    override val currencyCode: Flow<String> = _currency
    override val userName: Flow<String> = _userName
    override val themeMode: Flow<String> = _theme
    override val onboardingDone: Flow<Boolean> = _onboarding
    override val demoLoaded: Flow<Boolean> = _demoLoaded
    override val notificationsEnabled: Flow<Boolean> = _notifications
    override val notificationHour: Flow<Int> = _notificationHour
    override val biometricEnabled: Flow<Boolean> = _biometric

    override suspend fun setCurrency(code: String) { _currency.value = code }
    override suspend fun setUserName(name: String) { _userName.value = name }
    override suspend fun setThemeMode(mode: String) { _theme.value = mode }
    override suspend fun setOnboardingDone(done: Boolean) { _onboarding.value = done }
    override suspend fun setDemoLoaded(loaded: Boolean) { _demoLoaded.value = loaded }
    override suspend fun setNotificationsEnabled(enabled: Boolean) { _notifications.value = enabled }
    override suspend fun setNotificationHour(hour: Int) { _notificationHour.value = hour }
    override suspend fun setBiometricEnabled(enabled: Boolean) { _biometric.value = enabled }
}

class FakeMerchantRuleRepository : MerchantRuleRepository {
    override suspend fun findCategoryId(description: String): Long? = null
    override suspend fun saveRule(merchantPattern: String, categoryId: Long) = Unit
}

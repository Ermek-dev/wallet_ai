package kz.abdiyev.walletai.domain.repository

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.TransactionData
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    val transactions: Flow<List<TransactionData>>
    suspend fun getAll(): List<TransactionData>
    fun search(query: String): Flow<List<TransactionData>>
    suspend fun insert(transaction: TransactionData): Long
    suspend fun insertAll(transactions: List<TransactionData>)
    suspend fun update(transaction: TransactionData)
    suspend fun delete(transaction: TransactionData)
    suspend fun deleteAll()
}

interface CategoryRepository {
    val categories: Flow<List<CategoryData>>
    suspend fun getAll(): List<CategoryData>
    suspend fun ensureDefaultCategories()
    suspend fun insert(category: CategoryData): Long
    suspend fun update(category: CategoryData)
    suspend fun delete(category: CategoryData)
    suspend fun getOtherCategory(): CategoryData?
}

interface SettingsRepository {
    val currencyCode: Flow<String>
    val userName: Flow<String>
    val themeMode: Flow<String>
    val onboardingDone: Flow<Boolean>
    val demoLoaded: Flow<Boolean>
    val notificationsEnabled: Flow<Boolean>
    val notificationHour: Flow<Int>
    val biometricEnabled: Flow<Boolean>
    suspend fun setCurrency(code: String)
    suspend fun setUserName(name: String)
    suspend fun setThemeMode(mode: String)
    suspend fun setOnboardingDone(done: Boolean)
    suspend fun setDemoLoaded(loaded: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setNotificationHour(hour: Int)
    suspend fun setBiometricEnabled(enabled: Boolean)
}

interface AdviceRepository {
    suspend fun generateAdvice(transactions: List<TransactionData>, categories: List<CategoryData>, currencyCode: String): String
    fun getFallbackAdvice(transactions: List<TransactionData>, categories: List<CategoryData>, currencyCode: String, reason: String): String
}

interface MerchantRuleRepository {
    suspend fun findCategoryId(description: String): Long?
    suspend fun saveRule(merchantPattern: String, categoryId: Long)
}

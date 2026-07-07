package kz.abdiyev.walletai.data.repository

import kz.abdiyev.walletai.data.database.CategoryDao
import kz.abdiyev.walletai.data.database.MerchantRuleDao
import kz.abdiyev.walletai.data.database.MerchantRuleEntity
import kz.abdiyev.walletai.data.database.TransactionDao
import kz.abdiyev.walletai.data.mapper.toDomain
import kz.abdiyev.walletai.data.mapper.toEntity
import kz.abdiyev.walletai.data.preferences.SettingsDataStore
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.repository.AdviceRepository
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.MerchantRuleRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.service.GeminiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val dao: TransactionDao
) : TransactionRepository {
    override val transactions: Flow<List<TransactionData>> =
        dao.getAllTransactions().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<TransactionData> =
        dao.getAllTransactionsList().map { it.toDomain() }

    override fun search(query: String): Flow<List<TransactionData>> =
        dao.searchTransactions(query).map { list -> list.map { it.toDomain() } }

    override suspend fun insert(transaction: TransactionData): Long =
        dao.insertTransaction(transaction.toEntity())

    override suspend fun insertAll(transactions: List<TransactionData>) {
        dao.insertTransactions(transactions.map { it.toEntity() })
    }

    override suspend fun update(transaction: TransactionData) {
        dao.updateTransaction(transaction.toEntity())
    }

    override suspend fun delete(transaction: TransactionData) {
        dao.deleteTransaction(transaction.toEntity())
    }

    override suspend fun deleteAll() {
        dao.deleteAllTransactions()
    }
}

class CategoryRepositoryImpl(
    private val dao: CategoryDao,
    private val transactionDao: TransactionDao
) : CategoryRepository {
    override val categories: Flow<List<CategoryData>> =
        dao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<CategoryData> =
        dao.getAllCategoriesList().map { it.toDomain() }

    override suspend fun ensureDefaultCategories() {
        kz.abdiyev.walletai.util.MockDataProvider.seedDefaultCategories(dao)
    }

    override suspend fun insert(category: CategoryData): Long =
        dao.insertCategory(category.toEntity())

    override suspend fun update(category: CategoryData) {
        dao.updateCategory(category.toEntity())
    }

    override suspend fun delete(category: CategoryData) {
        if (category.isSystem) return
        val other = getOtherCategory() ?: return
        transactionDao.reassignCategory(category.id, other.id)
        dao.deleteCategory(category.toEntity())
    }

    override suspend fun getOtherCategory(): CategoryData? =
        dao.getSystemCategoryByName("Другое")?.toDomain()
}

class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore
) : SettingsRepository {
    override val currencyCode = dataStore.currencyCode
    override val userName = dataStore.userName
    override val themeMode = dataStore.themeMode
    override val onboardingDone = dataStore.onboardingDone
    override val demoLoaded = dataStore.demoLoaded
    override val notificationsEnabled = dataStore.notificationsEnabled
    override val notificationHour = dataStore.notificationHour
    override val biometricEnabled = dataStore.biometricEnabled

    override suspend fun setCurrency(code: String) { dataStore.setCurrency(code) }
    override suspend fun setUserName(name: String) { dataStore.setUserName(name) }
    override suspend fun setThemeMode(mode: String) { dataStore.setThemeMode(mode) }
    override suspend fun setOnboardingDone(done: Boolean) { dataStore.setOnboardingDone(done) }
    override suspend fun setDemoLoaded(loaded: Boolean) { dataStore.setDemoLoaded(loaded) }
    override suspend fun setNotificationsEnabled(enabled: Boolean) { dataStore.setNotificationsEnabled(enabled) }
    override suspend fun setNotificationHour(hour: Int) { dataStore.setNotificationHour(hour) }
    override suspend fun setBiometricEnabled(enabled: Boolean) { dataStore.setBiometricEnabled(enabled) }
}

class AdviceRepositoryImpl(
    private val geminiService: GeminiService
) : AdviceRepository {
    override suspend fun generateAdvice(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String
    ): String = geminiService.generateFinancialAdvice(transactions, categories, currencyCode)

    override fun getFallbackAdvice(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String,
        reason: String
    ): String = geminiService.getFallbackAdvice(transactions, categories, currencyCode, reason)
}

class MerchantRuleRepositoryImpl(
    private val dao: MerchantRuleDao
) : MerchantRuleRepository {
    override suspend fun findCategoryId(description: String): Long? =
        dao.findMatchingRule(description)?.categoryId

    override suspend fun saveRule(merchantPattern: String, categoryId: Long) {
        dao.insertRule(MerchantRuleEntity(merchantPattern = merchantPattern.lowercase(), categoryId = categoryId))
    }
}

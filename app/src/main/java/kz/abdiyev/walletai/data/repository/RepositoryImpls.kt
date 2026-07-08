package kz.abdiyev.walletai.data.repository

import kz.abdiyev.walletai.BuildConfig
import kz.abdiyev.walletai.ai.AdvicePrompt
import kz.abdiyev.walletai.ai.AdviceResult
import kz.abdiyev.walletai.ai.FallbackAdvice
import kz.abdiyev.walletai.ai.GeminiClient
import kz.abdiyev.walletai.ai.GeminiErrorMapper
import kz.abdiyev.walletai.data.database.AppDatabase
import kz.abdiyev.walletai.data.database.CategoryDao
import kz.abdiyev.walletai.data.database.MerchantRuleDao
import kz.abdiyev.walletai.data.database.MerchantRuleEntity
import kz.abdiyev.walletai.data.database.TransactionDao
import kz.abdiyev.walletai.data.mapper.toDomain
import kz.abdiyev.walletai.data.mapper.toEntity
import kz.abdiyev.walletai.data.preferences.SettingsDataStore
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.repository.AdviceRepository
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.MaintenanceRepository
import kz.abdiyev.walletai.domain.repository.MerchantRuleRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.util.MockDataProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

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
        MockDataProvider.seedDefaultCategories(dao)
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
    override val settings = dataStore.settings
    override suspend fun update(transform: (Settings) -> Settings) = dataStore.update(transform)
}

class AdviceRepositoryImpl(
    private val geminiClient: GeminiClient
) : AdviceRepository {
    override suspend fun generateAdvice(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String
    ): AdviceResult {
        val prompt = AdvicePrompt.build(transactions, categories, currencyCode)
        return try {
            AdviceResult.Ai(geminiClient.generateAdvice(prompt))
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e(e, "Gemini call failed")
            }
            val reason = GeminiErrorMapper.userMessage(e)
            AdviceResult.Fallback(
                text = FallbackAdvice.build(transactions, categories, currencyCode, reason),
                reason = reason
            )
        }
    }

    override fun localAdvice(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String,
        reason: String
    ): AdviceResult.Fallback = AdviceResult.Fallback(
        text = FallbackAdvice.build(transactions, categories, currencyCode, reason),
        reason = reason
    )
}

class MaintenanceRepositoryImpl(
    private val db: AppDatabase
) : MaintenanceRepository {
    override suspend fun loadDemoData() {
        MockDataProvider.loadDemoTransactions(db.categoryDao(), db.transactionDao())
    }

    override suspend fun wipeAllData() {
        db.transactionDao().deleteAllTransactions()
    }
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

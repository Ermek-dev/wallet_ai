package kz.abdiyev.walletai.domain.repository

import kz.abdiyev.walletai.ai.AdviceResult
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Settings
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
    val settings: Flow<Settings>
    suspend fun update(transform: (Settings) -> Settings)
}

interface AdviceRepository {
    suspend fun generateAdvice(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String
    ): AdviceResult

    fun localAdvice(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String,
        reason: String
    ): AdviceResult.Fallback
}

interface MerchantRuleRepository {
    suspend fun findCategoryId(description: String): Long?
    suspend fun saveRule(merchantPattern: String, categoryId: Long)
}

/** Maintenance operations that need database-level access; keeps AppDatabase out of ViewModels. */
interface MaintenanceRepository {
    suspend fun loadDemoData()
    suspend fun wipeAllData()
}

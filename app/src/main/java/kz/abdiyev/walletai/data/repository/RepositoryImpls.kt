package kz.abdiyev.walletai.data.repository

import kz.abdiyev.walletai.BuildConfig
import kz.abdiyev.walletai.ai.AdviceAiClient
import kz.abdiyev.walletai.ai.AdviceCachePolicy
import kz.abdiyev.walletai.ai.AdvicePrompt
import kz.abdiyev.walletai.ai.AdviceResult
import kz.abdiyev.walletai.ai.AiErrorMapper
import kz.abdiyev.walletai.ai.FallbackAdvice
import kz.abdiyev.walletai.data.database.AppDatabase
import kz.abdiyev.walletai.data.database.CategoryDao
import kz.abdiyev.walletai.data.database.MerchantRuleDao
import kz.abdiyev.walletai.data.database.MerchantRuleEntity
import kz.abdiyev.walletai.data.database.TransactionDao
import kz.abdiyev.walletai.data.mapper.toDomain
import kz.abdiyev.walletai.data.mapper.toEntity
import kz.abdiyev.walletai.data.preferences.AdviceCacheDataStore
import kz.abdiyev.walletai.data.preferences.AdviceCacheEntry
import kz.abdiyev.walletai.data.preferences.SettingsDataStore
import kz.abdiyev.walletai.domain.category.CategoryHierarchyValidator
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.port.CategoryStore
import kz.abdiyev.walletai.domain.port.MerchantRuleStore
import kz.abdiyev.walletai.domain.port.TransactionStore
import kz.abdiyev.walletai.domain.repository.AdviceRepository
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.MaintenanceRepository
import kz.abdiyev.walletai.domain.repository.MerchantRuleRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.util.Clock
import kz.abdiyev.walletai.data.seed.DefaultCategories
import kz.abdiyev.walletai.data.seed.DemoDataSeeder
import kz.abdiyev.walletai.util.SystemClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class TransactionRepositoryImpl(
    private val dao: TransactionDao
) : TransactionRepository, TransactionStore {
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
        val original = dao.getById(transaction.id) ?: return
        dao.updateTransaction(transaction.copy(
            sourceKey = original.sourceKey,
            merchantKey = original.merchantKey,
            originalDescription = original.originalDescription,
            importBatchId = original.importBatchId,
            isAutoCategorized = false,
            needsReview = false
        ).toEntity())
    }

    override suspend fun delete(transaction: TransactionData) {
        dao.deleteTransaction(transaction.toEntity())
    }

    override suspend fun deleteAll() {
        dao.deleteAllTransactions()
    }

    override suspend fun sumByType(
        type: TransactionType,
        startInclusive: Long?,
        endExclusive: Long?
    ): Long {
        val typeName = type.name
        return if (startInclusive != null && endExclusive != null) {
            dao.sumByTypeInRange(typeName, startInclusive, endExclusive)
        } else {
            dao.sumAllByType(typeName)
        }
    }

    override suspend fun sumByCategoryGrouped(
        type: TransactionType,
        startInclusive: Long?,
        endExclusive: Long?
    ): Map<Long, Long> {
        val typeName = type.name
        val rows = if (startInclusive != null && endExclusive != null) {
            dao.sumByCategoryInRange(typeName, startInclusive, endExclusive)
        } else {
            dao.sumByCategoryAll(typeName)
        }
        return rows.associate { it.categoryId to it.totalMinor }
    }

    override suspend fun countAll(): Int = dao.countAll()

    override suspend fun countInPeriod(startInclusive: Long, endExclusive: Long): Int =
        dao.countInRange(startInclusive, endExclusive)
}

class CategoryRepositoryImpl(
    private val dao: CategoryDao,
    private val transactionDao: TransactionDao
) : CategoryRepository, CategoryStore {
    override val categories: Flow<List<CategoryData>> =
        dao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<CategoryData> =
        dao.getAllCategoriesList().map { it.toDomain() }

    override suspend fun ensureDefaultCategories() {
        val existingNames = dao.getAllCategoriesList().map { it.name }.toSet()
        val missing = DefaultCategories.entities.filter { it.name !in existingNames }
        if (missing.isNotEmpty()) {
            dao.insertCategories(missing)
        }
    }

    override suspend fun insert(category: CategoryData): Long {
        validateCategory(category)
        return dao.insertCategory(category.toEntity())
    }

    override suspend fun update(category: CategoryData) {
        validateCategory(category)
        dao.updateCategory(category.toEntity())
    }

    override suspend fun delete(category: CategoryData) {
        if (category.isSystem) return
        dao.orphanChildren(category.id)
        val other = getOtherCategory() ?: return
        transactionDao.reassignCategory(category.id, other.id)
        dao.deleteCategory(category.toEntity())
    }

    override suspend fun getOtherCategory(): CategoryData? =
        dao.getSystemCategoryByName("Другое")?.toDomain()

    private suspend fun validateCategory(category: CategoryData) {
        val all = getAll()
        val error = CategoryHierarchyValidator.validate(category, all)
        require(error == null) { "Invalid category hierarchy: $error" }
    }
}

class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore
) : SettingsRepository {
    override val settings = dataStore.settings
    override suspend fun update(transform: (Settings) -> Settings) = dataStore.update(transform)
}

class AdviceRepositoryImpl(
    private val aiClient: AdviceAiClient,
    private val adviceCacheDataStore: AdviceCacheDataStore,
    private val clock: Clock = SystemClock
) : AdviceRepository {
    private var memoryCache: AdviceCacheEntry? = null

    override suspend fun generateAdvice(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String,
        forceRefresh: Boolean
    ): AdviceResult {
        val prompt = AdvicePrompt.build(transactions, categories, currencyCode)
        val promptHash = AdviceCachePolicy.promptHash(prompt)
        val nowMillis = clock.nowMillis()

        memoryCache?.let { cached ->
            if (AdviceCachePolicy.isValid(
                    cachedPromptHash = cached.promptHash,
                    cachedTimestampMillis = cached.timestampMillis,
                    currentPromptHash = promptHash,
                    nowMillis = nowMillis,
                    forceRefresh = forceRefresh
                )
            ) {
                return AdviceResult.Ai(
                    text = cached.adviceText,
                    fromCache = true,
                    cachedAtMillis = cached.timestampMillis
                )
            }
        }

        if (!forceRefresh) {
            val stored = adviceCacheDataStore.get()
            if (stored != null && AdviceCachePolicy.isValid(
                    cachedPromptHash = stored.promptHash,
                    cachedTimestampMillis = stored.timestampMillis,
                    currentPromptHash = promptHash,
                    nowMillis = nowMillis,
                    forceRefresh = false
                )
            ) {
                memoryCache = stored
                return AdviceResult.Ai(
                    text = stored.adviceText,
                    fromCache = true,
                    cachedAtMillis = stored.timestampMillis
                )
            }
        }

        return try {
            val text = aiClient.generateAdvice(prompt)
            val entry = AdviceCacheEntry(
                adviceText = text,
                promptHash = promptHash,
                timestampMillis = nowMillis
            )
            memoryCache = entry
            adviceCacheDataStore.save(entry)
            AdviceResult.Ai(text)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e(e, "AI advice call failed")
            }
            val reason = AiErrorMapper.userMessage(e)
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
    private val db: AppDatabase,
    private val clock: Clock = SystemClock
) : MaintenanceRepository {
    override suspend fun loadDemoData() {
        DemoDataSeeder.loadDemoTransactions(db.categoryDao(), db.transactionDao(), clock)
    }

    override suspend fun wipeAllData() {
        db.transactionDao().deleteAllTransactions()
    }
}

class MerchantRuleRepositoryImpl(
    private val dao: MerchantRuleDao
) : MerchantRuleRepository, MerchantRuleStore {
    override suspend fun findCategoryId(description: String): Long? =
        dao.findExact(kz.abdiyev.walletai.util.DescriptionNormalizer.normalize(description))?.categoryId

    override suspend fun findMemory(pattern: String): kz.abdiyev.walletai.domain.model.MerchantMemory? =
        dao.findExact(pattern)?.let {
            kz.abdiyev.walletai.domain.model.MerchantMemory(it.merchantPattern, it.categoryId, it.displayName)
        }

    override suspend fun saveRule(merchantPattern: String, categoryId: Long) {
        val normalized = kz.abdiyev.walletai.util.DescriptionNormalizer.normalize(merchantPattern)
        if (normalized.isBlank()) return
        val previous = dao.findExact(normalized)
        dao.insertRule(MerchantRuleEntity(id = previous?.id ?: 0, merchantPattern = normalized, categoryId = categoryId))
    }
}

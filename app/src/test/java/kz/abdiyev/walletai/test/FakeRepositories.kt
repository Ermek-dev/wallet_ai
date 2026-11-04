package kz.abdiyev.walletai.test

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.port.CategoryStore
import kz.abdiyev.walletai.domain.port.MerchantRuleStore
import kz.abdiyev.walletai.domain.port.TransactionStore
import kz.abdiyev.walletai.domain.repository.AdviceRepository
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.MerchantRuleRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTransactionRepository : TransactionRepository, TransactionStore {
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

    override suspend fun sumByType(
        type: TransactionType,
        startInclusive: Long?,
        endExclusive: Long?
    ): Long = filtered(type, startInclusive, endExclusive).sumOf { it.money.amountMinor }

    override suspend fun sumByCategoryGrouped(
        type: TransactionType,
        startInclusive: Long?,
        endExclusive: Long?
    ): Map<Long, Long> = filtered(type, startInclusive, endExclusive)
        .groupBy { it.categoryId }
        .mapValues { (_, txs) -> txs.sumOf { it.money.amountMinor } }

    override suspend fun countAll(): Int = items.size

    override suspend fun countInPeriod(startInclusive: Long, endExclusive: Long): Int =
        items.count { it.date >= startInclusive && it.date < endExclusive }

    private fun filtered(
        type: TransactionType,
        startInclusive: Long?,
        endExclusive: Long?
    ): List<TransactionData> = items.filter { tx ->
        tx.type == type &&
            (startInclusive == null || tx.date >= startInclusive) &&
            (endExclusive == null || tx.date < endExclusive)
    }
}

class FakeCategoryRepository(
    initial: List<CategoryData> = listOf(
        CategoryData(1, "Продукты", kz.abdiyev.walletai.domain.model.TransactionType.EXPENSE, "#EF5350", "shopping_cart")
    )
) : CategoryRepository, CategoryStore {
    private val items = initial.toMutableList()
    private val flow = MutableStateFlow(items.toList())

    override val categories: Flow<List<CategoryData>> = flow

    override suspend fun getAll(): List<CategoryData> = items.toList()

    override suspend fun ensureDefaultCategories() {
        if (items.isEmpty()) {
            insert(CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart"))
        }
    }

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
    private val _settings = MutableStateFlow(Settings(onboardingDone = true))

    override val settings: Flow<Settings> = _settings

    override suspend fun update(transform: (Settings) -> Settings) {
        _settings.value = transform(_settings.value)
    }
}

class FakeMerchantRuleRepository : MerchantRuleRepository, MerchantRuleStore {
    override suspend fun findCategoryId(description: String): Long? = null
    override suspend fun saveRule(merchantPattern: String, categoryId: Long) = Unit
}

class FakeAdviceRepository(
    private val generateResult: kz.abdiyev.walletai.ai.AdviceResult = kz.abdiyev.walletai.ai.AdviceResult.Ai("AI advice"),
    private val fallbackReason: String = "offline"
) : AdviceRepository {
    override suspend fun generateAdvice(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String,
        forceRefresh: Boolean
    ): kz.abdiyev.walletai.ai.AdviceResult = generateResult

    override fun localAdvice(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String,
        reason: String
    ): kz.abdiyev.walletai.ai.AdviceResult.Fallback =
        kz.abdiyev.walletai.ai.AdviceResult.Fallback("Local report", fallbackReason)
}

package kz.abdiyev.walletai.test

import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.port.MerchantRuleStore
import kz.abdiyev.walletai.domain.port.TransactionStore

class FakeTransactionStore : TransactionStore {
    val items = mutableListOf<TransactionData>()

    override suspend fun insert(transaction: TransactionData): Long {
        val id = (items.maxOfOrNull { it.id } ?: 0L) + 1
        items.add(transaction.copy(id = id))
        return id
    }

    override suspend fun insertAll(transactions: List<TransactionData>) {
        transactions.forEach { insert(it) }
    }

    override suspend fun getAll(): List<TransactionData> = items.toList()
}

class FakeMerchantRuleStore : MerchantRuleStore {
    val savedRules = mutableMapOf<String, Long>()

    override suspend fun findCategoryId(description: String): Long? = null

    override suspend fun saveRule(merchantPattern: String, categoryId: Long) {
        savedRules[merchantPattern.lowercase()] = categoryId
    }
}

class FakeCategoryStore(
    private val categories: List<kz.abdiyev.walletai.domain.model.CategoryData> = emptyList()
) : kz.abdiyev.walletai.domain.port.CategoryStore {
    override suspend fun getAll() = categories
}

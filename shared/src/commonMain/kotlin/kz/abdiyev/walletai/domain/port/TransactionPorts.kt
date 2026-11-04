package kz.abdiyev.walletai.domain.port

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.TransactionData

interface TransactionStore {
    suspend fun insert(transaction: TransactionData): Long
    suspend fun insertAll(transactions: List<TransactionData>)
    suspend fun getAll(): List<TransactionData>
}

interface MerchantRuleStore {
    suspend fun findCategoryId(description: String): Long?
    suspend fun saveRule(merchantPattern: String, categoryId: Long)
    suspend fun findMemory(pattern: String): kz.abdiyev.walletai.domain.model.MerchantMemory? =
        findCategoryId(pattern)?.let { kz.abdiyev.walletai.domain.model.MerchantMemory(pattern, it) }
}

interface CategoryStore {
    suspend fun getAll(): List<CategoryData>
}

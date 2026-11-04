package kz.abdiyev.walletai.domain.usecase

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.port.MerchantRuleStore
import kz.abdiyev.walletai.domain.port.TransactionStore
import kz.abdiyev.walletai.ml.TransactionClassifier

data class SaveTransactionRequest(
    val money: Money,
    val type: TransactionType,
    val categoryId: Long,
    val description: String,
    val date: Long,
    val isAutoCategorized: Boolean = false,
    val persistMerchantRule: Boolean = false
)

sealed interface SaveTransactionResult {
    data class Success(val id: Long) : SaveTransactionResult
    data class BatchSuccess(val count: Int) : SaveTransactionResult
    data object InvalidAmount : SaveTransactionResult
}

class SaveTransactionUseCase(
    private val transactions: TransactionStore,
    private val merchantRules: MerchantRuleStore,
    private val classifier: TransactionClassifier
) {
    suspend fun execute(request: SaveTransactionRequest): SaveTransactionResult {
        if (request.money.amountMinor <= 0) return SaveTransactionResult.InvalidAmount
        val id = transactions.insert(
            TransactionData(
                money = request.money,
                type = request.type,
                categoryId = request.categoryId,
                description = request.description,
                date = request.date,
                isAutoCategorized = request.isAutoCategorized
            )
        )
        if (request.persistMerchantRule) {
            merchantRules.saveRule(request.description, request.categoryId)
        }
        return SaveTransactionResult.Success(id)
    }

    suspend fun executeBatch(items: List<TransactionData>): SaveTransactionResult {
        if (items.isEmpty()) return SaveTransactionResult.BatchSuccess(0)
        if (items.any { it.money.amountMinor <= 0 }) return SaveTransactionResult.InvalidAmount
        transactions.insertAll(items)
        return SaveTransactionResult.BatchSuccess(items.size)
    }

    suspend fun resolveCategoryId(
        description: String,
        categories: List<CategoryData>,
        trainedModel: TransactionClassifier.TrainedModel?,
        fallbackCategoryId: Long
    ): Long {
        merchantRules.findCategoryId(description)?.let { return it }
        if (trainedModel != null && categories.isNotEmpty()) {
            return classifier.classify(description, trainedModel, categories)
        }
        return fallbackCategoryId
    }
}

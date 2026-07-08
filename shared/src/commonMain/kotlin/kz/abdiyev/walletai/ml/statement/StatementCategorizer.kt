package kz.abdiyev.walletai.ml.statement

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.ml.TransactionClassifier

class StatementCategorizer(
    private val classifier: TransactionClassifier = TransactionClassifier()
) {
    fun categoryIdFor(
        raw: RawStatementLine,
        categories: List<CategoryData>,
        trainedModel: TransactionClassifier.TrainedModel?,
        transfersCategoryId: Long?
    ): Long {
        if (categories.isEmpty()) return 0L

        MerchantDictionary.categoryNameFor(raw.description, raw.rawOperation)
            ?.let { name -> categories.find { it.name.equals(name, ignoreCase = true) }?.id }
            ?.let { return it }

        if (trainedModel != null) {
            return classifier.classify(raw.description, trainedModel, categories)
        }

        if (shouldUseTransfersCategory(raw) && transfersCategoryId != null) {
            return transfersCategoryId
        }

        return categories.firstOrNull()?.id ?: 0L
    }

    private fun shouldUseTransfersCategory(raw: RawStatementLine): Boolean {
        val op = raw.rawOperation.lowercase()
        val desc = raw.description.lowercase()
        if (op.contains("перевод") || op.contains("пополнение")) {
            if (desc.contains("зарплата")) return false
            if (MerchantDictionary.categoryNameFor(raw.description, raw.rawOperation) != null) return false
            return true
        }
        return false
    }
}

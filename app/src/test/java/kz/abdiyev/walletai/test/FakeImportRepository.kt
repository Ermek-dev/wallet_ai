package kz.abdiyev.walletai.test

import kotlinx.coroutines.flow.MutableStateFlow
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionPreview
import kz.abdiyev.walletai.domain.repository.ImportBatch
import kz.abdiyev.walletai.domain.repository.ImportRepository

class FakeImportRepository(private val transactions: FakeTransactionRepository) : ImportRepository {
    override val history = MutableStateFlow<List<ImportBatch>>(emptyList())
    override suspend fun save(sourceLabel: String, createdAt: Long, previews: List<TransactionPreview>, skipTransfers: Boolean): Int {
        val selected = previews.filter { (!it.isDuplicate || it.forceImport) && (!skipTransfers || !it.isTransfer) }
        transactions.insertAll(selected.map {
            TransactionData(money = it.money, type = it.type, categoryId = it.predictedCategoryId,
                description = it.description, date = it.date, sourceKey = it.sourceKey,
                originalDescription = it.originalDescription, needsReview = it.needsReview,
                isAutoCategorized = !it.categoryConfirmed)
        })
        return selected.size
    }
    override suspend fun undo(batchId: Long) = Unit
    override suspend fun transactions(batchId: Long) = emptyList<TransactionData>()
}

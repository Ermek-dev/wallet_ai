package kz.abdiyev.walletai.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import kz.abdiyev.walletai.data.database.AppDatabase
import kz.abdiyev.walletai.data.database.ImportBatchEntity
import kz.abdiyev.walletai.data.database.MerchantRuleEntity
import kz.abdiyev.walletai.data.mapper.toDomain
import kz.abdiyev.walletai.data.mapper.toEntity
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionPreview
import kz.abdiyev.walletai.domain.repository.ImportBatch
import kz.abdiyev.walletai.domain.repository.ImportRepository

class ImportRepositoryImpl(private val db: AppDatabase) : ImportRepository {
    override val history = db.importBatchDao().observe().map { batches -> batches.map {
        ImportBatch(it.id, it.sourceLabel, it.createdAt, it.addedCount, it.skippedCount, it.reviewCount, it.undone)
    } }

    override suspend fun save(
        sourceLabel: String, createdAt: Long, previews: List<TransactionPreview>, skipTransfers: Boolean
    ): Int = db.withTransaction {
        val selected = kz.abdiyev.walletai.domain.usecase.ImportSelection.select(
            previews, db.transactionDao().getAllTransactionsList().mapNotNull { it.sourceKey }, skipTransfers
        )
        require(selected.all { it.money.amountMinor > 0 }) { "Invalid import amount" }
        val batchId = db.importBatchDao().insert(ImportBatchEntity(
            sourceLabel = sourceLabel, createdAt = createdAt, addedCount = selected.size,
            skippedCount = previews.size - selected.size, reviewCount = selected.count { it.needsReview }
        ))
        db.transactionDao().insertTransactions(selected.map { preview -> TransactionData(
            money = preview.money, type = preview.type, categoryId = preview.predictedCategoryId,
            description = preview.description, date = preview.date,
            isAutoCategorized = !preview.categoryConfirmed, sourceKey = preview.sourceKey,
            merchantKey = preview.merchantKey,
            originalDescription = preview.originalDescription, importBatchId = batchId,
            needsReview = preview.needsReview
        ).toEntity() })
        selected.filter { it.rememberMerchant }.distinctBy { it.merchantKey }.forEach { preview ->
            val previous = db.merchantRuleDao().findExact(preview.merchantKey)
            db.merchantRuleDao().insertRule(MerchantRuleEntity(
                id = previous?.id ?: 0, merchantPattern = preview.merchantKey,
                categoryId = preview.predictedCategoryId, displayName = preview.description
            ))
        }
        selected.size
    }

    override suspend fun undo(batchId: Long) = db.withTransaction {
        db.transactionDao().deleteBatch(batchId)
        db.importBatchDao().markUndone(batchId)
    }

    override suspend fun transactions(batchId: Long): List<TransactionData> =
        db.transactionDao().getAllTransactionsList().filter { it.importBatchId == batchId }.map { it.toDomain() }
}

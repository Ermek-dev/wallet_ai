package kz.abdiyev.walletai.domain.repository

import kotlinx.coroutines.flow.Flow
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionPreview

data class ImportBatch(
    val id: Long,
    val sourceLabel: String,
    val createdAt: Long,
    val addedCount: Int,
    val skippedCount: Int,
    val reviewCount: Int,
    val undone: Boolean
)

interface ImportRepository {
    val history: Flow<List<ImportBatch>>
    suspend fun save(sourceLabel: String, createdAt: Long, previews: List<TransactionPreview>, skipTransfers: Boolean): Int
    suspend fun undo(batchId: Long)
    suspend fun transactions(batchId: Long): List<TransactionData>
}

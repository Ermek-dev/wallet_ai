package kz.abdiyev.walletai.domain.model

data class TransactionPreview(
    val id: Long = 0,
    val money: Money,
    val type: TransactionType,
    val description: String,
    val predictedCategoryId: Long,
    val date: Long,
    val isTransfer: Boolean = false,
    val isDuplicate: Boolean = false
)

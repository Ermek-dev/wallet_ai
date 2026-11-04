package kz.abdiyev.walletai.domain.model

data class TransactionPreview(
    val id: Long = 0,
    val money: Money,
    val type: TransactionType,
    val description: String,
    val predictedCategoryId: Long,
    val date: Long,
    val isTransfer: Boolean = false,
    val isDuplicate: Boolean = false,
    val sourceKey: String? = null,
    val originalDescription: String = description,
    val needsReview: Boolean = false,
    val categoryConfirmed: Boolean = false,
    val forceImport: Boolean = false,
    val rememberMerchant: Boolean = false,
    val merchantKey: String = originalDescription
)

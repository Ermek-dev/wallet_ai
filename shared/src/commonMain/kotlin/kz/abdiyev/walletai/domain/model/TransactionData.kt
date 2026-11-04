package kz.abdiyev.walletai.domain.model

data class TransactionData(
    val id: Long = 0,
    val money: Money,
    val type: TransactionType,
    val categoryId: Long,
    val description: String,
    val date: Long,
    val isAutoCategorized: Boolean = false,
    val sourceKey: String? = null,
    val merchantKey: String? = null,
    val originalDescription: String? = null,
    val importBatchId: Long? = null,
    val needsReview: Boolean = false
)

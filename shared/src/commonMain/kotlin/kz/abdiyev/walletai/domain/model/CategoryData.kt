package kz.abdiyev.walletai.domain.model

data class CategoryData(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val colorHex: String,
    val iconName: String,
    val isSystem: Boolean = false,
    val parentId: Long? = null
)

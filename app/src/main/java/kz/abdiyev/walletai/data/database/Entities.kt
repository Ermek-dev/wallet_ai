package kz.abdiyev.walletai.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["parentId"])]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val colorHex: String,
    val iconName: String,
    val isSystem: Boolean = false,
    val parentId: Long? = null
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["categoryId"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val currencyCode: String = "KZT",
    val type: String,
    val categoryId: Long,
    val description: String,
    val date: Long,
    val isAutoCategorized: Boolean = false,
    val sourceKey: String? = null,
    val merchantKey: String? = null,
    val originalDescription: String? = null,
    val importBatchId: Long? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val needsReview: Boolean = false
)

@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantPattern: String,
    val categoryId: Long,
    val displayName: String? = null
)

@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceLabel: String,
    val createdAt: Long,
    val addedCount: Int,
    val skippedCount: Int,
    val reviewCount: Int,
    val undone: Boolean = false
)

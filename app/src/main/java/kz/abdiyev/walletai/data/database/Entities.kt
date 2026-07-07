package kz.abdiyev.walletai.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val colorHex: String,
    val iconName: String,
    val isSystem: Boolean = false
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
    val isAutoCategorized: Boolean = false
)

@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantPattern: String,
    val categoryId: Long
)

package kz.abdiyev.walletai.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE isSystem = 1 AND name = :name LIMIT 1")
    suspend fun getSystemCategoryByName(name: String): CategoryEntity?

    @Query("UPDATE categories SET parentId = NULL WHERE parentId = :parentId")
    suspend fun orphanChildren(parentId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE description LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE type = :type")
    suspend fun sumAllByType(type: String): Long

    @Query(
        """
        SELECT COALESCE(SUM(amountMinor), 0) FROM transactions
        WHERE type = :type AND date >= :startInclusive AND date < :endExclusive
        """
    )
    suspend fun sumByTypeInRange(type: String, startInclusive: Long, endExclusive: Long): Long

    @Query(
        """
        SELECT categoryId, SUM(amountMinor) AS totalMinor FROM transactions
        WHERE type = :type AND date >= :startInclusive AND date < :endExclusive
        GROUP BY categoryId
        """
    )
    suspend fun sumByCategoryInRange(
        type: String,
        startInclusive: Long,
        endExclusive: Long
    ): List<CategorySumRow>

    @Query(
        """
        SELECT categoryId, SUM(amountMinor) AS totalMinor FROM transactions
        WHERE type = :type
        GROUP BY categoryId
        """
    )
    suspend fun sumByCategoryAll(type: String): List<CategorySumRow>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countAll(): Int

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE date >= :startInclusive AND date < :endExclusive
        """
    )
    suspend fun countInRange(startInclusive: Long, endExclusive: Long): Int
}

@Dao
interface MerchantRuleDao {
    @Query("SELECT * FROM merchant_rules")
    suspend fun getAllRules(): List<MerchantRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: MerchantRuleEntity): Long

    @Query("SELECT * FROM merchant_rules WHERE :description LIKE '%' || merchantPattern || '%' LIMIT 1")
    suspend fun findMatchingRule(description: String): MerchantRuleEntity?
}

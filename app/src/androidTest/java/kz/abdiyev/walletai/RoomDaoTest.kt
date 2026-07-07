package kz.abdiyev.walletai

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kz.abdiyev.walletai.data.database.AppDatabase
import kz.abdiyev.walletai.data.database.CategoryEntity
import kz.abdiyev.walletai.data.database.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndReadTransaction() = runBlocking {
        val categoryId = database.categoryDao().insertCategory(
            CategoryEntity(name = "Продукты", type = "EXPENSE", colorHex = "#EF5350", iconName = "cart")
        )
        database.transactionDao().insertTransaction(
            TransactionEntity(
                amountMinor = 340_000,
                currencyCode = "KZT",
                type = "EXPENSE",
                categoryId = categoryId,
                description = "Magnum",
                date = System.currentTimeMillis()
            )
        )
        val all = database.transactionDao().getAllTransactions().first()
        assertEquals(1, all.size)
        assertEquals("Magnum", all.first().description)
    }

    @Test
    fun deleteTransaction_removesRow() = runBlocking {
        val categoryId = database.categoryDao().insertCategory(
            CategoryEntity(name = "Продукты", type = "EXPENSE", colorHex = "#EF5350", iconName = "cart")
        )
        val tx = TransactionEntity(
            amountMinor = 100_00,
            currencyCode = "KZT",
            type = "EXPENSE",
            categoryId = categoryId,
            description = "Test",
            date = 1_000L
        )
        val id = database.transactionDao().insertTransaction(tx)
        val saved = tx.copy(id = id)
        database.transactionDao().deleteTransaction(saved)
        assertEquals(0, database.transactionDao().getAllTransactionsList().size)
    }
}

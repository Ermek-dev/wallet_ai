package kz.abdiyev.walletai

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kz.abdiyev.walletai.data.database.AppDatabase
import kz.abdiyev.walletai.data.repository.ImportRepositoryImpl
import kz.abdiyev.walletai.data.repository.MerchantRuleRepositoryImpl
import kz.abdiyev.walletai.data.repository.TransactionRepositoryImpl
import kz.abdiyev.walletai.domain.model.*
import kz.abdiyev.walletai.domain.usecase.ImportIdentity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ImportRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: ImportRepositoryImpl
    private val preview = TransactionPreview(
        id = 1, money = Money(15000, Currency.KZT), type = TransactionType.EXPENSE,
        description = "Арзан", predictedCategoryId = 1, date = 1782691200000,
        sourceKey = ImportIdentity.key("KASPI:", 1782691200000, Money(15000, Currency.KZT), TransactionType.EXPENSE, "ИП Сергеев"),
        originalDescription = "ИП Сергеев", categoryConfirmed = true, rememberMerchant = true,
        merchantKey = ImportIdentity.merchantKey("KASPI:", "ИП Сергеев")
    )

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = ImportRepositoryImpl(db)
    }
    @After fun close() { db.close() }

    @Test fun savingAgainDoesNotOverwriteEdits() = runTest {
        assertEquals(1, repository.save("Первый", 1, listOf(preview), true))
        val transactions = TransactionRepositoryImpl(db.transactionDao())
        val saved = transactions.getAll().single()
        transactions.update(saved.copy(description = "Моя покупка", categoryId = 2,
            sourceKey = null, originalDescription = null, importBatchId = null))
        assertEquals(0, repository.save("Повторный", 2, listOf(preview), true))
        val updated = transactions.getAll().single()
        assertEquals("Моя покупка", updated.description)
        assertEquals(preview.sourceKey, updated.sourceKey)
        assertEquals(saved.importBatchId, updated.importBatchId)
        assertEquals("Арзан", MerchantRuleRepositoryImpl(db.merchantRuleDao()).findMemory(preview.merchantKey)?.displayName)
    }

    @Test fun undoOnlyRemovesTransactionsOfSelectedBatch() = runTest {
        repository.save("Первый", 1, listOf(preview), true)
        repository.save("Второй", 2, listOf(preview.copy(sourceKey = "other")), true)
        val first = repository.history.first().last()
        repository.undo(first.id)
        assertEquals("other", db.transactionDao().getAllTransactionsList().single().sourceKey)
        assertTrue(repository.history.first().last().undone)
    }

    @Test fun countMatchingPreservesTwoIdenticalPurchasesAndExplicitOverride() = runTest {
        repository.save("Первый", 1, listOf(preview), true)
        assertEquals(1, repository.save("Второй", 2, listOf(preview.copy(isDuplicate = true), preview.copy(id = 2)), true))
        assertEquals(2, db.transactionDao().getAllTransactionsList().size)
        assertEquals(1, repository.save("Третий", 3, listOf(preview.copy(isDuplicate = true, forceImport = true)), true))
    }

    @Test fun invalidBatchLeavesHistoryRulesAndTransactionsUntouched() = runTest {
        try {
            repository.save("Ошибочный", 1, listOf(preview.copy(money = Money(0, Currency.KZT))), true)
            fail("Expected invalid amount")
        } catch (_: IllegalArgumentException) { }
        assertTrue(repository.history.first().isEmpty())
        assertTrue(db.transactionDao().getAllTransactionsList().isEmpty())
        assertTrue(db.merchantRuleDao().getAllRules().isEmpty())
    }

    @Test fun ruleWriteFailureRollsBackWholeImport() = runTest {
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER reject_rule BEFORE INSERT ON merchant_rules
            BEGIN SELECT RAISE(ABORT, 'test failure'); END
        """.trimIndent())
        try {
            repository.save("Ошибка записи", 1, listOf(preview), true)
            fail("Expected rule write failure")
        } catch (_: android.database.sqlite.SQLiteException) { }
        assertTrue(repository.history.first().isEmpty())
        assertTrue(db.transactionDao().getAllTransactionsList().isEmpty())
    }
}

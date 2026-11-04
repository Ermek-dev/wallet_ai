package kz.abdiyev.walletai

import android.app.Application
import androidx.room.Room
import kz.abdiyev.walletai.data.database.AppDatabase
import kz.abdiyev.walletai.data.database.DatabaseMigrations
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ImportMigrationTest {
    @Test fun versionFourMigratesWithoutLosingManualCorrections() {
        val context = RuntimeEnvironment.getApplication()
        val name = "import-migration-test"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, 0, null).use { db ->
            db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, colorHex TEXT NOT NULL, iconName TEXT NOT NULL, isSystem INTEGER NOT NULL, parentId INTEGER)")
            db.execSQL("CREATE INDEX index_categories_parentId ON categories(parentId)")
            db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, amountMinor INTEGER NOT NULL, currencyCode TEXT NOT NULL, type TEXT NOT NULL, categoryId INTEGER NOT NULL, description TEXT NOT NULL, date INTEGER NOT NULL, isAutoCategorized INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX index_transactions_categoryId ON transactions(categoryId)")
            db.execSQL("CREATE TABLE merchant_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, merchantPattern TEXT NOT NULL, categoryId INTEGER NOT NULL)")
            db.execSQL("INSERT INTO transactions VALUES (1, 15000, 'KZT', 'EXPENSE', 2, 'Арзан', 1000, 0)")
            db.version = 4
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(DatabaseMigrations.MIGRATION_4_5).allowMainThreadQueries().build()
        try {
            database.openHelper.writableDatabase.query("SELECT description, amountMinor, sourceKey, needsReview FROM transactions").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Арзан", cursor.getString(0))
                assertEquals(15000L, cursor.getLong(1))
                assertTrue(cursor.isNull(2))
                assertEquals(0, cursor.getInt(3))
            }
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}

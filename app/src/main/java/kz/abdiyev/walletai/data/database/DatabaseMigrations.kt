package kz.abdiyev.walletai.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS transactions_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    currencyCode TEXT NOT NULL DEFAULT 'KZT',
                    type TEXT NOT NULL,
                    categoryId INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    isAutoCategorized INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO transactions_new (id, amountMinor, currencyCode, type, categoryId, description, date, isAutoCategorized)
                SELECT id, CAST(ROUND(amount * 100) AS INTEGER), 'KZT', type, categoryId, description, date, isAutoCategorized
                FROM transactions
                """.trimIndent()
            )
            db.execSQL("DROP TABLE transactions")
            db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE categories ADD COLUMN isSystem INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS merchant_rules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    merchantPattern TEXT NOT NULL,
                    categoryId INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
            db.execSQL("UPDATE categories SET isSystem = 1 WHERE name = 'Другое'")
        }
    }
}

package kz.abdiyev.walletai.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN sourceKey TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN merchantKey TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN originalDescription TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN importBatchId INTEGER")
            db.execSQL("ALTER TABLE transactions ADD COLUMN needsReview INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE merchant_rules ADD COLUMN displayName TEXT")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS import_batches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sourceLabel TEXT NOT NULL, createdAt INTEGER NOT NULL,
                    addedCount INTEGER NOT NULL, skippedCount INTEGER NOT NULL,
                    reviewCount INTEGER NOT NULL, undone INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

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

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE categories ADD COLUMN parentId INTEGER DEFAULT NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_parentId ON categories(parentId)")
        }
    }
}

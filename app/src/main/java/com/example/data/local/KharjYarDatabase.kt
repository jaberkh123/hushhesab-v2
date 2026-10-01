package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        DebtEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class KharjYarDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun debtDao(): DebtDao

    companion object {
        @Volatile
        private var INSTANCE: KharjYarDatabase? = null

        // ── Migration 1 → 2 ──────────────────────────────────────────────
        // The v1 schema was never exported (exportSchema = false), so the
        // exact v1→v2 diff is unknown. This migration is "self-healing":
        // it introspects the existing database at runtime, creates any
        // missing v2 tables and adds any missing v2 columns. All v2 DDL
        // below is generated 1:1 from the current @Entity definitions.
        //
        // NOTE: this only covers *additive* schema changes (new tables,
        // new columns). Renamed/removed columns or changed primary keys
        // would still fail Room's validation — exportSchema is now true
        // so future version bumps get real, precise migrations.

        private data class ColumnDef(val name: String, val ddl: String)

        private val V2_TABLE_DDL = listOf(
            """CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `balance` INTEGER NOT NULL, `currency` TEXT NOT NULL, `icon` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)""",
            """CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `destinationAccountId` INTEGER, `type` TEXT NOT NULL, `amount` INTEGER NOT NULL, `categoryId` TEXT NOT NULL, `categoryName` TEXT NOT NULL, `categoryIcon` TEXT NOT NULL, `currency` TEXT NOT NULL, `dateTimestamp` INTEGER NOT NULL, `note` TEXT NOT NULL, `tags` TEXT NOT NULL, `isRecurring` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)""",
            """CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `categoryId` TEXT NOT NULL, `categoryName` TEXT NOT NULL, `categoryIcon` TEXT NOT NULL, `monthlyLimit` INTEGER NOT NULL, `monthYear` TEXT NOT NULL)""",
            """CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `targetAmount` INTEGER NOT NULL, `currentAmount` INTEGER NOT NULL, `targetDate` INTEGER, `icon` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)""",
            """CREATE TABLE IF NOT EXISTS `debts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `personName` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` INTEGER NOT NULL, `dueDateTimestamp` INTEGER, `note` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"""
        )

        // Columns that may be missing in an older table. Nullable columns get
        // no DEFAULT; NOT NULL columns must carry a DEFAULT so the ALTER on
        // a populated table succeeds.
        private val V2_ALL_COLUMN_ADDITIONS = mapOf(
            "accounts" to listOf(
                ColumnDef("name", "`name` TEXT NOT NULL DEFAULT ''"),
                ColumnDef("type", "`type` TEXT NOT NULL DEFAULT 'BANK'"),
                ColumnDef("balance", "`balance` INTEGER NOT NULL DEFAULT 0"),
                ColumnDef("currency", "`currency` TEXT NOT NULL DEFAULT 'تومان'"),
                ColumnDef("icon", "`icon` TEXT NOT NULL DEFAULT '🏦'"),
                ColumnDef("colorHex", "`colorHex` TEXT NOT NULL DEFAULT '#10B981'"),
                ColumnDef("isActive", "`isActive` INTEGER NOT NULL DEFAULT 1"),
                ColumnDef("createdAt", "`createdAt` INTEGER NOT NULL DEFAULT 0")
            ),
            "transactions" to listOf(
                ColumnDef("accountId", "`accountId` INTEGER NOT NULL DEFAULT 0"),
                ColumnDef("destinationAccountId", "`destinationAccountId` INTEGER"),
                ColumnDef("type", "`type` TEXT NOT NULL DEFAULT 'EXPENSE'"),
                ColumnDef("amount", "`amount` INTEGER NOT NULL DEFAULT 0"),
                ColumnDef("categoryId", "`categoryId` TEXT NOT NULL DEFAULT 'other_expense'"),
                ColumnDef("categoryName", "`categoryName` TEXT NOT NULL DEFAULT 'سایر هزینه‌ها'"),
                ColumnDef("categoryIcon", "`categoryIcon` TEXT NOT NULL DEFAULT '📦'"),
                ColumnDef("currency", "`currency` TEXT NOT NULL DEFAULT 'تومان'"),
                ColumnDef("dateTimestamp", "`dateTimestamp` INTEGER NOT NULL DEFAULT 0"),
                ColumnDef("note", "`note` TEXT NOT NULL DEFAULT ''"),
                ColumnDef("tags", "`tags` TEXT NOT NULL DEFAULT ''"),
                ColumnDef("isRecurring", "`isRecurring` INTEGER NOT NULL DEFAULT 0"),
                ColumnDef("createdAt", "`createdAt` INTEGER NOT NULL DEFAULT 0")
            ),
            "budgets" to listOf(
                ColumnDef("categoryId", "`categoryId` TEXT NOT NULL DEFAULT 'ALL'"),
                ColumnDef("categoryName", "`categoryName` TEXT NOT NULL DEFAULT ''"),
                ColumnDef("categoryIcon", "`categoryIcon` TEXT NOT NULL DEFAULT '📊'"),
                ColumnDef("monthlyLimit", "`monthlyLimit` INTEGER NOT NULL DEFAULT 0"),
                ColumnDef("monthYear", "`monthYear` TEXT NOT NULL DEFAULT ''")
            ),
            "goals" to listOf(
                ColumnDef("name", "`name` TEXT NOT NULL DEFAULT ''"),
                ColumnDef("targetAmount", "`targetAmount` INTEGER NOT NULL DEFAULT 0"),
                ColumnDef("currentAmount", "`currentAmount` INTEGER NOT NULL DEFAULT 0"),
                ColumnDef("targetDate", "`targetDate` INTEGER"),
                ColumnDef("icon", "`icon` TEXT NOT NULL DEFAULT '🎯'"),
                ColumnDef("colorHex", "`colorHex` TEXT NOT NULL DEFAULT '#3B82F6'"),
                ColumnDef("createdAt", "`createdAt` INTEGER NOT NULL DEFAULT 0")
            ),
            "debts" to listOf(
                ColumnDef("personName", "`personName` TEXT NOT NULL DEFAULT ''"),
                ColumnDef("type", "`type` TEXT NOT NULL DEFAULT 'OWE'"),
                ColumnDef("amount", "`amount` INTEGER NOT NULL DEFAULT 0"),
                ColumnDef("dueDateTimestamp", "`dueDateTimestamp` INTEGER"),
                ColumnDef("note", "`note` TEXT NOT NULL DEFAULT ''"),
                ColumnDef("status", "`status` TEXT NOT NULL DEFAULT 'PENDING'"),
                ColumnDef("createdAt", "`createdAt` INTEGER NOT NULL DEFAULT 0")
            )
        )

        private fun existingColumns(db: SupportSQLiteDatabase, table: String): Set<String> {
            val columns = mutableSetOf<String>()
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(nameIndex))
                }
            }
            return columns
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create any v2 table that does not exist yet (no-op otherwise).
                V2_TABLE_DDL.forEach { ddl -> db.execSQL(ddl) }

                // 2. Add any v2 column missing from an existing table.
                V2_ALL_COLUMN_ADDITIONS.forEach { (table, columns) ->
                    val existing = existingColumns(db, table)
                    columns.forEach { column ->
                        if (column.name !in existing) {
                            db.execSQL("ALTER TABLE `$table` ADD COLUMN ${column.ddl}")
                        }
                    }
                }
            }
        }

        fun getDatabase(context: Context): KharjYarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KharjYarDatabase::class.java,
                    "kharjyar_database"
                )
                    // Destructive migration removed: never silently wipe user financial
                    // data. Bump `version` and register a Migration object instead.
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

    }
}

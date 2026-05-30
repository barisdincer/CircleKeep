package com.example.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensureColumn(db, "people", "notes", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "people", "tags", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "people", "addedDate", "INTEGER NOT NULL DEFAULT 0")
            ensureColumn(db, "people", "lastInteractionDate", "INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS interaction_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    personId INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    FOREIGN KEY(personId) REFERENCES people(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensureColumn(db, "people", "normalizedPhoneNumber", "TEXT NOT NULL DEFAULT ''")
            ensureColumn(db, "people", "contactLookupKey", "TEXT")
            ensureColumn(db, "people", "reminderEnabled", "INTEGER NOT NULL DEFAULT 1")
            ensureColumn(db, "people", "lastCallLogSyncDate", "INTEGER")
            db.execSQL("UPDATE people SET normalizedPhoneNumber = phoneNumber WHERE normalizedPhoneNumber = ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_people_waveId ON people(waveId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_people_normalizedPhoneNumber ON people(normalizedPhoneNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_people_contactLookupKey ON people(contactLookupKey)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_interaction_logs_personId ON interaction_logs(personId)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            ensureColumn(db, "people", "snoozedUntilDate", "INTEGER")
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_interaction_logs_personId_timestamp_type
                ON interaction_logs(personId, timestamp, type)
                """.trimIndent()
            )
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

    private fun ensureColumn(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
        columnDefinition: String
    ) {
        if (!hasColumn(db, tableName, columnName)) {
            db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnDefinition")
        }
    }

    private fun hasColumn(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String
    ): Boolean {
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) return true
            }
        }
        return false
    }
}

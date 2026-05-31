package com.barisdincer.circlekeep.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migratePeopleToVersion2(db)
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
            db.execSQL(
                """
                UPDATE people
                SET normalizedPhoneNumber = CASE
                    WHEN LENGTH($PHONE_DIGITS_SQL) = 0 THEN ''
                    WHEN SUBSTR($PHONE_DIGITS_SQL, 1, 2) = '00' THEN SUBSTR($PHONE_DIGITS_SQL, 3)
                    WHEN LENGTH($PHONE_DIGITS_SQL) = 11 AND SUBSTR($PHONE_DIGITS_SQL, 1, 1) = '0'
                        THEN '90' || SUBSTR($PHONE_DIGITS_SQL, 2)
                    WHEN LENGTH($PHONE_DIGITS_SQL) = 10 THEN '90' || $PHONE_DIGITS_SQL
                    ELSE $PHONE_DIGITS_SQL
                END
                WHERE normalizedPhoneNumber = ''
                """.trimIndent()
            )
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
                DELETE FROM interaction_logs
                WHERE id NOT IN (
                    SELECT MIN(id)
                    FROM interaction_logs
                    GROUP BY personId, timestamp, type
                )
                """.trimIndent()
            )
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

    private const val PHONE_DIGITS_SQL =
        "REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(phoneNumber, ' ', ''), '-', ''), '(', ''), ')', ''), '+', ''), '.', ''), '/', '')"

    private fun migratePeopleToVersion2(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS people_migration_v2")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS people_migration_v2 (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                phoneNumber TEXT NOT NULL,
                waveId INTEGER,
                notes TEXT NOT NULL,
                tags TEXT NOT NULL,
                addedDate INTEGER NOT NULL,
                lastInteractionDate INTEGER NOT NULL,
                FOREIGN KEY(waveId) REFERENCES waves(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO people_migration_v2(
                id, name, phoneNumber, waveId, notes, tags, addedDate, lastInteractionDate
            )
            SELECT
                id,
                name,
                phoneNumber,
                ${columnOrDefault(db, "people", "waveId", "NULL")},
                ${columnOrDefault(db, "people", "notes", "''")},
                ${columnOrDefault(db, "people", "tags", "''")},
                ${columnOrDefault(db, "people", "addedDate", "0")},
                ${columnOrDefault(db, "people", "lastInteractionDate", "0")}
            FROM people
            """.trimIndent()
        )

        db.execSQL("DROP TABLE people")
        db.execSQL("ALTER TABLE people_migration_v2 RENAME TO people")
    }

    private fun columnOrDefault(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
        defaultValue: String
    ): String {
        return if (hasColumn(db, tableName, columnName)) columnName else defaultValue
    }

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

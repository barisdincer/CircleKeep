package com.barisdincer.circlekeep

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.barisdincer.circlekeep.data.AppDatabase
import com.barisdincer.circlekeep.data.DatabaseMigrations
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMigrationTest {
    @Test
    fun `migrates version 1 database to current schema without losing people`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val dbName = "migration-v1-to-v6-test.db"
            context.deleteDatabase(dbName)
            createVersion1Database(context, dbName)

            val database = openMigratedDatabase(context, dbName)

            try {
                val people = database.networkDao().getPeopleSnapshot()
                val logs = database.networkDao().getInteractionSnapshot()
                val rhythms = database.networkDao().getPersonContactRhythmSnapshot()

                assertEquals(1, people.size)
                assertEquals("Ayse", people.first().name)
                assertEquals("0532 123 45 67", people.first().phoneNumber)
                assertEquals("905321234567", people.first().normalizedPhoneNumber)
                assertEquals("", people.first().notes)
                assertEquals("", people.first().tags)
                assertEquals("CALL", people.first().preferredContactTypeKey)
                assertNull(people.first().customFrequencyDays)
                assertEquals("", people.first().memoryNotes)
                assertEquals("", people.first().nextConversationHint)
                assertEquals("", people.first().importantDateLabel)
                assertNull(people.first().importantDateMillis)
                assertTrue(people.first().reminderEnabled)
                assertEquals(0L, people.first().addedDate)
                assertEquals(0L, people.first().lastInteractionDate)
                assertNull(people.first().snoozedUntilDate)
                assertEquals(0, logs.size)
                assertEquals(listOf("CALL"), rhythms.map { it.contactTypeKey })
                assertEquals(listOf(1), rhythms.map { it.personId })
                assertEquals(listOf("CALL", "MESSAGE", "MEETING"), database.networkDao().getContactTypeSnapshot().map { it.key })
                assertNotNull(database.openHelper.readableDatabase)
            } finally {
                database.close()
                context.deleteDatabase(dbName)
            }
        }
    }

    @Test
    fun `migrates version 2 database to current schema without losing people`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val dbName = "migration-v2-to-v6-test.db"
            context.deleteDatabase(dbName)
            createVersion2Database(context, dbName)

            val database = openMigratedDatabase(context, dbName)

            try {
                val people = database.networkDao().getPeopleSnapshot()
                val logs = database.networkDao().getInteractionSnapshot()
                val rhythms = database.networkDao().getPersonContactRhythmSnapshot()

                assertEquals(1, people.size)
                assertEquals("Ayse", people.first().name)
                assertEquals("0532 123 45 67", people.first().phoneNumber)
                assertEquals("905321234567", people.first().normalizedPhoneNumber)
                assertEquals("CALL", people.first().preferredContactTypeKey)
                assertTrue(people.first().reminderEnabled)
                assertNull(people.first().snoozedUntilDate)
                assertEquals(1, logs.size)
                assertEquals(1, logs.first().id)
                assertEquals("", logs.first().note)
                assertEquals(listOf("CALL"), rhythms.map { it.contactTypeKey })
                assertEquals(2000L, rhythms.first().lastInteractionDate)
                assertNotNull(database.openHelper.readableDatabase)
            } finally {
                database.close()
                context.deleteDatabase(dbName)
            }
        }
    }

    @Test
    fun `migrates version 4 database to current schema with contact types rhythms and memory defaults`() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val dbName = "migration-v4-to-v6-test.db"
            context.deleteDatabase(dbName)
            createVersion4Database(context, dbName)

            val database = openMigratedDatabase(context, dbName)

            try {
                val people = database.networkDao().getPeopleSnapshot()
                val logs = database.networkDao().getInteractionSnapshot()
                val contactTypes = database.networkDao().getContactTypeSnapshot()
                val rhythms = database.networkDao().getPersonContactRhythmSnapshot()

                assertEquals(1, people.size)
                assertEquals("CALL", people.first().preferredContactTypeKey)
                assertNull(people.first().customFrequencyDays)
                assertEquals("", people.first().memoryNotes)
                assertEquals("", people.first().nextConversationHint)
                assertEquals("", people.first().importantDateLabel)
                assertNull(people.first().importantDateMillis)
                assertEquals(1, logs.size)
                assertEquals("MEETING", logs.first().type)
                assertEquals("", logs.first().note)
                assertEquals(listOf("CALL", "MEETING"), rhythms.map { it.contactTypeKey }.sorted())
                assertEquals(2000L, rhythms.first { it.contactTypeKey == "MEETING" }.lastInteractionDate)
                assertEquals(listOf("CALL", "MESSAGE", "MEETING"), contactTypes.map { it.key })
                assertTrue(contactTypes.all { it.isActive })
                assertNotNull(database.openHelper.readableDatabase)
            } finally {
                database.close()
                context.deleteDatabase(dbName)
            }
        }
    }

    private fun openMigratedDatabase(context: Context, dbName: String): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*DatabaseMigrations.ALL)
            .allowMainThreadQueries()
            .build()
    }

    private fun createVersion1Database(context: Context, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.execSQL(
            """
            CREATE TABLE waves (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                frequencyDays INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE people (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                phoneNumber TEXT NOT NULL,
                waveId INTEGER,
                FOREIGN KEY(waveId) REFERENCES waves(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO waves(id, name, frequencyDays) VALUES (1, 'Friends', 21)")
        db.execSQL(
            """
            INSERT INTO people(
                id, name, phoneNumber, waveId
            ) VALUES (
                1, 'Ayse', '0532 123 45 67', 1
            )
            """.trimIndent()
        )
        db.version = 1
        db.close()
    }

    private fun createVersion2Database(context: Context, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.execSQL(
            """
            CREATE TABLE waves (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                frequencyDays INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE people (
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
            CREATE TABLE interaction_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                personId INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                type TEXT NOT NULL,
                FOREIGN KEY(personId) REFERENCES people(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO waves(id, name, frequencyDays) VALUES (1, 'Friends', 21)")
        db.execSQL(
            """
            INSERT INTO people(
                id, name, phoneNumber, waveId, notes, tags, addedDate, lastInteractionDate
            ) VALUES (
                1, 'Ayse', '0532 123 45 67', 1, 'old note', 'friend', 1000, 2000
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO interaction_logs(id, personId, timestamp, type) VALUES (1, 1, 2000, 'CALL')")
        db.execSQL("INSERT INTO interaction_logs(id, personId, timestamp, type) VALUES (2, 1, 2000, 'CALL')")
        db.version = 2
        db.close()
    }

    private fun createVersion4Database(context: Context, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.execSQL(
            """
            CREATE TABLE waves (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                frequencyDays INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE people (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                phoneNumber TEXT NOT NULL,
                normalizedPhoneNumber TEXT NOT NULL DEFAULT '',
                contactLookupKey TEXT,
                waveId INTEGER,
                notes TEXT NOT NULL,
                tags TEXT NOT NULL,
                reminderEnabled INTEGER NOT NULL DEFAULT 1,
                addedDate INTEGER NOT NULL,
                lastInteractionDate INTEGER NOT NULL,
                lastCallLogSyncDate INTEGER,
                snoozedUntilDate INTEGER,
                FOREIGN KEY(waveId) REFERENCES waves(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE interaction_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                personId INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                type TEXT NOT NULL,
                FOREIGN KEY(personId) REFERENCES people(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_people_waveId ON people(waveId)")
        db.execSQL("CREATE INDEX index_people_normalizedPhoneNumber ON people(normalizedPhoneNumber)")
        db.execSQL("CREATE INDEX index_people_contactLookupKey ON people(contactLookupKey)")
        db.execSQL("CREATE INDEX index_interaction_logs_personId ON interaction_logs(personId)")
        db.execSQL("CREATE UNIQUE INDEX index_interaction_logs_personId_timestamp_type ON interaction_logs(personId, timestamp, type)")
        db.execSQL("INSERT INTO waves(id, name, frequencyDays) VALUES (1, 'Friends', 21)")
        db.execSQL(
            """
            INSERT INTO people(
                id, name, phoneNumber, normalizedPhoneNumber, contactLookupKey, waveId,
                notes, tags, reminderEnabled, addedDate, lastInteractionDate, lastCallLogSyncDate, snoozedUntilDate
            ) VALUES (
                1, 'Ayse', '0532 123 45 67', '905321234567', NULL, 1,
                'old note', 'friend', 1, 1000, 2000, NULL, NULL
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO interaction_logs(id, personId, timestamp, type) VALUES (1, 1, 2000, 'MEETING')")
        db.version = 4
        db.close()
    }
}

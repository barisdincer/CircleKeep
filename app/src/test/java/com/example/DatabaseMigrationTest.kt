package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DatabaseMigrations
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {
    @Test
    fun `migrates version 2 database to current schema without losing people`() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration-v2-to-v4-test.db"
        context.deleteDatabase(dbName)
        createVersion2Database(context, dbName)

        val database = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*DatabaseMigrations.ALL)
            .allowMainThreadQueries()
            .build()

        val people = database.networkDao().getPeopleSnapshot()
        val logs = database.networkDao().getInteractionSnapshot()

        assertEquals(1, people.size)
        assertEquals("Ayse", people.first().name)
        assertEquals("0532 123 45 67", people.first().phoneNumber)
        assertEquals("0532 123 45 67", people.first().normalizedPhoneNumber)
        assertEquals(true, people.first().reminderEnabled)
        assertEquals(null, people.first().snoozedUntilDate)
        assertEquals(1, logs.size)
        assertNotNull(database.openHelper.readableDatabase)

        database.close()
        context.deleteDatabase(dbName)
        }
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
        db.version = 2
        db.close()
    }
}

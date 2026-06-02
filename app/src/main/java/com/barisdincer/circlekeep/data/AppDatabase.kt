package com.barisdincer.circlekeep.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Wave::class, ContactType::class, Person::class, PersonContactRhythm::class, InteractionLog::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun networkDao(): NetworkDao
}

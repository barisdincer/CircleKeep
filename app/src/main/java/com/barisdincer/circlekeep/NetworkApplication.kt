package com.barisdincer.circlekeep

import android.app.Application
import androidx.room.Room
import com.barisdincer.circlekeep.data.AppDatabase
import com.barisdincer.circlekeep.data.DatabaseMigrations
import com.barisdincer.circlekeep.data.NetworkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NetworkApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: NetworkRepository
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "circlekeep_database"
        )
        .addMigrations(*DatabaseMigrations.ALL)
        .build()
        repository = NetworkRepository(database.networkDao())
        applicationScope.launch {
            repository.ensureDefaultContactTypes()
            repository.ensureDefaultWaves()
        }
    }
}

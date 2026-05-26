package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.NetworkRepository

class NetworkApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: NetworkRepository

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "network_database"
        )
        .fallbackToDestructiveMigration()
        .build()
        repository = NetworkRepository(database.networkDao())
    }
}

package org.example.project.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Android implementation of database builder
 */
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val appContext = getApplicationContext()
    val dbFile = appContext.getDatabasePath(AppDatabase.DATABASE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

/**
 * Get application context - this will be set from MainActivity
 */
private lateinit var applicationContext: Context

fun initializeDatabase(context: Context) {
    applicationContext = context.applicationContext
}

fun getApplicationContext(): Context {
    return applicationContext
}


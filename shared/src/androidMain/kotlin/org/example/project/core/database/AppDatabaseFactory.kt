package org.example.project.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

class AppDatabaseFactory(
    private val context: Context,
) {
    fun <T : RoomDatabase> createDatabase(
        databaseClass: Class<T>,
        databaseName: String,
    ): RoomDatabase.Builder<T> =
        Room.databaseBuilder(
            context.applicationContext,
            databaseClass,
            databaseName,
        )
}

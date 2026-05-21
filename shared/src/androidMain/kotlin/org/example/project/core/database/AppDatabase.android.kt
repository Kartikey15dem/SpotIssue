package org.example.project.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Android implementation of database builder
 */
//actual fun getDatabaseBuilder(): RoomDatabase.Builder<IssueSpotDatabase> {
//    val appContext = getApplicationContext()
//    val dbFile = appContext.getDatabasePath(IssueSpotDatabase.Companion.DATABASE_NAME)
//    return Room.databaseBuilder<IssueSpotDatabase>(
//        context = appContext,
//        name = dbFile.absolutePath
//    )
//}
//
///**
// * Get application context - this will be set from MainActivity
// */
private lateinit var applicationContext: Context

fun initializeDatabase(context: Context) {
    applicationContext = context.applicationContext
}

fun getApplicationContext(): Context {
    return applicationContext
}


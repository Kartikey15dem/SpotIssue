package org.example.project.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.ConstructedBy
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.example.project.core.data.local.dao.ProfileDao
import org.example.project.core.data.local.dao.PostDao
import org.example.project.core.data.local.dao.CacheMetadataDao
import org.example.project.core.data.local.dao.ActiveIssuesDao
import org.example.project.core.data.local.dao.UserPostDao
import org.example.project.core.data.local.dao.LikedPostDao
import org.example.project.core.data.local.entities.ProfileEntity
import org.example.project.core.data.local.entities.PostEntity
import org.example.project.core.data.local.entities.CacheMetadataEntity
import org.example.project.core.data.local.entities.ActiveIssuesEntity
import org.example.project.core.data.local.entities.UserPostEntity
import org.example.project.core.data.local.entities.LikedPostEntity

@Database(
    entities = [
        ProfileEntity::class,
        PostEntity::class,
        CacheMetadataEntity::class,
        ActiveIssuesEntity::class,
        UserPostEntity::class,
        LikedPostEntity::class
    ],
    version = 3,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun postDao(): PostDao
    abstract fun cacheMetadataDao(): CacheMetadataDao
    abstract fun activeIssuesDao(): ActiveIssuesDao
    abstract fun userPostDao(): UserPostDao
    abstract fun likedPostDao(): LikedPostDao

    companion object {
        const val DATABASE_NAME = "issuespot.db"
    }
}


@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

fun getDatabase(): AppDatabase {
    return getDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true) // For development - will recreate DB on schema changes
        .build()
}

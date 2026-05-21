package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.example.project.core.database.entities.CacheMetadataEntity

@Dao
interface CacheMetadataDao {
    /**
     * Get cache metadata by key
     */
    @Query("SELECT * FROM cache_metadata WHERE cacheKey = :key")
    suspend fun getMetadata(key: String): CacheMetadataEntity?

    /**
     * Insert or update cache metadata
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: CacheMetadataEntity)

    /**
     * Delete metadata by key
     */
    @Query("DELETE FROM cache_metadata WHERE cacheKey = :key")
    suspend fun deleteMetadata(key: String)

    /**
     * Clear all metadata
     */
    @Query("DELETE FROM cache_metadata")
    suspend fun clearAll()
}


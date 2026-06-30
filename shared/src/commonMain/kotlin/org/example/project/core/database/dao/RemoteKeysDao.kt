package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.example.project.core.database.entities.RemoteKeysEntity

@Dao
interface RemoteKeysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKey: List<RemoteKeysEntity>)

    @Query("SELECT * FROM remote_keys WHERE id = :id AND type = :type")
    suspend fun remoteKeysId(id: String, type: String): RemoteKeysEntity?

    @Query("DELETE FROM remote_keys WHERE type = :type")
    suspend fun clearRemoteKeys(type: String)

}

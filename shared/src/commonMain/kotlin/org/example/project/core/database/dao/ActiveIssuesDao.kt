package org.example.project.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.example.project.core.database.entities.ActiveIssuesEntity

@Dao
interface ActiveIssuesDao {
    /**
     * Get active issues count for a specific post level
     */
    @Query("SELECT * FROM active_issues WHERE postLevel = :postLevel")
    suspend fun getActiveIssues(postLevel: String): ActiveIssuesEntity?

    /**
     * Observe active issues count for a specific post level
     */
    @Query("SELECT * FROM active_issues WHERE postLevel = :postLevel")
    fun observeActiveIssues(postLevel: String): Flow<ActiveIssuesEntity?>

    /**
     * Insert or update active issues count
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveIssues(activeIssues: ActiveIssuesEntity)

    /**
     * Delete active issues for a specific level
     */
    @Query("DELETE FROM active_issues WHERE postLevel = :postLevel")
    suspend fun deleteActiveIssues(postLevel: String)

    /**
     * Clear all active issues
     */
    @Query("DELETE FROM active_issues")
    suspend fun clearAll()
}

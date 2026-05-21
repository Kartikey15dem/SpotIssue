package org.example.project.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

/**
 * Entity to store active issues count for each PostLevel
 */
@Entity(tableName = "active_issues")
data class ActiveIssuesEntity(
    @PrimaryKey
    val postLevel: String, // LOCALITY, DISTRICT, STATE, NATIONAL
    val count: Int,
    val cachedAt: Long = Clock.System.now().toEpochMilliseconds()
)


package org.example.project.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "remote_keys",
    primaryKeys = ["id", "type"]
)
data class RemoteKeysEntity(
    val id: String,
    val prevKey: Int?,
    val nextKey: Int?,
    val type: String // e.g., "FEED", "USER_POSTS", "LIKED_POSTS"
)

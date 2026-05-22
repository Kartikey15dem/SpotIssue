package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for Active Issues Count from Supabase
 */
@Serializable
data class ActiveIssuesDto(
    @SerialName("level")
    val level: String,

    @SerialName("total_active_issues")
    val totalActiveIssues: Int,

    @SerialName("updated_at")
    val updatedAt: String? = null
)
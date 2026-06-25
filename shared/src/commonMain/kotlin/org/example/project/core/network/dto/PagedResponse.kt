package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    @SerialName("items")
    val items: List<T>,
    @SerialName("prev_key")
    val prevKey: Int?,
    @SerialName("next_key")
    val nextKey: Int?,
    @SerialName("active_issues_count")
    val activeIssuesCount: Int? = null
)

package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    @SerialName("items")
    val items: List<T>,
    @SerialName("prevKey")
    val prevKey: Int?,
    @SerialName("nextKey")
    val nextKey: Int?,
    @SerialName("active_issues_count")
    val activeIssuesCount: Int? = null
)

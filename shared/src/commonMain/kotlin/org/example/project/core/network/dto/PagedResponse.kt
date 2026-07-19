package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    // The actual list of items (e.g., Posts, Comments) for the current page.
    @SerialName("items")
    val items: List<T>,
    // The key (often an index or a cursor) to fetch the previous page.
    // Null means we are at the very beginning (page 0).
    @SerialName("prev_key")
    val prevKey: Int?,
    // The key to fetch the next page.
    // If this is null, it means there are no more items left to load (hasMore = false).
    @SerialName("next_key")
    val nextKey: Int?,
    // Extra metadata that can optionally come with the paginated response.
    @SerialName("active_issues_count")
    val activeIssuesCount: Int? = null,
)

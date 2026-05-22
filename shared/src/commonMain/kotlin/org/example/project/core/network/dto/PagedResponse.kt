package org.example.project.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val prevKey: Int?,
    val nextKey: Int?
)

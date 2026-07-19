package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoordinatesDto(
    @SerialName("latitude")
    val latitude: Double,
    @SerialName("longitude")
    val longitude: Double,
)

@Serializable
data class ProfileDto(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("image_url")
    val imageUrl: String?,
    @SerialName("total_posts")
    val totalPosts: Int,
    @SerialName("acks")
    val acks: Int,
    @SerialName("posts_by_level")
    val postByArea: List<Int>? = null,
)

@Serializable
data class UpsertProfileRequest(
    val name: String,
    val email: String,
    @SerialName("image_url")
    val imageUrl: String? = null,
)

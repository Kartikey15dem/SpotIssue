package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Post from Supabase
 */
@Serializable
data class PostDto(
    @SerialName("id")
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("post_level")
    val postLevel: String, // LOCALITY, DISTRICT, STATE, NATIONAL

    @SerialName("post_text")
    val postText: String,

    @SerialName("media_type")
    val mediaType: String, // IMAGE, VIDEO, GIF

    @SerialName("media_url")
    val mediaUrl: String? = null,

    @SerialName("likes")
    val likes: Int = 0,

    @SerialName("comments")
    val comments: Int = 0,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("locality")
    val locality: String? = null,

    @SerialName("district")
    val district: String? = null,

    @SerialName("state")
    val state: String? = null,

    @SerialName("country")
    val country: String? = null,

    @SerialName("coordinates")
    val coordinates: CoordinatesDto? = null
)

/**
 * Profile info embedded in post response
 */
@Serializable
data class PostWithProfileDto(
    @SerialName("id")
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("post_level")
    val postLevel: String,

    @SerialName("post_text")
    val postText: String,

    @SerialName("media_type")
    val mediaType: String,

    @SerialName("media_url")
    val mediaUrl: String? = null,

    @SerialName("likes")
    val likes: Int = 0,

    @SerialName("comments")
    val comments: Int = 0,

    @SerialName("created_at")
    val createdAt: String,

    // Profile info
    @SerialName("profiles")
    val profile: ProfileInfoDto? = null,

    @SerialName("locality")
    val locality: String? = null,

    @SerialName("district")
    val district: String? = null,

    @SerialName("state")
    val state: String? = null,

    @SerialName("country")
    val country: String? = null,

    @SerialName("coordinates")
    val coordinates: CoordinatesDto? = null
)

@Serializable
data class ProfileInfoDto(
    @SerialName("id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("image_url")
    val imageUrl: String?
)

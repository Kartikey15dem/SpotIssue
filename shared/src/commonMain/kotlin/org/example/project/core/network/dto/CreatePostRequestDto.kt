package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePostRequestDto(
    @SerialName("post_level")
    val postLevel: String,
    @SerialName("post_text")
    val postText: String,
    @SerialName("media_type")
    val mediaType: String,
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

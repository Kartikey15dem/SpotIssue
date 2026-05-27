package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentDto(
    @SerialName("id") val id: String,
    @SerialName("post_id") val postId: String,
    @SerialName("comment_text") val commentText: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("profile") val profile: ProfileInfoDto? = null
)

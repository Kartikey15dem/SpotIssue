package org.example.project.core.model.home

import org.example.project.core.network.dto.CommentDto
import org.example.project.core.utils.getRelativeTime

data class Comment(
    val id : String,
    val postId: String,
    val text: String,
    val timeAgo: String,
    val userName: String,
    val userImageUrl: String?
)

fun CommentDto.toComment(): Comment {
    return Comment(
        id = this.id,
        postId = this.postId,
        text = this.commentText,
        timeAgo = getRelativeTime(this.createdAt),
        userName = this.profile?.name ?: "Unknown User",
        userImageUrl = this.profile?.imageUrl
    )
}

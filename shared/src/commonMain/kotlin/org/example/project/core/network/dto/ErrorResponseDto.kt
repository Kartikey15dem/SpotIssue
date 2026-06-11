package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    @SerialName("developer_message")
    val developerMessage: String? = null,
    @SerialName("user_message")
    val userMessage: String? = null,
)

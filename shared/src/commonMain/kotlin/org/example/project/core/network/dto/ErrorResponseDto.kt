package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    @SerialName("developerMessage")
    val developerMessage: String? = null,
    @SerialName("userMessage")
    val userMessage: String? = null,
)

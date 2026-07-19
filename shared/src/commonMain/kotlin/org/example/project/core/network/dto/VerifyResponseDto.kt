package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyResponseDto(
    val token: String,
    @SerialName("is_new_user")
    val isNewUser: Boolean,
)

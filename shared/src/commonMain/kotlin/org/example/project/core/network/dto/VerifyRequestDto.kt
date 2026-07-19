package org.example.project.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyRequestDto(
    val email: String,
    val otp: String,
)

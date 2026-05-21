package org.example.project.core.network.dto

data class VerifyRequestDto(
    val email : String,
    val otp : String
)
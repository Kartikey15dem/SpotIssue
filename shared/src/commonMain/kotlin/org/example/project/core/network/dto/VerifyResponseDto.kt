package org.example.project.core.network.dto

data class VerifyResponseDto(
    val token : String,
    val isNewUser : Boolean
)
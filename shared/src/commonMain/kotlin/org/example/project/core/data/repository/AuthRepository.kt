package org.example.project.core.data.repository

import org.example.project.core.network.dto.AuthRequestOtpResponse
import org.example.project.core.network.dto.VerifyResponseDto
import org.example.project.core.utils.DataState

interface AuthRepository {
    suspend fun requestOtp(email: String): DataState<AuthRequestOtpResponse>

    suspend fun verifyOtp(
        email: String,
        otp: String,
    ): DataState<VerifyResponseDto>
}

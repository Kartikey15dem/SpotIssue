package org.example.project.core.network.services

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST
import org.example.project.core.network.dto.AuthRequestOtpResponse
import org.example.project.core.network.dto.LoginRequestDto
import org.example.project.core.network.dto.VerifyRequestDto
import org.example.project.core.network.dto.VerifyResponseDto
import org.example.project.core.utils.ApiEndPoints

interface AuthenticationService {
    @POST(ApiEndPoints.AUTHENTICATION + "/otp/request")
    suspend fun requestOtp(
        @Body request: LoginRequestDto,
    ): AuthRequestOtpResponse

    @POST(ApiEndPoints.AUTHENTICATION + "/otp/verify")
    suspend fun verifyOtp(
        @Body request: VerifyRequestDto,
    ): VerifyResponseDto
}

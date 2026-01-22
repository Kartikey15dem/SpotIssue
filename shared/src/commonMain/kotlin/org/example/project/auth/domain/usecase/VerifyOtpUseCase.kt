package org.example.project.auth.domain.usecase

import org.example.project.auth.domain.repository.AuthRepository

class VerifyOtpUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phoneNumber: String, otp: String): Result<String> {
        // Validate OTP
        if (otp.length != 6) {
            return Result.failure(Exception("Invalid OTP"))
        }

        return authRepository.verifyOtp(phoneNumber, otp)
    }
}


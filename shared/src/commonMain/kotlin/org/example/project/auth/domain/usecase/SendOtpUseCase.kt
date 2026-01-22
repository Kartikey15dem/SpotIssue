package org.example.project.auth.domain.usecase
import org.example.project.auth.domain.repository.AuthRepository

class SendOtpUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Boolean> {
        // Validate phone number
//        if (phoneNumber.length != 10) {
//            return Result.failure(Exception("Invalid phone number"))
//        }

        return authRepository.sendOtp(email)
    }
}



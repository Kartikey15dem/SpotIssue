package org.example.project.auth.domain.repository

interface AuthRepository {
    suspend fun sendOtp(email: String): Result<Boolean>
    suspend fun verifyOtp(email: String, otp: String): Result<String>
}

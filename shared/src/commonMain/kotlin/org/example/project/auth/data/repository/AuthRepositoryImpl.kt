package org.example.project.auth.data.repository

import co.touchlab.kermit.Logger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.example.project.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val supabase: SupabaseClient
) : AuthRepository {
    private val logger = Logger.withTag("AppLogger")

    override suspend fun sendOtp(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        logger.d { "sendOtp called for email=$email" }
        try {
            supabase.auth.signInWith(OTP)  {
                this.email = email
            }
            logger.d { "OTP sent successfully to email=$email" }
            return@withContext Result.success(true)

        } catch (t: Throwable) {
            logger.e(t) { "Failed to send OTP to email=$email" }
            return@withContext Result.failure(t)
        }
    }

    override suspend fun verifyOtp(email: String, otp: String): Result<String> = withContext(Dispatchers.IO) {
        logger.d { "verifyOtp called for email=$email" }
        try {
            supabase.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = otp)
            logger.d { "OTP verified successfully for email=$email" }
            return@withContext Result.success("Verified Successfully")

        } catch (t: Throwable) {
            logger.e(t) { "Failed to verify OTP for email=$email" }
            return@withContext Result.failure(t)
        }
    }

}

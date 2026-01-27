package org.example.project.auth.data.repository


import org.example.project.auth.domain.repository.AuthRepository

/**
 * A fake implementation of AuthRepository for Unit Tests and UI Previews.
 * It stores state in memory and allows forcing errors to test failure UI.
 */
class FakeAuthRepository : AuthRepository {

    // --- Test Configuration ---

    // Set this to true in your test to simulate a network/api exception
    var shouldReturnError: Boolean = false

    // The specific error to throw if shouldReturnError is true
    var exceptionToThrow: Throwable = Exception("Network error simulation")

    // The hardcoded OTP to check against (default is "111111")
    var validOtp: String = "111111"

    // --- State Inspection ---

    // Keeps track of which emails have "received" an OTP
    val sentOtps = mutableSetOf<String>()

    override suspend fun sendOtp(email: String): Result<Boolean> {
        if (shouldReturnError) {
            return Result.failure(exceptionToThrow)
        }

        // Simulate logic: Add email to the "sent" list
        sentOtps.add(email)
        return Result.success(true)
    }

    override suspend fun verifyOtp(email: String, otp: String): Result<String> {
        if (shouldReturnError) {
            return Result.failure(exceptionToThrow)
        }

        // 1. Check if we actually asked for an OTP for this email
        if (!sentOtps.contains(email)) {
            return Result.failure(Exception("No OTP requested for this email"))
        }

        // 2. Check if the code matches our "valid" test code
        return if (otp == validOtp) {
            Result.success("Verified Successfully")
        } else {
            Result.failure(Exception("Invalid OTP Code"))
        }
    }

    // Helper to reset state between tests
    fun reset() {
        shouldReturnError = false
        sentOtps.clear()
        validOtp = "111111"
    }
}
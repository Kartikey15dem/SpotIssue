package org.example.project.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.auth.domain.usecase.SendOtpUseCase
import org.example.project.auth.domain.usecase.VerifyOtpUseCase
import org.example.project.core.settings.AuthSettings

class AuthViewModel(
    private val sendOtpUseCase: SendOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val authSettings: AuthSettings,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            error = null
        )
    }

    fun sendOtp(onSuccess: (String) -> Unit) {
        val email = _uiState.value.email
        if (!email.contains("@") || !email.contains(".")) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter a valid email address"
            )
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            sendOtpUseCase(email)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, otpSent = true)
                    onSuccess(email)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to send OTP"
                    )
                }
        }
    }

    fun onOtpChange(otp: String) {
        val filteredOtp = otp.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(
            otp = filteredOtp,
            error = null
        )
    }

    fun verifyOtp(onSuccess: () -> Unit) {
        val email = _uiState.value.email
        val otp = _uiState.value.otp
        if (otp.length != 6) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter a valid 6-digit OTP"
            )
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            verifyOtpUseCase(email, otp)
                .onSuccess {
                    authSettings.setLoggedIn(true)
                    _uiState.value = _uiState.value.copy(isLoading = false, otpVerified = true)
                    onSuccess()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to verify OTP"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

// Combined UI state for login and OTP

data class AuthUiState(
    val email: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpSent: Boolean = false,
    val otpVerified: Boolean = false
)

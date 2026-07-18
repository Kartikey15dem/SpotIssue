package org.example.project.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.data.repository.AuthRepository
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.utils.DataState
import org.example.project.core.datastore.UserPreferencesRepository

sealed class AuthEffect {
    data class NavigateToOtpScreen(val email: String) : AuthEffect()
    data class NavigateToNameCaptureScreen(val email: String) : AuthEffect()
    data class ShowDialog(val message: String) : AuthEffect()
}

data class AuthUiState(
    val email: String = "",
    val otp: String = "",
    val isLoading : Boolean = false,
)

sealed class AuthIntent{
    data class EmailChanged(val email: String) : AuthIntent()
    data class OtpChanged(val otp: String) : AuthIntent()
    data object SendOtpClicked : AuthIntent()
    data object VerifyOtpClicked : AuthIntent()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val prefRepository: UserPreferencesRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effect = Channel<AuthEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged -> updateEmail(intent.email)
            is AuthIntent.OtpChanged -> updateOtp(intent.otp)
            is AuthIntent.SendOtpClicked -> sendOtp()
            is AuthIntent.VerifyOtpClicked -> verifyOtp()
        }
    }

    private fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    private fun updateOtp(otp: String) {
        val filteredOtp = otp.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otp = filteredOtp) }
    }

    private fun sendOtp() {
        val email = _uiState.value.email

        if (!email.contains("@") || !email.contains(".")) {
            showError("Invalid email\n\nPlease ensure you have entered a correct and valid email address.")
            return
        }

        viewModelScope.launch {
            showLoading()

            when (val result = authRepository.requestOtp(email)) {
                is DataState.Success -> {
                    hideLoading()
                    _effect.send(AuthEffect.NavigateToOtpScreen(email))
                }
                is DataState.Error -> {
                    hideLoading()
                    showError(result.exception.message ?: "An unexpected error occurred\n\nPlease try again.")
                }
                DataState.Loading -> {
                    showLoading()
                }
            }
        }
    }

    private fun verifyOtp() {
        val email = _uiState.value.email
        val otp = _uiState.value.otp

        if (otp.length != 6) {
            showError("Invalid OTP\n\nPlease enter a valid 6-digit OTP.")
            return
        }

        viewModelScope.launch {
            showLoading()

            when (val result = authRepository.verifyOtp(email, otp)) {
                is DataState.Success -> {
                    val isNewUser = result.data.isNewUser
                    if(isNewUser) _effect.send(AuthEffect.NavigateToNameCaptureScreen(email))
                    else {
                        when(val res = profileRepository.refreshProfile()){
                            is DataState.Error -> {
                                showError(res.exception.message ?: "An unexpected error occurred\n\nPlease try again.")
                            }
                            DataState.Loading -> {
                                showLoading()
                            }
                            is DataState.Success -> {
                                hideLoading()
                                prefRepository.setLoggedIn(true)
                            }
                        }

                    }

                }
                is DataState.Error -> {
                    hideLoading()
                    showError(result.exception.message ?: "An unexpected error occurred\n\nPlease try again.")
                }
                DataState.Loading -> {
                    showLoading()
                }
            }
        }
    }


    private fun showLoading() {
        _uiState.update { it.copy(isLoading = true)  }
    }

    private fun hideLoading() {
        _uiState.update { it.copy(isLoading = false) }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _effect.send(AuthEffect.ShowDialog(message))
        }
    }
}

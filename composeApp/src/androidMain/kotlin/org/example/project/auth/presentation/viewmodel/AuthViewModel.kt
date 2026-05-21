package org.example.project.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.data.repository.AuthRepository
import org.example.project.core.utils.DataState
import org.example.project.core.datastore.UserPreferencesRepository

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val prefRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AuthEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val effect: SharedFlow<AuthEffect> = _effect.asSharedFlow()

    var isNewUser = true

    fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged -> updateEmail(intent.email)
            is AuthIntent.OtpChanged -> updateOtp(intent.otp)
            is AuthIntent.SendOtpClicked -> sendOtp()
            is AuthIntent.VerifyOtpClicked -> verifyOtp()
            is AuthIntent.DismissDialog -> clearDialog()
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
            showError("Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            showLoading()

            when (val result = authRepository.requestOtp(email)) {
                is DataState.Success -> {
                    hideLoading()
                    _effect.emit(AuthEffect.NavigateToOtpScreen(email))
                }
                is DataState.Error -> {
                    showError(result.exception.message ?: "An unexpected error occurred")
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
            showError("Please enter a valid 6-digit OTP")
            return
        }

        viewModelScope.launch {
            showLoading()

            when (val result = authRepository.verifyOtp(email, otp)) {
                is DataState.Success -> {
                    isNewUser = result.data.isNewUser
                    hideLoading()
                    if(isNewUser) _effect.emit(AuthEffect.NavigateToNextScreen)
                    else {
                        // update profile
                        prefRepository.setLoggedIn(true)
                    }

                }
                is DataState.Error -> {
                    showError(result.exception.message ?: "An unexpected error occurred")
                }
                DataState.Loading -> {
                    showLoading()
                }
            }
        }
    }

    private fun clearDialog() {
        _uiState.update { it.copy(dialogState = null) }
    }

    private fun showLoading() {
        _uiState.update { it.copy(dialogState = AuthUiState.DialogState.Loading) }
    }

    private fun hideLoading() {
        _uiState.update { it.copy(dialogState = null) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(dialogState = AuthUiState.DialogState.Error(message)) }
    }
}

sealed class AuthIntent {
    data class EmailChanged(val email: String) : AuthIntent()
    data class OtpChanged(val otp: String) : AuthIntent()
    data object SendOtpClicked : AuthIntent()
    data object VerifyOtpClicked : AuthIntent()
    data object DismissDialog : AuthIntent()
}

sealed class AuthEffect {
    data class NavigateToOtpScreen(val email: String) : AuthEffect()
    data object NavigateToNextScreen : AuthEffect()
}

data class AuthUiState(
    val email: String = "",
    val otp: String = "",
    val dialogState: DialogState? = null
) {
    sealed interface DialogState {
        data class Error(val message: String) : DialogState
        data object Loading : DialogState
    }
}
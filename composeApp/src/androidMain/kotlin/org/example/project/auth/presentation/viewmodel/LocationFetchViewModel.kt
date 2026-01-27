package org.example.project.auth.presentation.viewmodel

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.auth.domain.models.UserLocation
import org.example.project.auth.presentation.screens.LocationFetchStep
import org.example.project.auth.presentation.screens.LocationFetchUiState
import org.example.project.profile.domain.repository.ProfileRepository
import org.example.project.utils.LocationProvider

class LocationFetchViewModel(
    private val locationProvider: LocationProvider,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationFetchUiState())
    val uiState: StateFlow<LocationFetchUiState> = _uiState.asStateFlow()

    private var userName: String = ""
    private var userEmail: String = ""

    fun setUserData(name: String, email: String) {
        userName = name
        userEmail = email
    }

    fun startLocationFlow() {
        viewModelScope.launch {
            try {
                fetchLocation()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start location flow: ${e.message}"
                )
            }
        }
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            error = "Location permission is required to continue"
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun fetchLocation() {
        try {
            // Try to get location, with retries to allow for permission dialog
            var userLocation: UserLocation? = null
            var attempts = 0
            val maxAttempts = 10 // Retry up to 10 times (10 seconds)

            while (userLocation == null && attempts < maxAttempts) {
                userLocation = locationProvider.getCurrentLocation()
                if (userLocation == null) {
                    delay(1000) // Wait 1 second before retrying
                    attempts++
                }
            }

            if (userLocation == null) {
                _uiState.value = _uiState.value.copy(
                    error = "Unable to get location. Please check location permissions."
                )
                return
            }

            // Simple one-line formatting similar to other apps: "Home - flat, street, area..."
            val formattedAddress = userLocation.address

            // Update UI to show completion
            _uiState.value = _uiState.value.copy(
                currentStep = LocationFetchStep.COMPLETED,
                address = formattedAddress,
                isCompleted = false
            )

            // Save profile to Room database
            val result = profileRepository.upsertProfile(
                name = userName,
                imageUrl = null, // No image yet
                locality = userLocation.city, // Using city as locality
                district = userLocation.city, // Using city as district
                state = userLocation.state,
                country = userLocation.country
            )

            if (result.isSuccess) {
                // Mark as completed after successful save
                _uiState.value = _uiState.value.copy(isCompleted = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save profile: ${result.exceptionOrNull()?.message}"
                )
            }

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to fetch location: ${e.message}"
            )
        }
    }
}

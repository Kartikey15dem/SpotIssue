package org.example.project.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.model.auth.UserLocation
import org.example.project.core.utils.LocationProvider

class LocationFetchViewModel(
    private val locationProvider: LocationProvider,
    private val prefRepository: UserPreferencesRepository
) : ViewModel() {

    /* ===================================================================================
     * SECTION: LOCATION FETCHING STATE MACHINE
     * ===================================================================================
     * This ViewModel manages the complex lifecycle of obtaining high-accuracy GPS data.
     * 
     * States:
     * - FETCHING: Actively polling the LocationTracker.
     * - ERROR: Handles varied failure modes (Permissions Denied, GPS Disabled, 
     *   Network Timeout) and provides actionable recovery paths to the user.
     * - COMPLETED: Geocodes coordinates to a readable address and saves the 
     *   hierarchical data (locality, district, state, country) to the local DataStore.
     */

    private val _uiState = MutableStateFlow(LocationFetchUiState())
    val uiState: StateFlow<LocationFetchUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LocationFetchEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val effect: SharedFlow<LocationFetchEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: LocationFetchIntent) {
        when (intent) {
            is LocationFetchIntent.StartLocationFlow -> startFetching()
            is LocationFetchIntent.ShowRationale -> showError(LocationErrorState.RATIONALE)
            is LocationFetchIntent.PermissionDenied -> showError(LocationErrorState.PERMISSION_DENIED)
            is LocationFetchIntent.GpsDisabled -> showError(LocationErrorState.GPS_DISABLED)
            is LocationFetchIntent.ActionClicked -> handleActionClicked()
            is LocationFetchIntent.RetryClicked -> startFetching()
            is LocationFetchIntent.ContinueWithoutLocation -> handleContinueWithoutLocation()
        }
    }

    private fun startFetching() {
        _uiState.update {
            it.copy(
                currentStep = LocationFetchStep.FETCHING,
                errorState = null
            )
        }

        viewModelScope.launch {
            try {
                var userLocation: UserLocation? = null
                var attempts = 0

                while (userLocation == null && attempts < 3) {
                    userLocation = locationProvider.getCurrentLocation()
                    if (userLocation == null) {
                        delay(1000)
                        attempts++
                    }
                }

                if (userLocation == null) {
                    showError(LocationErrorState.FETCH_FAILED)
                    return@launch
                }


                val formattedAddress = userLocation.address

                prefRepository.updateUserLocation(userLocation)

                _uiState.update { state ->
                    state.copy(
                        currentStep = LocationFetchStep.COMPLETED,
                        address = formattedAddress,
                        isCompleted = true
                    )
                }
                _effect.emit(LocationFetchEffect.NavigateToNextScreen)


            } catch (e: Exception) {
                showError(LocationErrorState.FETCH_FAILED)
            }
        }
    }

    private fun handleActionClicked() {
        viewModelScope.launch {
            when (_uiState.value.errorState) {
                LocationErrorState.RATIONALE, LocationErrorState.PERMISSION_DENIED ->
                    _effect.emit(LocationFetchEffect.OpenAppSettings)
                LocationErrorState.GPS_DISABLED ->
                    _effect.emit(LocationFetchEffect.PromptGpsSettings)
                LocationErrorState.FETCH_FAILED, LocationErrorState.SAVE_FAILED ->
                    startFetching()
                null -> {}
            }
        }
    }

    private fun handleContinueWithoutLocation() {
        viewModelScope.launch {
            _effect.emit(LocationFetchEffect.NavigateToNextScreen)
        }
    }

    private fun showError(errorState: LocationErrorState) {
        _uiState.update {
            it.copy(
                currentStep = LocationFetchStep.ERROR,
                errorState = errorState
            )
        }
    }
}

sealed class LocationFetchIntent {
    data object StartLocationFlow : LocationFetchIntent()
    data object ShowRationale : LocationFetchIntent()
    data object PermissionDenied : LocationFetchIntent()
    data object GpsDisabled : LocationFetchIntent()
    data object ActionClicked : LocationFetchIntent()
    data object RetryClicked : LocationFetchIntent()
    data object ContinueWithoutLocation : LocationFetchIntent()
}

sealed class LocationFetchEffect {
    data object NavigateToNextScreen : LocationFetchEffect()
    data object OpenAppSettings : LocationFetchEffect()
    data object PromptGpsSettings : LocationFetchEffect()
}

enum class LocationFetchStep {
    FETCHING, COMPLETED, ERROR
}

// Configured specifically to your edge cases!
enum class LocationErrorState(val message: String, val primaryButtonText: String, val showSecondaryRetry: Boolean) {
    RATIONALE("Location access helps us find your address automatically.", "Open Settings", false),
    PERMISSION_DENIED("Location permission is disabled. Please enable it in settings.", "Open Settings", false),
    GPS_DISABLED("Your device's location services are turned off.", "Turn On Location", true),
    FETCH_FAILED("We couldn't pinpoint your location. Please check your signal.", "Retry", false),
    SAVE_FAILED("Failed to save your address to the server.", "Retry", false)
}

data class LocationFetchUiState(
    val currentStep: LocationFetchStep = LocationFetchStep.FETCHING,
    val address: String? = null,
    val isCompleted: Boolean = false,
    val errorState: LocationErrorState? = null
)
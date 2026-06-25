package org.example.project.core.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import org.example.project.core.model.auth.UserLocation
import platform.CoreLocation.*
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

actual class LocationProvider actual constructor() {
    private val locationManager = CLLocationManager()
    private var locationDelegate: CLLocationManagerDelegateProtocol? = null

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun getCurrentLocation(): UserLocation = suspendCancellableCoroutine { continuation ->
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(
                manager: CLLocationManager,
                didUpdateLocations: List<*>
            ) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation
                if (location != null) {
                    manager.stopUpdatingLocation()
                    manager.delegate = null
                    locationDelegate = null // Clear reference
                    
                    val geocoder = CLGeocoder()
                    geocoder.reverseGeocodeLocation(location) { placemarks, error ->
                        if (error != null || placemarks.isNullOrEmpty()) {
                            val userLocation = UserLocation(
                                address = "${location.coordinate.useContents { latitude }},${location.coordinate.useContents { longitude }}",
                                latitude = location.coordinate.useContents { latitude },
                                longitude = location.coordinate.useContents { longitude }
                            )
                            if (continuation.isActive) continuation.resume(userLocation)
                            return@reverseGeocodeLocation
                        }

                        val placemark = placemarks.first() as CLPlacemark
                        
                        val locality = placemark.subLocality ?: placemark.locality
                        val district = placemark.subAdministrativeArea
                        val state = placemark.administrativeArea
                        val country = placemark.country
                        
                        val normalized = normalizeLocation(state, district)
                        val newState = normalized.state
                        val newDistrict = normalized.district

                        val formattedAddress = listOfNotNull(
                            locality,
                            newDistrict,
                            newState,
                            country
                        ).filter { it.isNotBlank() }.joinToString(", ")
                        
                        val userLocation = UserLocation(
                            address = formattedAddress,
                            latitude = location.coordinate.useContents { latitude },
                            longitude = location.coordinate.useContents { longitude },
                            locality = locality ?: "",
                            district = newDistrict ?: "",
                            state = newState ?: "",
                            country = country ?: ""
                        )
                        if (continuation.isActive) continuation.resume(userLocation)
                    }
                } else {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(Exception("No location found")))
                }
            }

            override fun locationManager(
                manager: CLLocationManager,
                didFailWithError: NSError
            ) {
                manager.stopUpdatingLocation()
                manager.delegate = null
                locationDelegate = null // Clear reference
                if (continuation.isActive) continuation.resumeWith(Result.failure(Exception(didFailWithError.localizedDescription)))
            }
        }
        
        locationDelegate = delegate
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        
        continuation.invokeOnCancellation {
            locationManager.stopUpdatingLocation()
            locationManager.delegate = null
            locationDelegate = null
        }
    }
}

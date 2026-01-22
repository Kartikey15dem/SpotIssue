package org.example.project.utils

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.example.project.auth.domain.models.UserLocation
import kotlin.coroutines.resume

/**
 * Android actual implementation of [LocationProvider].
 *
 * This is intentionally simple and uses the last known location from the system services.
 * Permissions must be granted before calling [getCurrentLocation].
 * Use [LocationPermissionHandler] to request permissions before using this provider.
 */
class LocationProvider(private val context: Context) {

    private val geocoder: Geocoder? = if (Geocoder.isPresent()) {
        Geocoder(context)
    } else null

    @RequiresPermission(anyOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION])
     suspend fun getCurrentLocation(): UserLocation? = withContext(Dispatchers.IO) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }

        val loc = bestLocation ?: return@withContext null

        // Perform reverse geocoding to get address
        val address = reverseGeocode(loc.latitude, loc.longitude)

        address
    }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): UserLocation {
        if (geocoder == null) {
            Log.w("LocationProvider", "Geocoder not available")
            return UserLocation(
                address = "$latitude, $longitude",
                latitude = latitude,
                longitude = longitude
            )
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use new async API for Android 13+
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val userLocation = if (addresses.isNotEmpty()) {
                            parseAddress(addresses[0], latitude, longitude)
                        } else {
                            UserLocation(
                                address = "$latitude, $longitude",
                                latitude = latitude,
                                longitude = longitude
                            )
                        }
                        continuation.resume(userLocation)
                    }
                }
            } else {
                // Use deprecated synchronous API for older versions
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    parseAddress(addresses[0], latitude, longitude)
                } else {
                    UserLocation(
                        address = "$latitude, $longitude",
                        latitude = latitude,
                        longitude = longitude
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("LocationProvider", "Reverse geocoding failed", e)
            UserLocation(
                address = "$latitude, $longitude",
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    private fun parseAddress(address: android.location.Address, latitude: Double, longitude: Double): UserLocation {
        // Extract address components
        val flatNumber = address.subThoroughfare ?: address.featureName // House/building number
        val streetAddress = address.thoroughfare // Street name
        val city = address.locality ?: address.subAdminArea // City
        val state = address.adminArea // State
        val postalCode = address.postalCode
        val country = address.countryName

        // Build formatted address: "Flat/House, Street, City, State"
        val formattedParts = mutableListOf<String>()
        flatNumber?.let { if (it.isNotBlank()) formattedParts.add(it) }
        streetAddress?.let { if (it.isNotBlank()) formattedParts.add(it) }
        city?.let { if (it.isNotBlank()) formattedParts.add(it) }
        state?.let { if (it.isNotBlank()) formattedParts.add(it) }

        val formattedAddress = if (formattedParts.isNotEmpty()) {
            formattedParts.joinToString(", ")
        } else {
            "$latitude, $longitude"
        }

        Log.d("LocationProvider", "Reverse geocoded: $formattedAddress")

        return UserLocation(
            address = formattedAddress,
            latitude = latitude,
            longitude = longitude,
            flatNumber = flatNumber,
            streetAddress = streetAddress,
            city = city,
            state = state,
            postalCode = postalCode,
            country = country
        )
    }
}

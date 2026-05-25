package org.example.project.utils.location

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.example.project.core.model.auth.UserLocation
import kotlin.coroutines.resume

class LocationProvider(
    private val context: Context
) {

    private val geocoder: Geocoder? =
        if (Geocoder.isPresent()) {
            Geocoder(context)
        } else {
            null
        }

    @RequiresPermission(
        anyOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ]
    )
    suspend fun getCurrentLocation(): UserLocation? =
        withContext(Dispatchers.IO) {

            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE)
                        as? LocationManager
                    ?: return@withContext null

            val providers = locationManager.getProviders(true)

            var bestLocation: Location? = null

            for (provider in providers) {

                val location =
                    locationManager.getLastKnownLocation(provider)
                        ?: continue

                if (
                    bestLocation == null ||
                    location.accuracy < bestLocation.accuracy
                ) {
                    bestLocation = location
                }
            }

            val location = bestLocation
                ?: return@withContext null

            reverseGeocode(
                location.latitude,
                location.longitude
            )
        }

    private suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): UserLocation {

        if (geocoder == null) {
            return UserLocation(
                address = "$latitude,$longitude",
                latitude = latitude,
                longitude = longitude
            )
        }

        return try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                suspendCancellableCoroutine { continuation ->

                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1
                    ) { addresses ->

                        val result =
                            if (addresses.isNotEmpty()) {
                                parseAddress(
                                    addresses[0],
                                    latitude,
                                    longitude
                                )
                            } else {
                                UserLocation(
                                    address = "$latitude,$longitude",
                                    latitude = latitude,
                                    longitude = longitude
                                )
                            }

                        continuation.resume(result)
                    }
                }

            } else {

                @Suppress("DEPRECATION")
                val addresses =
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1
                    )

                if (!addresses.isNullOrEmpty()) {
                    parseAddress(
                        addresses[0],
                        latitude,
                        longitude
                    )
                } else {
                    UserLocation(
                        address = "$latitude,$longitude",
                        latitude = latitude,
                        longitude = longitude
                    )
                }
            }

        } catch (e: Exception) {

            Log.e(
                "LocationProvider",
                "Reverse geocoding failed",
                e
            )

            UserLocation(
                address = "$latitude,$longitude",
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    private fun parseAddress(
        address: Address,
        latitude: Double,
        longitude: Double
    ): UserLocation {

        val locality =
            address.subLocality
                ?: address.locality

        val district =
            address.subAdminArea

        val state =
            address.adminArea

        val country =
            address.countryName

        val formattedAddress =
            listOf(
                locality,
                district,
                state,
                country
            )
                .filter {
                    !it.isNullOrBlank()
                }
                .joinToString(", ")

        return UserLocation(
            address = formattedAddress,
            latitude = latitude,
            longitude = longitude,

            locality = locality,
            district = district,
            state = state,
            country = country
        )
    }
}
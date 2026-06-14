package org.example.project.utils.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Handles location permission requests and checks.
 * Use this before calling [org.example.project.core.utils.LocationProvider.getCurrentLocation].
 */
class LocationPermissionHandler(private val context: Context) {

    companion object {
        const val REQUEST_LOCATION_PERMISSIONS = 0xB00
    }

    /**
     * Check if location permissions are currently granted.
     */
    fun hasLocationPermission(): Boolean {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return hasFineLocation || hasCoarseLocation
    }

    /**
     * Request location permissions using the provided Activity.
     * Returns true if permissions are already granted.
     * Returns false if permission dialog was shown (wait for result callback).
     */
    fun requestLocationPermission(activity: Activity): Boolean {
        if (hasLocationPermission()) {
            Log.d("LocationPermissionHandler", "Permissions already granted")
            return true
        }

        Log.d("LocationPermissionHandler", "Requesting location permissions")
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_LOCATION_PERMISSIONS
        )
        return false
    }

    /**
     * Check if we should show rationale for requesting permissions.
     */
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
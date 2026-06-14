package org.example.project.core.utils

import org.example.project.core.model.auth.UserLocation

actual class LocationProvider actual constructor() {
    actual suspend fun getCurrentLocation(): UserLocation? {
        TODO("Not yet implemented")
    }
}
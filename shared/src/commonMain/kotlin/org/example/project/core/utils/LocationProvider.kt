package org.example.project.core.utils

import org.example.project.core.model.auth.UserLocation

expect class LocationProvider(){
    suspend fun getCurrentLocation(): UserLocation?
}

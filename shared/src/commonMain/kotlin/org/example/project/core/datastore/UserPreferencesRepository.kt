package org.example.project.core.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.example.project.core.datastore.model.UserData
import org.example.project.core.model.auth.UserLocation

interface UserPreferencesRepository {
    val userData: StateFlow<UserData>

    suspend fun updateToken(token: String)
    suspend fun setLoggedIn(isLoggedIn: Boolean)

    suspend fun updateUserLocation(userLocation: UserLocation)

    suspend fun logOut()

     fun getUserData(): Flow<UserData>

     suspend fun updateLastSync(key: String, timestamp: Long)
     suspend fun getLastSync(key: String): Long
}
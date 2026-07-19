package org.example.project.core.datastore

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.example.project.core.datastore.model.UploadDraftState
import org.example.project.core.datastore.model.UserData
import org.example.project.core.model.auth.UserLocation

class UserPreferencesRepositoryImpl(
    private val localDataSource: UserPreferencesDataSource,
) : UserPreferencesRepository {
    override val userData: StateFlow<UserData>
        get() = localDataSource.userData

    override suspend fun updateToken(token: String) {
        localDataSource.updateToken(token)
    }

    override suspend fun setLoggedIn(isLoggedIn: Boolean) {
        localDataSource.setLoggedIn(isLoggedIn)
    }

    override suspend fun updateUserLocation(userLocation: UserLocation) {
        localDataSource.updateUserLocation(userLocation)
        Logger.d { "User location updated: $userLocation" }
    }

    override suspend fun updateUploadDraftState(state: UploadDraftState) {
        localDataSource.updateUploadDraftState(state)
    }

    override suspend fun logOut() {
        localDataSource.clearUserData()
    }

    override fun getUserData(): Flow<UserData> = localDataSource.userData

    override suspend fun updateLastSync(
        key: String,
        timestamp: Long,
    ) {
        localDataSource.updateLastSync(key, timestamp)
    }

    override suspend fun getLastSync(key: String): Long = localDataSource.getLastSync(key)
}

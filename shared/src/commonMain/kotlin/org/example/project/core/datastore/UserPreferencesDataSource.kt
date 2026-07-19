package org.example.project.core.datastore

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.decodeValue
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.example.project.core.datastore.model.UploadDraftState
import org.example.project.core.datastore.model.UserData
import org.example.project.core.model.auth.UserLocation

@OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
class UserPreferencesDataSource(
    private val settings: Settings,
) {
    companion object {
        private const val USER_DATA_KEY = "user_data"
    }

    private val _userData =
        MutableStateFlow(
            settings.decodeValue(
                key = USER_DATA_KEY,
                serializer = UserData.serializer(),
                defaultValue =
                    settings.decodeValueOrNull(
                        key = USER_DATA_KEY,
                        serializer = UserData.serializer(),
                    ) ?: UserData.DEFAULT,
            ),
        )
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    suspend fun updateUserData(newUserData: UserData) {
        withContext(Dispatchers.IO) {
            settings.encodeValue(
                key = USER_DATA_KEY,
                serializer = UserData.serializer(),
                value = newUserData,
            )
            _userData.value = newUserData
        }
    }

    suspend fun updateToken(newToken: String) {
        val updatedData = userData.value.copy(token = newToken)
        updateUserData(updatedData)
    }

    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        val updatedData = userData.value.copy(isLoggedIn = isLoggedIn)
        updateUserData(updatedData)
    }

    suspend fun updateUserLocation(userLocation: UserLocation) {
        val updatedData = userData.value.copy(userLocation = userLocation)
        updateUserData(updatedData)
    }

    suspend fun updateUploadDraftState(state: UploadDraftState) {
        val updatedData = userData.value.copy(uploadDraftState = state)
        updateUserData(updatedData)
    }

    suspend fun clearUserData() {
        withContext(Dispatchers.IO) {
            val defaultData = UserData.DEFAULT
            settings.encodeValue(
                key = USER_DATA_KEY,
                serializer = UserData.serializer(),
                value = defaultData,
            )
            _userData.value = defaultData
        }
    }

    suspend fun updateLastSync(
        key: String,
        timestamp: Long,
    ) {
        withContext(Dispatchers.IO) {
            settings.putLong("last_sync_$key", timestamp)
        }
    }

    suspend fun getLastSync(key: String): Long =
        withContext(Dispatchers.IO) {
            settings.getLong("last_sync_$key", 0L)
        }
}

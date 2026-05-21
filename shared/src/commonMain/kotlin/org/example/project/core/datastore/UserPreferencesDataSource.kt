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
import org.example.project.core.datastore.model.UserData

@OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
class UserPreferencesDataSource(
    private val settings: Settings
) {
    companion object {
        private const val USER_DATA_KEY = "user_data"
    }

    private val _userData = MutableStateFlow(
        settings.decodeValue(
            key = USER_DATA_KEY,
            serializer = UserData.serializer(),
            defaultValue = settings.decodeValueOrNull(
                key = USER_DATA_KEY,
                serializer = UserData.serializer()
            ) ?: UserData.DEFAULT
        )
    )
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    suspend fun updateUserData(newUserData: UserData) {
        withContext(Dispatchers.IO) {
            settings.encodeValue(
                key = USER_DATA_KEY,
                serializer = UserData.serializer(),
                value = newUserData
            )
            _userData.value = newUserData
        }
    }

    suspend fun updateName(newName: String) {
        val updatedData = userData.value.copy(name = newName)
        updateUserData(updatedData)
    }

    suspend fun updatePhoneNumber(newPhone: String) {
        val updatedData = userData.value.copy(phoneNumber = newPhone)
        updateUserData(updatedData)
    }

    suspend fun updateAddress(newAddress: String) {
        val updatedData = userData.value.copy(address = newAddress)
        updateUserData(updatedData)
    }

    suspend fun updateToken(newToken: String) {
        val updatedData = userData.value.copy(token = newToken)
        updateUserData(updatedData)
    }

    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        val updatedData = userData.value.copy(isLoggedIn = isLoggedIn)
        updateUserData(updatedData)
    }
    suspend fun clearUserData() {
        withContext(Dispatchers.IO) {
            val defaultData = UserData.DEFAULT
            settings.encodeValue(
                key = USER_DATA_KEY,
                serializer = UserData.serializer(),
                value = defaultData
            )
            _userData.value = defaultData
        }
    }

}
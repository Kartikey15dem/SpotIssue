package org.example.project.core.datastore.model

import kotlinx.serialization.Serializable
import org.example.project.core.model.auth.UserLocation

@Serializable
data class UserData(
    val token: String = "",
    val isLoggedIn: Boolean = false,
    val userLocation : UserLocation = UserLocation(),
    val uploadDraftState: UploadDraftState = UploadDraftState.DEFAULT
) {
    companion object {
        val DEFAULT = UserData()
    }
}
package org.example.project.core.datastore.model

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val name: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val token: String = "",
    val isLoggedIn: Boolean = false
) {
    companion object {
        val DEFAULT = UserData()
    }
}
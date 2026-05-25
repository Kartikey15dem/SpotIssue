package org.example.project.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route:NavKey{
    @Serializable
    data object Auth : Route{
        @Serializable
        data object Login : Route

        @Serializable
        data object Otp : Route

        @Serializable
        data class LocationFetch(
            val name: String,
            val email: String
        ) : Route

        @Serializable
        data class NameCapture(val email : String) : Route
    }

    @Serializable
    data object Home: Route

    @Serializable
    data object CreatePost : Route

    @Serializable
    data object Profile: Route{
        @Serializable
        object ProfileDetail: Route

        @Serializable
        object EditProfileRoute: Route
    }

}


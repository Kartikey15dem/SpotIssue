package org.example.project.core.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class UserLocation(
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    val locality: String = "",
    val district: String = "",
    val state: String = "",
    val country: String = ""
)
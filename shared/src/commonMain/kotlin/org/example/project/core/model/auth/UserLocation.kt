package org.example.project.core.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class UserLocation(
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,

    val locality: String? = null,
    val district: String? = null,
    val state: String? = null,
    val country: String? = null
)
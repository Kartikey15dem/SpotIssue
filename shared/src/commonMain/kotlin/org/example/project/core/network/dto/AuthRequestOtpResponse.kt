package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequestOtpResponse(
    val sent: Boolean,
    @SerialName("dev_code")
    val devCode: String? = null
)

package org.example.project.core.model.auth

data class UserLocation(
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val flatNumber: String? = null,
    val streetAddress: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null
) {
    fun getFormattedAddress(): String {
        val parts = mutableListOf<String>()
        flatNumber?.let { if (it.isNotBlank()) parts.add(it) }
        streetAddress?.let { if (it.isNotBlank()) parts.add(it) }
        city?.let { if (it.isNotBlank()) parts.add(it) }
        state?.let { if (it.isNotBlank()) parts.add(it) }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
    }
}

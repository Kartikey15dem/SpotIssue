package org.example.project.auth.domain.models


data class UserLocation(
    val address: String, // Formatted address in one line
    val latitude: Double? = null,
    val longitude: Double? = null,
    val flatNumber: String? = null,
    val streetAddress: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null
) {
    /**
     * Get formatted address in one line: "Flat/House, Street, City, State"
     */
    fun getFormattedAddress(): String {
        val parts = mutableListOf<String>()
        flatNumber?.let { if (it.isNotBlank()) parts.add(it) }
        streetAddress?.let { if (it.isNotBlank()) parts.add(it) }
        city?.let { if (it.isNotBlank()) parts.add(it) }
        state?.let { if (it.isNotBlank()) parts.add(it) }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
    }
}


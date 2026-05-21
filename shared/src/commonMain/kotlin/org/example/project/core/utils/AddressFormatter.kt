package org.example.project.core.utils

/**
 * Formats address for display in UI
 * Handles truncation and formatting similar to Uber/Urban Company
 * Priority: House/Flat Number -> Street Address -> Landmark
 */
object AddressFormatter {
    /**
     * Formats address for single-line display with intelligent truncation
     * Shows house/flat number first, then street, truncates rest
     * Returns: "101, Main Street, Near..." (truncated to show what fits)
     */
    fun formatAddressOneLine(address: String?, maxLength: Int = 50): String {
        if (address.isNullOrBlank()) return "Add delivery address"

        val trimmed = address.trim()

        // If it fits in one line, return as is
        if (trimmed.length <= maxLength) {
            return trimmed
        }

        // Split by comma and reorder: house number first
        val parts = trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return trimmed.take(maxLength) + "..."

        // Try to fit: first part + remaining parts
        val firstPart = parts[0] // Usually house/flat number or street
        val remainingParts = if (parts.size > 1) parts.drop(1) else emptyList()

        val result = if (remainingParts.isNotEmpty()) {
            val remaining = remainingParts.joinToString(", ")
            val combined = "$firstPart, $remaining"

            if (combined.length <= maxLength) {
                combined
            } else {
                // Truncate with ellipsis
                combined.take(maxLength - 3) + "..."
            }
        } else {
            firstPart
        }

        return result
    }

    /**
     * Formats full address for detail display (e.g., in summary screen bottom bar)
     * Shows all parts in one line with truncation if needed
     */
    fun formatFullAddress(address: String?): String {
        return formatAddressOneLine(address, maxLength = 60)
    }

    /**
     * Formats address for short display (e.g., in top bar)
     * Shows first two parts only for compact view
     * Returns: "House/Flat, Street Address"
     */
    fun formatShortAddress(address: String?): String {
        if (address.isNullOrBlank()) return "Select location"

        val trimmed = address.trim()
        val parts = trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return if (parts.size > 1) {
            // Return first two parts
            parts.take(2).joinToString(", ")
        } else {
            trimmed
        }
    }

    /**
     * Gets just the main address part (first part - usually house/flat number)
     * Used for card headers or compact display
     */
    fun getAddressMainPart(address: String?): String {
        if (address.isNullOrBlank()) return "Delivery Address"

        val parts = address.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return parts.firstOrNull() ?: address.trim()
    }
}
import SwiftUI

struct IssueSpotColors {
    static let primary = Color(hex: 0xFF00C853)
    static let onPrimary = Color(hex: 0xFFFFFFFF)
    static let primaryContainer = Color(hex: 0xFFE8F5E8)
    static let onPrimaryContainer = Color(hex: 0xFF004F0E)

    static let surface = Color(hex: 0xFFFFFFFF)
    static let onSurface = Color(hex: 0xFF1C1B1F)
    static let onSurfaceVariant = Color(hex: 0xFF6B6B6B)
    static let surfaceLowest = Color(hex: 0xFFF8F8F8)
    static let surfaceVariant = Color(hex: 0xFFF5F5F5)

    static let background = Color(hex: 0xFFF8F8F8)
    static let onBackground = Color(hex: 0xFF1C1B1F)

    static let secondary = Color(hex: 0xFF625B71)
    static let onSecondary = Color(hex: 0xFFFFFFFF)
    static let secondaryContainer = Color(hex: 0xFFE8DEF8)
    static let onSecondaryContainer = Color(hex: 0xFF1E192B)

    static let tertiary = Color(hex: 0xFF7D5260)
    static let onTertiary = Color(hex: 0xFFFFFFFF)
    static let tertiaryContainer = Color(hex: 0xFFFFD8E4)
    static let onTertiaryContainer = Color(hex: 0xFF31111D)

    static let cardBackground = Color(hex: 0xFFFFFFFF)
    static let dividerColor = Color(hex: 0xFFE0E0E0)
    static let iconTint = Color(hex: 0xFF6B6B6B)
    static let activeIconTint = Color(hex: 0xFF00C853)

    static let likeColor = Color(hex: 0xFF6B6B6B)
    static let likeActiveColor = Color(hex: 0xFFE91E63)
    static let commentColor = Color(hex: 0xFF6B6B6B)
    static let shareColor = Color(hex: 0xFF6B6B6B)

    static let localityBadge = Color(hex: 0xFF00C853)
    static let districtBadge = Color(hex: 0xFF2196F3)
    static let stateBadge = Color(hex: 0xFFFF9800)
    static let nationalBadge = Color(hex: 0xFFE91E63)

    static let postButtonBackground = Color(hex: 0xFF1C1B1F)
    static let postButtonText = Color(hex: 0xFFFFFFFF)

    static let issueCountBackground = Color(hex: 0xFFF0F0F0)
    static let issueCountText = Color(hex: 0xFF6B6B6B)

    static let outline = Color(hex: 0xFFE0E0E0)

    static let error = Color(hex: 0xFFBB0000)
    static let onError = Color(hex: 0xFFFFFFFF)
    static let errorContainer = Color(hex: 0xFF5A1414)
    static let onErrorContainer = Color(hex: 0xFFFFEDEC)
}

extension Color {
    init(hex: UInt, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 08) & 0xff) / 255,
            blue: Double((hex >> 00) & 0xff) / 255,
            opacity: alpha
        )
    }
}

import SwiftUI
import Shared

extension Shared.PostLevel {
    var color: Color {
        switch self {
        case .locality: return IssueSpotColors.localityBadge
        case .district: return IssueSpotColors.districtBadge
        case .state: return IssueSpotColors.stateBadge
        case .national: return IssueSpotColors.nationalBadge
        default: return IssueSpotColors.primary
        }
    }
    
    var text: String {
        switch self {
        case .locality: return "Issues in your immediate area"
        case .district: return "Issues across your district"
        case .state: return "State-wide issues"
        case .national: return "National issues"
        default: return ""
        }
    }
    
    var displayName: String {
        switch self {
        case .locality: return "Locality"
        case .district: return "District"
        case .state: return "State"
        case .national: return "National"
        default: return "Unknown"
        }
    }
}

struct PostLevelChip: View {
    let postLevel: Shared.PostLevel
    
    var body: some View {
        Text(postLevel.displayName)
            .font(IssueSpotTypography.bodySmall)
            .fontWeight(.bold)
            .foregroundColor(postLevel.color)
            .padding(.horizontal, IssueSpotSpacing.smallMedium)
            .padding(.vertical, IssueSpotSpacing.extraSmall)
            .background(postLevel.color.opacity(0.1))
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(postLevel.color.opacity(0.15), lineWidth: 1)
            )
    }
}

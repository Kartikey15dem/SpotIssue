import SwiftUI
import Shared

struct FeedInitialStateView: View {
    let feedState: FeedState
    let onRetry: () -> Void
    var emptyMessage: String = "No posts found"

    var body: some View {
        Group {
            if feedState.isLoading {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
            } else if let error = feedState.error {
                VStack(spacing: IssueSpotSpacing.small) {
                    Text(error.message)
                        .font(IssueSpotTypography.bodyMedium)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                    Button("Retry") { onRetry() }
                        .font(IssueSpotTypography.labelMedium)
                        .foregroundColor(IssueSpotColors.onPrimary)
                        .padding(.horizontal, IssueSpotSpacing.medium)
                        .padding(.vertical, IssueSpotSpacing.small)
                        .background(IssueSpotColors.primary)
                        .cornerRadius(8)
                }
            } else {
                Text(emptyMessage)
                    .font(IssueSpotTypography.bodyMedium)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 300)
    }
}

struct FeedFooterView: View {
    let feedState: FeedState
    let onRetry: () -> Void
    var endMessage: String = "No more posts"

    var body: some View {
        Group {
            if feedState.isAppending {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
            } else if let error = feedState.appendError {
                VStack(spacing: IssueSpotSpacing.small) {
                    Text(error.message)
                        .font(IssueSpotTypography.bodyMedium)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                    Button("Retry") { onRetry() }
                        .font(IssueSpotTypography.labelMedium)
                        .foregroundColor(IssueSpotColors.onPrimary)
                        .padding(.horizontal, IssueSpotSpacing.medium)
                        .padding(.vertical, IssueSpotSpacing.small)
                        .background(IssueSpotColors.primary)
                        .cornerRadius(8)
                }
            } else if !feedState.hasMore && !feedState.posts.isEmpty {
                Text(endMessage)
                    .font(IssueSpotTypography.bodyMedium)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
            } else {
                EmptyView()
            }
        }
        .frame(maxWidth: .infinity)
        .frame(minHeight: 72)
    }
}

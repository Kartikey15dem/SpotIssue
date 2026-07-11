import SwiftUI

/// Reusable UI for the initial load states of a Paging Flow
struct PagingInitialStateView<T: AnyObject>: View {
    let presentation: PagingPresentation<T>
    let onRefresh: () -> Void
    var emptyMessage: String = "No items available"

    var body: some View {
        Group {
            if presentation.showInitialLoading {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                    .frame(maxWidth: .infinity)
                    .frame(height: 300)
            } else if presentation.showInitialError, let error = presentation.refreshError {
                VStack(spacing: IssueSpotSpacing.small) {
                    Text(error)
                        .font(IssueSpotTypography.bodyMedium)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    Button("Retry") { onRefresh() }
                        .buttonStyle(.borderedProminent)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 300)
            } else if presentation.showEmpty {
                Text(emptyMessage)
                    .font(IssueSpotTypography.bodyMedium)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    .frame(maxWidth: .infinity)
                    .frame(height: 300)
            }
        }
    }
}

/// Reusable UI for the appending footer states of a Paging Flow
struct PagingFooterView: View {
    let state: PagingFooterState
    let onRetry: () -> Void
    var endMessage: String = "No more items"

    var body: some View {
        Group {
            switch state {
            case .loading:
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
            case .error(let error):
                VStack {
                    Text(error)
                        .font(IssueSpotTypography.bodyMedium)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    Button("Retry") { onRetry() }
                        .font(IssueSpotTypography.labelMedium)
                        .foregroundColor(IssueSpotColors.onPrimary)
                        .padding(.horizontal, IssueSpotSpacing.medium)
                        .padding(.vertical, IssueSpotSpacing.small)
                        .background(IssueSpotColors.primary)
                        .cornerRadius(8)
                }
            case .endReached:
                Text(endMessage)
                    .font(IssueSpotTypography.bodyMedium)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
            case .hidden:
                EmptyView()
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 72)
    }
}

/// Reusable overlay for pull-to-refresh without invalidating ScrollView
struct PagingRefreshOverlay: View {
    let isRefreshing: Bool

    var body: some View {
        Group {
            if isRefreshing {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                    .padding(.top, IssueSpotSpacing.small)
            }
        }
    }
}

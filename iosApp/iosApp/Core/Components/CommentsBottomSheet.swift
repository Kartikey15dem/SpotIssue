import SwiftUI
import Shared

struct CommentsBottomSheet: View {
    @ObservedObject var pagingHolder: PagingItemsHolder<Comment>
    let presentation: PagingPresentation<Comment>
    let currentUserImageUrl: String?
    let onDismiss: () -> Void
    let onSubmit: (String) -> Void
    let onRefresh: () -> Void
    let onRetry: () -> Void
    var onItemAppeared: ((Int) -> Void)? = nil
    
    @State private var commentText: String = ""
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("Comments")
                    .font(IssueSpotTypography.titleMedium)
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                        .font(.title2)
                }
            }
            .padding()
            
            Divider()
            
            // Comments List
            Group {
                if !presentation.showContent {
                    PagingInitialStateView(
                        presentation: presentation,
                        onRefresh: onRefresh,
                        emptyMessage: "No comments yet. Be the first to start the discussion!"
                    )
                } else {
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            ForEach(0..<presentation.itemCount, id: \.self) { index in
                                if let comment = pagingHolder.items?.get(index: Int32(index)) {
                                    CommentItem(comment: comment)
                                        .id(comment.id)
                                        .onAppear {
                                            onItemAppeared?(index)
                                        }
                                }
                            }

                            PagingFooterView(
                                state: presentation.footer,
                                onRetry: onRetry,
                                endMessage: "No more comments"
                            )
                        }
                        .padding()
                    }
                }
            }
            
            Divider()
            
            // Input Area
            HStack(spacing: 12) {
                // Avatar
                if let urlString = currentUserImageUrl, let url = URL(string: urlString) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Image(systemName: "person.circle.fill")
                            .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    }
                    .frame(width: 32, height: 32)
                    .clipShape(Circle())
                } else {
                    Image(systemName: "person.circle.fill")
                        .resizable()
                        .frame(width: 32, height: 32)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                }
                
                TextField("Add a comment...", text: $commentText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                
                Button(action: {
                    if !commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        onSubmit(commentText)
                        commentText = ""
                    }
                }) {
                    Image(systemName: "paperplane.fill")
                        .foregroundColor(commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? IssueSpotColors.onSurfaceVariant : IssueSpotColors.primary)
                }
                .disabled(commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding()
            .padding(.bottom, 8)
        }
        .background(IssueSpotColors.surface)
    }

}

private struct CommentItem: View {
    let comment: Comment
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // Avatar
            if let urlString = comment.userImageUrl, let url = URL(string: urlString) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Image(systemName: "person.circle.fill")
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                }
                .frame(width: 36, height: 36)
                .clipShape(Circle())
            } else {
                Image(systemName: "person.circle.fill")
                    .resizable()
                    .frame(width: 36, height: 36)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .firstTextBaseline) {
                    Text(comment.userName)
                        .font(IssueSpotTypography.titleMedium)
                        .foregroundColor(IssueSpotColors.onSurface)
                    
                    Spacer()
                    
                    Text(comment.timeAgo)
                        .font(IssueSpotTypography.bodySmall)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                }
                
                Text(comment.text)
                    .font(IssueSpotTypography.bodyLarge)
                    .foregroundColor(IssueSpotColors.onSurface)
            }
        }
        .onAppear {
            print("\(PagingDebug.tag)\nComments PagingHolder CREATED")
        }
        .onDisappear {
            print("\(PagingDebug.tag)\nComments PagingHolder DESTROYED")
        }
    }
}

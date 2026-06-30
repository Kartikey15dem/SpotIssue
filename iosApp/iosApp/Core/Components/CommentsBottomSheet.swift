import SwiftUI
import Shared

struct CommentsBottomSheet: View {
    let comments: [Comment]
    let currentUserImageUrl: String?
    let onDismiss: () -> Void
    let onSubmit: (String) -> Void
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
            ScrollView {
                LazyVStack(spacing: 16) {
                    ForEach(Array(comments.enumerated()), id: \.element.id) { index, comment in
                        CommentItem(comment: comment)
                            .onAppear {
                                onItemAppeared?(index)
                            }
                    }
                }
                .padding()
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
    }
}

import SwiftUI
import Shared

struct PostDetailScreen: View {
    @StateObject private var holder: ViewModelHolder<PostDetailViewModel>
    @EnvironmentObject var router: Router
    
    init(postId: String) {
        _holder = StateObject(wrappedValue: KoinHelper().holder { $0.getPostDetailViewModel(postId: postId) })
    }

    var body: some View {
        Observing(holder.vm.uiState) { (state: PostDetailState) in
            VStack(spacing: 0) {
                // Top App Bar equivalent
                HStack {
                    Button(action: { router.goBack() }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(IssueSpotColors.onSurface)
                    }
                    .padding(.trailing, IssueSpotSpacing.small)
                    
                    Text("Post")
                        .font(IssueSpotTypography.titleLarge)
                        .foregroundColor(IssueSpotColors.onBackground)
                    
                    Spacer()
                }
                .padding()
                .background(IssueSpotColors.background)
                
                // Show loading only if post is nil (loading from DB/Network initially)
                if state.isLoading && state.post == nil {
                    Spacer()
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                    Spacer()
                } else if let error = state.error, state.post == nil {
                    Spacer()
                    Text(error)
                        .font(IssueSpotTypography.bodyLarge)
                        .foregroundColor(IssueSpotColors.error)
                    Spacer()
                } else if let post = state.post {
                    ScrollView {
                        LazyVStack(spacing: 0, pinnedViews: []) {
                            PostCard(
                                post: post,
                                isLiked: post.isLiked,
                                likesCount: Int(post.likes),
                                commentsCount: Int(post.comments),
                                isReported: post.isReported,
                                canDelete: false,
                                canReport: true,
                                isDetailMode: true,
                                onLikeClick: { },
                                onCommentIconClick: { },
                                onShareClick: { shareContent(text: "Check out this issue: \(post.postText)") },
                                onReportClick: { _ in },
                                onDeleteClick: { },
                                onPostClick: { },
                                onCollapseClick: { router.goBack() }
                            )
                            
                            Spacer().frame(height: IssueSpotSpacing.medium)
                            
                            VStack(alignment: .leading, spacing: 0) {
                                Text("Comments")
                                    .font(IssueSpotTypography.titleMedium)
                                    .foregroundColor(IssueSpotColors.onSurface)
                                    .padding(.horizontal, IssueSpotSpacing.medium)
                                    .padding(.vertical, IssueSpotSpacing.small)
                                
                                Divider().background(IssueSpotColors.outline)
                            }
                            
                            if state.comments.isEmpty {
                                if state.isLoading {
                                    ProgressView().padding()
                                } else {
                                    Text("No comments yet")
                                        .font(IssueSpotTypography.bodyMedium)
                                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                                        .padding()
                                }
                            } else {
                                ForEach(state.comments, id: \.id) { comment in
                                    VStack(spacing: 0) {
                                        CommentItemView(comment: comment)
                                        Divider().padding(.leading, 64)
                                    }
                                    .padding(.vertical, IssueSpotSpacing.small)
                                }
                            }
                            
                            Spacer().frame(height: IssueSpotSpacing.large)
                        }
                    }
                    .background(IssueSpotColors.background)
                    .refreshable {
                        holder.vm.onIntent(intent: PostDetailIntentRefresh.shared)
                    }
                }
            }
            .navigationBarHidden(true)
        }
    }
    
    private func shareContent(text: String) {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first,
              let rootVC = window.rootViewController else { return }
        
        let activityVC = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        rootVC.present(activityVC, animated: true, completion: nil)
    }
}

private struct CommentItemView: View {
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
        .padding(.horizontal, IssueSpotSpacing.medium)
    }
}

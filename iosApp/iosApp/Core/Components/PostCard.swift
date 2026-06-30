import SwiftUI
import Shared

struct PostCard: View {
    let post: Post
    let isLiked: Bool
    let likesCount: Int
    let commentsCount: Int
    let isReported: Bool
    let canDelete: Bool
    let canReport: Bool
    let isDetailMode: Bool
    
    let onLikeClick: () -> Void
    let onCommentIconClick: () -> Void
    let onShareClick: () -> Void
    let onReportClick: (String) -> Void
    let onDeleteClick: () -> Void
    let onPostClick: () -> Void
    let onCollapseClick: () -> Void
    
    @State private var showReportDialog = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: IssueSpotSpacing.small)
                
                let locationParts = [post.locality, post.district, post.state, post.country]
                    .compactMap { $0 }
                    .filter { !$0.isEmpty }
                let locationString = locationParts.isEmpty ? "Unknown Location" : locationParts.joined(separator: ", ")
                
                PostHeader(
                    userName: post.userName,
                    userImageUrl: post.userUrl,
                    timeAgo: post.timeAgo,
                    postLevel: post.postLevel,
                    location: locationString,
                    isDetailMode: isDetailMode,
                    onCollapseClick: onCollapseClick
                )
                
                Spacer().frame(height: IssueSpotSpacing.medium)
                
                Text(post.postText)
                    .font(IssueSpotTypography.bodyLarge)
                    .lineLimit(isDetailMode ? nil : 4)
                    .foregroundColor(IssueSpotColors.onSurface)
                
                Spacer().frame(height: IssueSpotSpacing.smallMedium)
                
                if let mediaUrls = post.mediaUrls, !mediaUrls.isEmpty {
                    PostMediaPreview(post: post)
                }
            }
            .contentShape(Rectangle())
            .onTapGesture {
                if !isDetailMode {
                    onPostClick()
                }
            }
            
            Spacer().frame(height: IssueSpotSpacing.smallMedium)
            
            // Interaction Row
            RowView(
                isLiked: isLiked,
                likesCount: likesCount,
                commentsCount: commentsCount,
                isReported: isReported,
                canDelete: canDelete,
                canReport: canReport,
                onLikeClick: onLikeClick,
                onCommentClick: onCommentIconClick,
                onReportClick: { showReportDialog = true },
                onShareClick: onShareClick,
                onDeleteClick: onDeleteClick
            )
        }
        .padding(IssueSpotSpacing.medium)
        .background(IssueSpotColors.cardBackground)
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 2, x: 0, y: 1)
        .alert("Report Post", isPresented: $showReportDialog) {
            Button("Spam", action: { onReportClick("Spam") })
            Button("Inappropriate", action: { onReportClick("Inappropriate") })
            Button("Cancel", role: .cancel, action: {})
        }
    }
}

private struct RowView: View {
    let isLiked: Bool
    let likesCount: Int
    let commentsCount: Int
    let isReported: Bool
    let canDelete: Bool
    let canReport: Bool
    
    let onLikeClick: () -> Void
    let onCommentClick: () -> Void
    let onReportClick: () -> Void
    let onShareClick: () -> Void
    let onDeleteClick: () -> Void
    
    var body: some View {
        HStack(alignment: .center) {
            // Like
            Button(action: onLikeClick) {
                HStack(spacing: 4) {
                    Image(systemName: isLiked ? "hand.thumbsup.fill" : "hand.thumbsup")
                        .foregroundColor(isLiked ? Color(hex: 0xFF0A66C2) : IssueSpotColors.onSurfaceVariant)
                    Text("\(likesCount)")
                        .font(IssueSpotTypography.bodyLarge)
                        .foregroundColor(IssueSpotColors.onSurface)
                }
            }
            .buttonStyle(PlainButtonStyle())
            
            Spacer().frame(width: IssueSpotSpacing.medium)
            
            // Comment
            Button(action: onCommentClick) {
                HStack(spacing: 4) {
                    Image(systemName: "bubble.right")
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    Text("\(commentsCount)")
                        .font(IssueSpotTypography.bodyLarge)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                }
            }
            .buttonStyle(PlainButtonStyle())
            
            Spacer()
            
            // Report
            if canReport {
                Button(action: onReportClick) {
                    Image(systemName: isReported ? "exclamationmark.triangle.fill" : "exclamationmark.triangle")
                        .foregroundColor(isReported ? IssueSpotColors.error : IssueSpotColors.onSurfaceVariant)
                }
                .disabled(isReported)
                .buttonStyle(PlainButtonStyle())
                
                Spacer().frame(width: IssueSpotSpacing.medium)
            }
            
            // Share
            Button(action: onShareClick) {
                Image(systemName: "square.and.arrow.up")
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
            }
            .buttonStyle(PlainButtonStyle())
            
            // Delete
            if canDelete {
                Spacer().frame(width: IssueSpotSpacing.medium)
                Button(action: onDeleteClick) {
                    Image(systemName: "trash")
                        .foregroundColor(IssueSpotColors.error)
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(.horizontal, IssueSpotSpacing.small)
        .padding(.vertical, IssueSpotSpacing.extraSmall)
    }
}

struct PostHeader: View {
    let userName: String
    let userImageUrl: String?
    let timeAgo: String?
    let postLevel: Shared.PostLevel
    let location: String
    let isDetailMode: Bool
    let onCollapseClick: () -> Void
    
    var body: some View {
        HStack(alignment: .top) {
            if isDetailMode {
                Button(action: onCollapseClick) {
                    Image(systemName: "xmark")
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                        .frame(width: 36, height: 36)
                }
                Spacer().frame(width: IssueSpotSpacing.extraSmall)
            }
            
            // Avatar
            Group {
                if let userImageUrl = userImageUrl, let url = URL(string: userImageUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable()
                             .scaledToFill()
                    } placeholder: {
                        Image(systemName: "person.circle.fill")
                            .resizable()
                            .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    }
                } else {
                    Image(systemName: "person.circle.fill")
                        .resizable()
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                }
            }
            .frame(width: 48, height: 48)
            .clipShape(Circle())
            .background(Circle().fill(IssueSpotColors.surfaceVariant))
            
            Spacer().frame(width: IssueSpotSpacing.smallMedium)
            
            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .top) {
                    Text(userName)
                        .font(IssueSpotTypography.titleMedium)
                        .fontWeight(.semibold)
                        .lineLimit(1)
                        .foregroundColor(IssueSpotColors.onSurface)
                    
                    Spacer()
                    
                    PostLevelChip(postLevel: postLevel)
                }
                
                Text(location)
                    .font(IssueSpotTypography.bodySmall)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    .lineLimit(3)
                
                if let timeAgo = timeAgo, !timeAgo.isEmpty {
                    Text(timeAgo)
                        .font(IssueSpotTypography.bodySmall)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                        .padding(.top, 1)
                }
            }
        }
    }
}

struct PostMediaPreview: View {
    let post: Post
    @EnvironmentObject var overlayController: MediaOverlayController
    
    var body: some View {
        guard let mediaUrls = post.mediaUrls, let firstUrl = mediaUrls.first, let url = URL(string: firstUrl) else {
            return AnyView(EmptyView())
        }
        
        return AnyView(
            Group {
                if post.mediaType == .image {
                    AsyncImage(url: url) { phase in
                        switch phase {
                        case .empty:
                            ProgressView()
                                .frame(maxWidth: .infinity, minHeight: 200)
                                .background(IssueSpotColors.surfaceVariant)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                        case .success(let image):
                            image.resizable()
                                .scaledToFill()
                                .frame(maxWidth: .infinity, minHeight: 200, maxHeight: 400)
                                .clipped()
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                                .onTapGesture {
                                    overlayController.show(type: .image, urls: post.mediaUrls ?? [])
                                }
                        case .failure:
                            Image(systemName: "photo")
                                .frame(maxWidth: .infinity, minHeight: 200)
                                .background(IssueSpotColors.surfaceVariant)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                        @unknown default:
                            EmptyView()
                        }
                    }
                } else if post.mediaType == .video {
                    ZStack {
                        Rectangle()
                            .fill(Color.black.opacity(0.8))
                            .frame(height: 200)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        Image(systemName: "play.circle.fill")
                            .resizable()
                            .frame(width: 48, height: 48)
                            .foregroundColor(.white)
                    }
                    .onTapGesture {
                        overlayController.show(type: .video, urls: post.mediaUrls ?? [])
                    }
                } else if post.mediaType == .pdf {
                    ZStack {
                        Rectangle()
                            .fill(IssueSpotColors.surfaceVariant)
                            .frame(height: 120)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        VStack {
                            Image(systemName: "doc.fill")
                                .resizable()
                                .scaledToFit()
                                .frame(height: 32)
                                .foregroundColor(IssueSpotColors.primary)
                            Text("View PDF")
                                .font(IssueSpotTypography.bodyMedium)
                                .foregroundColor(IssueSpotColors.onSurfaceVariant)
                        }
                    }
                    .onTapGesture {
                        overlayController.show(type: .pdf, urls: post.mediaUrls ?? [])
                    }
                }
            }
        )
    }
}

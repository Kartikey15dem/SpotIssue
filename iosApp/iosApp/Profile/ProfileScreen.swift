import SwiftUI
import Shared

struct ProfileScreen: View {
    @StateObject private var holder = KoinHelper().holder { $0.getProfileViewModel() }
    @EnvironmentObject var router: Router
    
    @State private var postToDelete: String? = nil

    var body: some View {
        Observing(holder.vm.uiState) { (state: ProfileState) in
            ZStack(alignment: .bottom) {
                IssueSpotColors.background
                    .ignoresSafeArea()
                
                if state.profile == nil && state.isProfileLoading {
                    Spacer()
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                        .frame(maxWidth: .infinity)
                    Spacer()
                } else if state.profile == nil {
                    VStack(spacing: IssueSpotSpacing.medium) {
                        Text(state.profileError ?? "Error loading profile")
                            .font(IssueSpotTypography.bodyLarge)
                            .foregroundColor(IssueSpotColors.onBackground)
                            .multilineTextAlignment(.center)
                        
                        Button(action: { holder.vm.onIntent(intent: ProfileIntentRetryProfileClicked.shared) }) {
                            Text("Retry")
                                .font(IssueSpotTypography.labelLarge)
                                .foregroundColor(IssueSpotColors.onPrimary)
                                .padding(.horizontal, IssueSpotSpacing.large)
                                .padding(.vertical, IssueSpotSpacing.medium)
                                .background(IssueSpotColors.primary)
                                .cornerRadius(8)
                        }
                    }
                    .padding()
                } else {
                    if let expandedPost = state.expandedPost {
                        let override = state.postOverrides[expandedPost.id]
                        let isLiked = override?.isLiked?.boolValue ?? expandedPost.isLiked
                        let resolvedLikes = override?.likesCount?.intValue ?? Int(expandedPost.likes)
                        let resolvedComments = override?.commentsCount?.intValue ?? Int(expandedPost.comments)
                        let isReported = override?.isReported?.boolValue ?? expandedPost.isReported
                        
                        PostCard(
                            post: expandedPost,
                            isLiked: isLiked,
                            likesCount: resolvedLikes,
                            commentsCount: resolvedComments,
                            isReported: isReported,
                            canDelete: state.isMine,
                            canReport: !state.isMine,
                            isDetailMode: true,
                            onLikeClick: {
                                holder.vm.onIntent(intent: ProfileIntentLikeClicked(postId: expandedPost.id, currentIsLiked: isLiked, currentLikesCount: Int32(resolvedLikes)))
                            },
                            onCommentIconClick: {
                                holder.vm.onIntent(intent: ProfileIntentCommentsIconClicked(postId: expandedPost.id, currentCommentsCount: Int32(resolvedComments)))
                            },
                            onShareClick: {
                                holder.vm.onIntent(intent: ProfileIntentShareClicked(post: expandedPost))
                            },
                            onReportClick: { reason in
                                holder.vm.onIntent(intent: ProfileIntentReportClicked(postId: expandedPost.id, reason: reason))
                            },
                            onDeleteClick: {
                                postToDelete = expandedPost.id
                            },
                            onPostClick: {},
                            onCollapseClick: {
                                holder.vm.onIntent(intent: ProfileIntentDismissPost.shared)
                            }
                        )
                        .transition(.move(edge: .bottom))
                        .zIndex(1)
                    } else {
                        ScrollView {
                            LazyVStack(spacing: IssueSpotSpacing.smallMedium) {
                                Spacer().frame(height: IssueSpotSpacing.small)
                                
                                if let profile = state.profile {
                                    ProfileHeader(profile: profile, onEditClick: {
                                        holder.vm.onIntent(intent: ProfileIntentEditProfileClicked.shared)
                                    })
                                    .padding(.horizontal, IssueSpotSpacing.medium)
                                    
                                    VStack(alignment: .leading, spacing: IssueSpotSpacing.extraSmall) {
                                        Text("Posts by Area")
                                            .font(IssueSpotTypography.titleLarge)
                                            .fontWeight(.semibold)
                                            .padding(.bottom, IssueSpotSpacing.extraSmall)
                                        
                                        let postLevels: [Shared.PostLevel] = [.locality, .district, .state, .national]
                                        ForEach(0..<postLevels.count, id: \.self) { i in
                                            let entry = postLevels[i]
                                            let count = Int(profile.postByArea.count > Int32(i) ? profile.postByArea[i].int32Value : 0)
                                            PostByAreaBar(postByArea: count, postLevel: entry)
                                        }
                                    }
                                    .padding(.horizontal, IssueSpotSpacing.medium)
                                    .padding(.top, IssueSpotSpacing.smallMedium)
                                }
                                
                                Button(action: { holder.vm.onIntent(intent: ProfileIntentCreatePostClicked.shared) }) {
                                    Text("+  Post New Issue")
                                        .font(IssueSpotTypography.bodyLarge)
                                        .fontWeight(.bold)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 12)
                                        .background(IssueSpotColors.primary)
                                        .foregroundColor(.white)
                                        .cornerRadius(12)
                                }
                                .padding(.horizontal, IssueSpotSpacing.medium)
                                .padding(.vertical, IssueSpotSpacing.smallMedium)
                                
                                ProfilePostTabsHeader(state: state, onIntent: { holder.vm.onIntent(intent: $0) })
                                    .padding(.horizontal, IssueSpotSpacing.medium)
                                
                                ObservePagingItems(Post.self, flow: state.activePostsFlow) { snapshot, pagingHolder in
                                    ProfilePostsListView(
                                        snapshot: snapshot,
                                        pagingHolder: pagingHolder,
                                        state: state,
                                        holder: holder,
                                        onDelete: { postToDelete = $0 }
                                    )
                                }
                                Spacer().frame(height: IssueSpotSpacing.medium)
                            }
                        }
                    }
                }
            }
            .navigationBarHidden(false)
            .navigationTitle("Profile")
            .task {
                for await effect in holder.vm.sideEffects {
                    switch effect {
                    case is ProfileSideEffectNavigateToCreatePost:
                        router.navigate(to: .createPost)
                    case let nav as ProfileSideEffectNavigateToPost:
                        router.navigate(to: .postDetail(nav.postId))
                    case is ProfileSideEffectNavigateToEditProfile:
                        router.navigate(to: .editProfile)
                    case let shareEffect as ProfileSideEffectSharePost:
                        shareContent(text: shareEffect.text)
                    case let snackbarEffect as ProfileSideEffectShowSnackbar:
                        SnackbarManager.shared.show(snackbarEffect.message)
                    default:
                        break
                    }
                }
            }
            .sheet(isPresented: Binding(
                get: { state.showCommentsSheetForPostId != nil },
                set: { if !$0 { holder.vm.onIntent(intent: ProfileIntentDismissCommentsSheet.shared) } }
            )) {
                if let postId = state.showCommentsSheetForPostId {
                    let override = state.postOverrides[postId]
                    let commentsFlow = override?.commentsFlow
                    ObservePagingItems(Comment.self, flow: commentsFlow) { commentsSnapshot, pagingHolder in
                        CommentsBottomSheet(
                            comments: commentsSnapshot.items,
                            currentUserImageUrl: state.profile?.imageUrl,
                            onDismiss: { holder.vm.onIntent(intent: ProfileIntentDismissCommentsSheet.shared) },
                            onSubmit: { text in
                                let currentCount = override?.commentsCount?.int32Value ?? 0
                                holder.vm.onIntent(intent: ProfileIntentCommentSubmitted(postId: postId, commentText: text, currentCommentCount: currentCount))
                            },
                            onItemAppeared: { index in
                                pagingHolder.loadNextPageIfNecessary(index: index)
                            }
                        )
                    }
                    .id("comments_\(postId)")
                }
            }
            .alert(isPresented: Binding(
                get: { postToDelete != nil },
                set: { if !$0 { postToDelete = nil } }
            )) {
                Alert(
                    title: Text("Delete Post"),
                    message: Text("Are you sure you want to delete this post? This action cannot be undone."),
                    primaryButton: .destructive(Text("Delete")) {
                        if let id = postToDelete {
                            holder.vm.onIntent(intent: ProfileIntentDeletePostClicked(postId: id))
                        }
                        postToDelete = nil
                    },
                    secondaryButton: .cancel(Text("Cancel")) {
                        postToDelete = nil
                    }
                )
            }
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

private struct ProfilePostsListView: View {
    let snapshot: Shared.PagingState<Shared.Post>
    let pagingHolder: PagingItemsHolder<Shared.Post>
    let state: Shared.ProfileState
    let holder: ViewModelHolder<Shared.ProfileViewModel>
    let onDelete: (String) -> Void
    
    var body: some View {
        if (snapshot.isRefreshing) && snapshot.items.isEmpty {
            ProgressView().padding(.top, IssueSpotSpacing.huge)
        } else if snapshot.isRefreshError && snapshot.items.isEmpty {
            VStack(spacing: IssueSpotSpacing.small) {
                Text(snapshot.error ?? "An error occurred")
                    .font(IssueSpotTypography.bodyMedium)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
                Button(action: { pagingHolder.refresh() }) {
                    Text("Retry")
                        .font(IssueSpotTypography.labelMedium)
                        .foregroundColor(IssueSpotColors.onPrimary)
                        .padding(.horizontal, IssueSpotSpacing.medium)
                        .padding(.vertical, IssueSpotSpacing.small)
                        .background(IssueSpotColors.primary)
                        .cornerRadius(8)
                }
            }
            .padding(.top, IssueSpotSpacing.huge)
        } else if snapshot.items.isEmpty {
            Text("No posts found")
                .font(IssueSpotTypography.bodyLarge)
                .foregroundColor(IssueSpotColors.onBackground)
                .padding(.top, IssueSpotSpacing.huge)
        } else {
            ForEach(Array(snapshot.items.enumerated()), id: \.element.id) { index, post in
                let override = state.postOverrides[post.id]
                let isLiked = override?.isLiked?.boolValue ?? post.isLiked
                let resolvedLikes = override?.likesCount?.intValue ?? Int(post.likes)
                let resolvedComments = override?.commentsCount?.intValue ?? Int(post.comments)
                let isReported = override?.isReported?.boolValue ?? post.isReported
                
                PostCard(
                    post: post,
                    isLiked: isLiked,
                    likesCount: resolvedLikes,
                    commentsCount: resolvedComments,
                    isReported: isReported,
                    canDelete: state.isMine,
                    canReport: !state.isMine,
                    isDetailMode: false,
                    onLikeClick: { holder.vm.likePost(post: post) },
                    onCommentIconClick: { holder.vm.openComments(post: post) },
                    onShareClick: { holder.vm.sharePost(post: post) },
                    onReportClick: { reason in
                        holder.vm.onIntent(intent: ProfileIntentReportClicked(postId: post.id, reason: reason))
                    },
                    onDeleteClick: { onDelete(post.id) },
                    onPostClick: {
                        holder.vm.onIntent(intent: ProfileIntentPostClicked(post: post))
                    },
                    onCollapseClick: {}
                )
                .padding(.horizontal, IssueSpotSpacing.medium)
                .onAppear {
                    pagingHolder.loadNextPageIfNecessary(index: index)
                }
            }
            
            if snapshot.isAppending {
                ProgressView()
                    .padding()
            } else if snapshot.isAppendError, let error = snapshot.error {
                VStack {
                    Text(error)
                        .font(IssueSpotTypography.bodyMedium)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    Button("Retry") { pagingHolder.retry() }
                        .font(IssueSpotTypography.labelMedium)
                        .foregroundColor(IssueSpotColors.onPrimary)
                        .padding(.horizontal, IssueSpotSpacing.medium)
                        .padding(.vertical, IssueSpotSpacing.small)
                        .background(IssueSpotColors.primary)
                        .cornerRadius(8)
                }
                .padding()
            } else if !snapshot.isAppending && snapshot.isAppendEndOfPaginationReached && !snapshot.items.isEmpty {
                Text("No more posts")
                    .font(IssueSpotTypography.bodyMedium)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    .padding()
            }
        }
    }
}

private struct ProfileHeader: View {
    let profile: Profile
    let onEditClick: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center) {
                if let urlString = profile.imageUrl, let url = URL(string: urlString) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Image(systemName: "person.circle.fill").resizable()
                    }
                    .frame(width: 60, height: 60)
                    .clipShape(Circle())
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
                } else {
                    Image(systemName: "person.circle.fill")
                        .resizable()
                        .frame(width: 60, height: 60)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                }
                
                Spacer().frame(width: IssueSpotSpacing.small)
                
                VStack(alignment: .leading, spacing: IssueSpotSpacing.extraSmall) {
                    Text(profile.name)
                        .font(IssueSpotTypography.titleLarge)
                        .fontWeight(.semibold)
                        .lineLimit(1)
                    Text(profile.location.isEmpty ? "No location set" : profile.location)
                        .font(IssueSpotTypography.bodyMedium)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                        .lineLimit(2)
                }
                
                Spacer()
                
                Button(action: onEditClick) {
                    Image(systemName: "pencil")
                        .foregroundColor(IssueSpotColors.onSurface)
                }
            }
            
            Spacer().frame(height: IssueSpotSpacing.medium)
            
            HStack(spacing: IssueSpotSpacing.smallMedium) {
                StatsCard(count: Int(profile.totalPosts), label: "Total Posts")
                StatsCard(count: Int(profile.acks), label: "Acknowledgements")
            }
        }
    }
}

private struct StatsCard: View {
    let count: Int
    let label: String
    
    var body: some View {
        VStack(spacing: 0) {
            Text("\(count)")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(IssueSpotColors.onSurface)
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(IssueSpotColors.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, IssueSpotSpacing.smallMedium)
        .background(IssueSpotColors.cardBackground)
        .cornerRadius(12)
    }
}

private struct PostByAreaBar: View {
    let postByArea: Int
    let postLevel: Shared.PostLevel
    
    var body: some View {
        HStack {
            PostLevelChip(postLevel: postLevel)
            Spacer()
            Text("\(postByArea) posts")
                .font(IssueSpotTypography.bodySmall)
        }
        .padding(IssueSpotSpacing.extraSmall)
    }
}

private struct ProfilePostTabsHeader: View {
    let state: ProfileState
    let onIntent: (ProfileIntent) -> Void
    
    var body: some View {
        VStack(spacing: IssueSpotSpacing.large) {
            SegmentedControl(
                items: ["My Posts", "Liked Posts"],
                selectedIndex: state.isMine ? 0 : 1,
                onItemSelected: { index in
                    onIntent(ProfileIntentTabChanged(isMine: index == 0))
                }
            )
            
            let sortIndex: Int = {
                switch state.sort {
                case .latest: return 0
                case .oldest: return 1
                case .popular: return 2
                default: return 0
                }
            }()
            
            SegmentedControlFilter(
                items: ["Latest", "Oldest", "Popular"],
                selectedIndex: sortIndex,
                onItemSelected: { index in
                    let sort: Shared.Sort
                    switch index {
                    case 0: sort = .latest
                    case 1: sort = .oldest
                    case 2: sort = .popular
                    default: sort = .latest
                    }
                    onIntent(ProfileIntentSortChanged(sort: sort))
                }
            )
        }
    }
    

}

private struct SegmentedControl: View {
    let items: [String]
    let selectedIndex: Int
    let onItemSelected: (Int) -> Void
    
    var body: some View {
        HStack {
            ForEach(0..<items.count, id: \.self) { index in
                let isSelected = index == selectedIndex
                Text(items[index])
                    .font(IssueSpotTypography.labelLarge)
                    .fontWeight(.medium)
                    .foregroundColor(isSelected ? IssueSpotColors.onSurface : IssueSpotColors.onSurfaceVariant)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(isSelected ? IssueSpotColors.surface : Color.clear)
                    .cornerRadius(50)
                    .onTapGesture {
                        onItemSelected(index)
                    }
            }
        }
        .frame(height: 36)
        .padding(IssueSpotSpacing.extraSmall)
        .background(IssueSpotColors.surfaceVariant)
        .cornerRadius(50)
    }
}

private struct SegmentedControlFilter: View {
    let items: [String]
    let selectedIndex: Int
    let onItemSelected: (Int) -> Void
    
    var body: some View {
        HStack(spacing: 10) {
            ForEach(0..<items.count, id: \.self) { index in
                let isSelected = index == selectedIndex
                Text(items[index])
                    .font(IssueSpotTypography.labelLarge)
                    .fontWeight(.medium)
                    .foregroundColor(isSelected ? .white : IssueSpotColors.onSurface)
                    .frame(maxWidth: .infinity)
                    .frame(height: 32)
                    .background(isSelected ? IssueSpotColors.onBackground : IssueSpotColors.surface)
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(IssueSpotColors.outline, lineWidth: 0.4)
                    )
                    .onTapGesture {
                        onItemSelected(index)
                    }
            }
        }
    }
}

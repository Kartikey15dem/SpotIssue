import SwiftUI
import Shared

struct HomeScreen: View {
    @StateObject private var holder = KoinHelper().holder { $0.getHomeViewModel() }
    @EnvironmentObject var router: Router
    
    

    var body: some View {
        Observing(holder.vm.uiState) { (state: HomeState) in
            ZStack(alignment: .bottom) {
                IssueSpotColors.background
                    .ignoresSafeArea()
                
                let flow = state.postsFlow
                
                VStack(spacing: 0) {
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
                            canDelete: false,
                            canReport: true,
                            isDetailMode: true,
                            onLikeClick: {
                                holder.vm.onIntent(intent: HomeIntentLikeClicked(postId: expandedPost.id, currentIsLiked: isLiked, currentLikesCount: Int32(resolvedLikes)))
                            },
                            onCommentIconClick: {
                                holder.vm.onIntent(intent: HomeIntentCommentsIconClicked(postId: expandedPost.id, currentCommentsCount: Int32(resolvedComments)))
                            },
                            onShareClick: {
                                holder.vm.onIntent(intent: HomeIntentShareClicked(post: expandedPost))
                            },
                            onReportClick: { reason in
                                holder.vm.onIntent(intent: HomeIntentReportClicked(postId: expandedPost.id, reason: reason))
                            },
                            onDeleteClick: {},
                            onPostClick: {},
                            onCollapseClick: {
                                holder.vm.onIntent(intent: HomeIntentDismissPost.shared)
                            }
                        )
                        .transition(.move(edge: .bottom))
                        .zIndex(1)
                    } else {
                        HomeHeader(state: state, onIntent: { intent in
                            holder.vm.onIntent(intent: intent)
                        })
                        
                        let isQueryNotBlank = !state.query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        let activeFlow = (isQueryNotBlank && state.searchPostsFlow != nil)
                            ? state.searchPostsFlow
                            : state.postsFlow
                        
                        ScrollView {
                            LazyVStack(spacing: IssueSpotSpacing.smallMedium) {
                                Spacer().frame(height: IssueSpotSpacing.small)
                                
                                let flowId = (isQueryNotBlank && state.searchPostsFlow != nil) ? "search_\(state.query)" : "posts"
                                ObservePagingItems(Post.self, flow: activeFlow) { snapshot, pagingHolder in
                                    Group {
                                        if snapshot.items.isEmpty {
                                            if snapshot.isRefreshing {
                                                ProgressView()
                                                    .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                                                    .frame(maxWidth: .infinity)
                                                    .frame(height: 300)
                                            } else if let error = snapshot.error, !snapshot.isAppendError {
                                                VStack(spacing: IssueSpotSpacing.small) {
                                                    Text(error)
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
                                                .frame(maxWidth: .infinity)
                                                .frame(height: 300)
                                            } else {
                                                Text("No posts found")
                                                    .font(IssueSpotTypography.bodyLarge)
                                                    .foregroundColor(IssueSpotColors.onBackground)
                                                    .frame(maxWidth: .infinity)
                                                    .frame(height: 300)
                                            }
                                        }
                                        
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
                                                canDelete: false,
                                                canReport: true,
                                                isDetailMode: false,
                                                onLikeClick: {
                                                    holder.vm.onIntent(intent: HomeIntentLikeClicked(postId: post.id, currentIsLiked: isLiked, currentLikesCount: Int32(resolvedLikes)))
                                                },
                                                onCommentIconClick: {
                                                    holder.vm.onIntent(intent: HomeIntentCommentsIconClicked(postId: post.id, currentCommentsCount: Int32(resolvedComments)))
                                                },
                                                onShareClick: {
                                                    holder.vm.onIntent(intent: HomeIntentShareClicked(post: post))
                                                },
                                                onReportClick: { reason in
                                                    holder.vm.onIntent(intent: HomeIntentReportClicked(postId: post.id, reason: reason))
                                                },
                                                onDeleteClick: {},
                                                onPostClick: {
                                                    holder.vm.onIntent(intent: HomeIntentPostClicked(post: post))
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
                                        } else {
                                            Spacer().frame(height: IssueSpotSpacing.medium)
                                        }
                                        
                                        Color.clear
                                            .frame(width: 0, height: 0)
                                            .onChange(of: snapshot.error) { newError in
                                                if let error = newError, !snapshot.isAppendError, !snapshot.items.isEmpty {
                                                    holder.vm.onIntent(intent: HomeIntentShowRefreshErrorSnackbar(message: error))
                                                }
                                            }
                                    }
                                    .refreshable {
                                        holder.vm.onIntent(intent: HomeIntentPullRefreshStarted.shared)
                                        pagingHolder.refresh()
                                    }
                                }
                                .id(flowId)
                            }
                        }
                    }
                } // End VStack
                
                // Bottom Navigation Bar
                if state.expandedPost == nil {
                    VStack {
                        Spacer()
                        HomeBottomNavigationBar(
                            currentLevel: state.postLevel,
                            onLevelChange: { level in
                                holder.vm.onIntent(intent: HomeIntentChangeLevel(level: level))
                            }
                        )
                    }
                }
            }
            .navigationBarHidden(true)
            .task {
                for await effect in holder.vm.sideEffects {
                    switch effect {
                    case is HomeSideEffectNavigateToCreatePost:
                        router.navigate(to: .createPost)
                    case is HomeSideEffectNavigateToProfile:
                        router.navigate(to: .profile)
                    case let errorEffect as HomeSideEffectShowError:
                        print("Error: \(errorEffect.message)")
                    case let snackbarEffect as HomeSideEffectShowSnackbar:
                        SnackbarManager.shared.show(snackbarEffect.message)
                    case let shareEffect as HomeSideEffectSharePost:
                        shareContent(text: shareEffect.text)
                    default:
                        break
                    }
                }
            }
            .sheet(isPresented: Binding(
                get: { state.showCommentsSheetForPostId != nil },
                set: { if !$0 { holder.vm.onIntent(intent: HomeIntentDismissCommentsSheet.shared) } }
            )) {
                if let postId = state.showCommentsSheetForPostId {
                    let override = state.postOverrides[postId]
                    let commentsFlow = override?.commentsFlow
                    ObservePagingItems(Comment.self, flow: commentsFlow) { commentsSnapshot, pagingHolder in
                        CommentsBottomSheet(
                            comments: commentsSnapshot.items,
                            currentUserImageUrl: state.currentUserImage,
                            onDismiss: { holder.vm.onIntent(intent: HomeIntentDismissCommentsSheet.shared) },
                            onSubmit: { text in
                                let currentCount = override?.commentsCount?.int32Value ?? 0
                                holder.vm.onIntent(intent: HomeIntentCommentSubmitted(postId: postId, commentText: text, currentCommentCount: currentCount))
                            },
                            onItemAppeared: { index in
                                pagingHolder.loadNextPageIfNecessary(index: index)
                            }
                        )
                    }
                    .id("comments_\(postId)")
                }
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

struct HomeHeader: View {
    let state: HomeState
    let onIntent: (HomeIntent) -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center) {
                // Search Field
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    TextField("Search issues, users, location", text: Binding(
                        get: { state.query },
                        set: { onIntent(HomeIntentSearchQueryChanged(query: $0)) }
                    ))
                    .font(IssueSpotTypography.bodyLarge)
                    .foregroundColor(IssueSpotColors.onSurface)
                }
                .padding(.horizontal, IssueSpotSpacing.smallMedium)
                .frame(height: 48)
                .background(IssueSpotColors.surfaceVariant)
                .cornerRadius(12)
                
                Spacer().frame(width: IssueSpotSpacing.small)
                
                // Post Button
                Button(action: { onIntent(HomeIntentCreatePostClicked.shared) }) {
                    HStack(spacing: IssueSpotSpacing.small) {
                        Image(systemName: "plus")
                            .font(.system(size: 14, weight: .bold))
                        Text("Post")
                            .font(IssueSpotTypography.bodyLarge)
                    }
                    .padding(.horizontal, IssueSpotSpacing.smallMedium)
                    .padding(.vertical, IssueSpotSpacing.small)
                    .foregroundColor(IssueSpotColors.postButtonText)
                    .background(IssueSpotColors.postButtonBackground)
                    .cornerRadius(12)
                }
                
                Spacer().frame(width: IssueSpotSpacing.small)
                
                // Profile Button
                Button(action: { onIntent(HomeIntentProfileClicked.shared) }) {
                    Image(systemName: "person.circle")
                        .resizable()
                        .frame(width: 28, height: 28)
                        .foregroundColor(IssueSpotColors.onSurface)
                }
            }
            .padding(.horizontal, IssueSpotSpacing.smallMedium)
            .padding(.vertical, IssueSpotSpacing.small)
            
            if state.query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: IssueSpotSpacing.smallMedium)
                    
                    HStack {
                        PostLevelChip(postLevel: state.postLevel)
                        Spacer().frame(width: IssueSpotSpacing.small)
                        Text("\(state.activeIssues) active issues")
                            .font(IssueSpotTypography.bodyLarge)
                            .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    }
                    
                    Spacer().frame(height: IssueSpotSpacing.smallMedium)
                    
                    Text("\(state.postLevel.displayName) Issues")
                        .font(IssueSpotTypography.bodyLarge)
                        .fontWeight(.bold)
                        .foregroundColor(IssueSpotColors.onSurface)
                    
                    Spacer().frame(height: IssueSpotSpacing.extraSmall)
                    
                    Text(state.postLevel.text)
                        .font(IssueSpotTypography.bodyLarge)
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, IssueSpotSpacing.smallMedium)
                .padding(.bottom, IssueSpotSpacing.smallMedium)
            }
        }
        .background(IssueSpotColors.surface)
    }
}

struct HomeBottomNavigationBar: View {
    let currentLevel: Shared.PostLevel
    let onLevelChange: (Shared.PostLevel) -> Void
    
    struct BottomNavItem {
        let level: Shared.PostLevel
        let iconName: String
    }
    
    let items = [
        BottomNavItem(level: .locality, iconName: "location.fill"),
        BottomNavItem(level: .district, iconName: "building.2.fill"),
        BottomNavItem(level: .state, iconName: "map.fill"),
        BottomNavItem(level: .national, iconName: "globe.americas.fill")
    ]
    
    var body: some View {
        HStack {
            ForEach(items, id: \.level.name) { item in
                let isSelected = currentLevel == item.level
                Spacer()
                Button(action: { onLevelChange(item.level) }) {
                    VStack(spacing: 4) {
                        Image(systemName: item.iconName)
                            .font(.system(size: 20))
                            .foregroundColor(isSelected ? IssueSpotColors.primary : IssueSpotColors.onSurfaceVariant)
                        Text(item.level.displayName)
                            .font(IssueSpotTypography.labelSmall)
                            .foregroundColor(isSelected ? IssueSpotColors.primary : IssueSpotColors.onSurfaceVariant)
                    }
                }
                Spacer()
            }
        }
        .padding(.vertical, 8)
        .padding(.bottom, UIApplication.shared.windows.first?.safeAreaInsets.bottom ?? 0)
        .background(IssueSpotColors.surface)
        .shadow(color: Color.black.opacity(0.1), radius: 5, x: 0, y: -2)
    }
}

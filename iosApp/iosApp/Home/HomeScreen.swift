import SwiftUI
import Shared

struct HomeScreen: View {
    @StateObject private var holder: ViewModelHolder<HomeViewModel>
    @EnvironmentObject var router: Router

    @State private var localityScrollPosition = ScrollPosition(idType: String.self)
    @State private var districtScrollPosition = ScrollPosition(idType: String.self)
    @State private var stateScrollPosition = ScrollPosition(idType: String.self)
    @State private var nationalScrollPosition = ScrollPosition(idType: String.self)

    init() {
        let holder = KoinHelper().holder { $0.getHomeViewModel() }
        _holder = StateObject(wrappedValue: holder)
    }

    var body: some View {
        let _ = print("""
        [HOME_RECOMPOSE] HomeScreen BODY
        [HOME_RECOMPOSE] time=\(Date())
        [HOME_RECOMPOSE] ========================
        """)
        let _ = print("\(PagingDebug.tag)\nHomeScreen BODY")
        Observing(holder.vm.uiState) { (state: HomeState) in
            ZStack(alignment: .top) {
                IssueSpotColors.background
                    .ignoresSafeArea()
                
                Observing(holder.vm.expandedPost) { expandedPost in
                    let _ = print("""
                    [HOME_RECOMPOSE] ExpandedPost OBSERVED
                    [HOME_RECOMPOSE] postId=\(expandedPost?.id ?? "nil")
                    [HOME_RECOMPOSE] time=\(Date())
                    [HOME_RECOMPOSE] ========================
                    """)
                    let _ = print("\(PagingDebug.tag)\nExpandedPost Changed\npostId: \(expandedPost?.id ?? "nil")")
                    VStack(spacing: 0) {
                        if let expandedPost {
                            let isLiked = expandedPost.isLiked
                            let resolvedLikes = Int(expandedPost.likes)
                            let resolvedComments = Int(expandedPost.comments)
                            let isReported = expandedPost.isReported

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
                                    holder.vm.onIntent(intent: HomeIntentLikeClicked(postId: expandedPost.id))
                                },
                                onCommentIconClick: {
                                    holder.vm.onIntent(intent: HomeIntentCommentsIconClicked(postId: expandedPost.id))
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
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                            .transition(.move(edge: .bottom))
                            .zIndex(1)
                        } else {
                            Observing(holder.vm.currentLevel) { currentLevel in
                                let _ = print("""
                                [HOME_RECOMPOSE] CurrentLevel OBSERVED
                                [HOME_RECOMPOSE] \(currentLevel)
                                [HOME_RECOMPOSE] time=\(Date())
                                [HOME_RECOMPOSE] ========================
                                """)
                                let _ = print("\(PagingDebug.tag)\nCurrentLevel Changed\nlevel: \(currentLevel)")
                                Observing(holder.vm.activeIssues) { activeIssues in
                                    let _ = print("""
                                    [HOME_RECOMPOSE] ActiveIssues OBSERVED
                                    [HOME_RECOMPOSE] count=\(activeIssues)
                                    [HOME_RECOMPOSE] time=\(Date())
                                    [HOME_RECOMPOSE] ========================
                                    """)
                                    if state.query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                        Observing(holder.vm.feedState) { (feedState: FeedState) in
                                            HomeFeedContainer(
                                                state: state,
                                                currentLevel: currentLevel,
                                                activeIssues: activeIssues,
                                                feedState: feedState,
                                                scrollPosition: scrollPosition(for: currentLevel),
                                                onIntent: holder.vm.onIntent,
                                                isSearch: false
                                            )
                                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                                        }
                                    } else {
                                        Observing(holder.vm.searchState) { (searchState: FeedState) in
                                            HomeFeedContainer(
                                                state: state,
                                                currentLevel: currentLevel,
                                                activeIssues: activeIssues,
                                                feedState: searchState,
                                                scrollPosition: scrollPosition(for: currentLevel),
                                                onIntent: holder.vm.onIntent,
                                                isSearch: true
                                            )
                                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)

                    // Bottom Navigation Bar
                    if expandedPost == nil {
                        Observing(holder.vm.currentLevel) { currentLevel in
                            let _ = print("""
                            [HOME_RECOMPOSE] CurrentLevel OBSERVED
                            [HOME_RECOMPOSE] \(currentLevel)
                            [HOME_RECOMPOSE] time=\(Date())
                            [HOME_RECOMPOSE] ========================
                            """)
                            VStack {
                                Spacer()
                                HomeBottomNavigationBar(
                                    currentLevel: currentLevel,
                                    onLevelChange: { level in
                                        holder.vm.onIntent(intent: HomeIntentChangeLevel(level: level))
                                    }
                                )
                            }
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                        }
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
                        SnackbarManager.shared.show(errorEffect.message)
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
                    Observing(holder.vm.activeCommentsFlow) { activeCommentsFlow in
                        let _ = print("\(PagingDebug.tag)\nComments Flow Changed\npostId: \(postId)")
                        if let activeCommentsFlow {
                            ObserveErasedPagingItems(
                                Comment.self,
                                flow: activeCommentsFlow,
                                sourceKey: "home-comments:\(postId)"
                            ) { commentsSnapshot, pagingHolder in
                                let commentsPresentation = PagingPresentation(
                                    snapshot: commentsSnapshot,
                                    endRule: .comments
                                )

                                CommentsBottomSheet(
                                    pagingHolder: pagingHolder,
                                    presentation: commentsPresentation,
                                    currentUserImageUrl: state.currentUserImage,
                                    onDismiss: { holder.vm.onIntent(intent: HomeIntentDismissCommentsSheet.shared) },
                                    onSubmit: { text in
                                        holder.vm.onIntent(intent: HomeIntentCommentSubmitted(postId: postId, commentText: text))
                                    },
                                    onRefresh: pagingHolder.refresh,
                                    onRetry: pagingHolder.retry,
                                    onItemAppeared: { index in
                                        pagingHolder.loadNextPageIfNecessary(index: index)
                                    }
                                )
                            }
                            .id("comments_\(postId)")
                        } else {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                        }
                    }
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

    private func scrollPosition(for level: Shared.PostLevel) -> Binding<ScrollPosition> {
        switch level {
        case .locality:
            return $localityScrollPosition
        case .district:
            return $districtScrollPosition
        case .state:
            return $stateScrollPosition
        case .national:
            return $nationalScrollPosition
        }
    }
}

private struct HomeFeedContainer: View {
    let state: HomeState
    let currentLevel: Shared.PostLevel
    let activeIssues: KotlinInt
    let feedState: FeedState
    let scrollPosition: Binding<ScrollPosition>
    let onIntent: (HomeIntent) -> Void
    let isSearch: Bool

    var body: some View {
        let _ = print("""
        [HOME_RECOMPOSE] HomeFeedContainer BODY
        [HOME_RECOMPOSE] time=\(Date())
        [HOME_RECOMPOSE] ========================
        """)
        VStack(spacing: 0) {
            HomeHeader(
                state: state,
                currentLevel: currentLevel,
                activeIssues: activeIssues,
                onIntent: onIntent
            )

            ScrollView {
                LazyVStack(spacing: IssueSpotSpacing.smallMedium) {
                    Spacer().frame(height: IssueSpotSpacing.small)
                    HomePostsList(
                        feedState: feedState,
                        onIntent: onIntent,
                        isSearch: isSearch
                    )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .scrollPosition(scrollPosition)
            .refreshable {
                onIntent(isSearch ? HomeIntentRefreshSearchPosts.shared : HomeIntentRefreshCurrentPosts.shared)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}

private struct HomePostsList: View {
    let feedState: FeedState
    let onIntent: (HomeIntent) -> Void
    let isSearch: Bool

    var body: some View {
        Group {
            if feedState.posts.isEmpty {
                FeedInitialStateView(
                    feedState: feedState,
                    onRetry: { onIntent(isSearch ? HomeIntentRetrySearchPosts.shared : HomeIntentRetryPosts.shared) },
                    emptyMessage: "No posts available"
                )
            }

            if !feedState.posts.isEmpty {
                ForEach(Array(feedState.posts.enumerated()), id: \.element.id) { index, post in
                    let isFirstOrLast = index == 0 || index >= feedState.posts.count - 5
                    PostCard(
                        post: post,
                        isLiked: post.isLiked,
                        likesCount: Int(post.likes),
                        commentsCount: Int(post.comments),
                        isReported: post.isReported,
                        canDelete: false,
                        canReport: true,
                        isDetailMode: false,
                        onLikeClick: {
                            onIntent(HomeIntentLikeClicked(postId: post.id))
                        },
                        onCommentIconClick: {
                            onIntent(HomeIntentCommentsIconClicked(postId: post.id))
                        },
                        onShareClick: {
                            onIntent(HomeIntentShareClicked(post: post))
                        },
                        onReportClick: { reason in
                            onIntent(HomeIntentReportClicked(postId: post.id, reason: reason))
                        },
                        onDeleteClick: {},
                        onPostClick: {
                            onIntent(HomeIntentPostClicked(postId: post.id))
                        },
                        onCollapseClick: {},
                        isEdgeItem: isFirstOrLast
                    )
                    .padding(.horizontal, IssueSpotSpacing.medium)
                    .id(post.id)
                    .onAppear {
                        if index >= feedState.posts.count - 5 {
                            onIntent(isSearch ? HomeIntentLoadMoreSearchPosts.shared : HomeIntentLoadMorePosts.shared)
                        }
                    }
                }

                FeedFooterView(
                    feedState: feedState,
                    onRetry: { onIntent(isSearch ? HomeIntentRetrySearchPosts.shared : HomeIntentRetryPosts.shared) },
                    endMessage: "No more posts"
                )
            }
        }
        .overlay(alignment: .top) {
            if feedState.isRefreshing && !feedState.posts.isEmpty {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                    .padding(.top, IssueSpotSpacing.small)
            }
        }
        .onChange(of: feedState.error?.message) { _, error in
            if let error, !feedState.posts.isEmpty {
                onIntent(HomeIntentShowRefreshErrorSnackbar(message: error))
            }
        }
    }
}

struct HomeHeader: View {
    let state: HomeState
    let currentLevel: Shared.PostLevel
    let activeIssues: KotlinInt
    let onIntent: (HomeIntent) -> Void
    
    var body: some View {
        let _ = print("""
        [HOME_RECOMPOSE] HomeHeader BODY
        [HOME_RECOMPOSE] query=\(state.query)
        [HOME_RECOMPOSE] level=\(currentLevel)
        [HOME_RECOMPOSE] time=\(Date())
        [HOME_RECOMPOSE] ========================
        """)
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
                        PostLevelChip(postLevel: currentLevel)
                        Spacer().frame(width: IssueSpotSpacing.small)
                        Text("\(activeIssues.intValue) active issues")
                            .font(IssueSpotTypography.bodyLarge)
                            .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    }
                    
                    Spacer().frame(height: IssueSpotSpacing.smallMedium)
                    
                    Text("\(currentLevel.displayName) Issues")
                        .font(IssueSpotTypography.bodyLarge)
                        .fontWeight(.bold)
                        .foregroundColor(IssueSpotColors.onSurface)
                    
                    Spacer().frame(height: IssueSpotSpacing.extraSmall)
                    
                    Text(currentLevel.text)
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
        let _ = print("""
        [HOME_RECOMPOSE] BottomNavigation BODY
        [HOME_RECOMPOSE] level=\(currentLevel)
        [HOME_RECOMPOSE] time=\(Date())
        [HOME_RECOMPOSE] ========================
        """)
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

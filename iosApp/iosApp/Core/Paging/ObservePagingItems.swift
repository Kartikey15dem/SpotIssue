import SwiftUI
import Shared

enum PagingDebug {
    static let tag = "[SWIFT_PAGING]"
}

enum PagingFooterState: Equatable {
    case hidden
    case loading
    case error(String)
    case endReached
}

enum PagingEndRule {
    case home
    case profile
    case comments
}

/// SwiftUI's equivalent of the derived Paging3 state used by the Compose screens.
struct PagingPresentation<T: AnyObject> {
    let itemCount: Int
    let showInitialLoading: Bool
    let showInitialError: Bool
    let showEmpty: Bool
    let showContent: Bool
    let isPullRefreshing: Bool
    let refreshError: String?
    let footer: PagingFooterState

    init(snapshot: PagingState<T>, endRule: PagingEndRule) {
        itemCount = Int(snapshot.itemCount)

        let refreshError = snapshot.isRefreshError
            ? snapshot.refreshError ?? "An error occurred"
            : nil
        let appendError = snapshot.isAppendError
            ? snapshot.appendError ?? "An error occurred"
            : nil
        let hasItems = snapshot.itemCount > 0
        let isAwaitingFirstLoad = !hasItems && snapshot.loadStates == nil

        self.refreshError = refreshError
        showInitialLoading = !hasItems && (snapshot.isRefreshing || isAwaitingFirstLoad)
        showInitialError = !hasItems && refreshError != nil && !snapshot.isRefreshing
        showEmpty = !hasItems
            && !snapshot.isRefreshing
            && !isAwaitingFirstLoad
            && refreshError == nil
        showContent = hasItems
        isPullRefreshing = hasItems && snapshot.isRefreshing

        guard hasItems else {
            footer = .hidden
            return
        }

        switch endRule {
        case .home where snapshot.isRefreshing:
            footer = .hidden
            return
        case .comments where snapshot.isRefreshing:
            footer = .loading
            return
        default:
            break
        }

        if snapshot.isAppending {
            footer = .loading
        } else if let appendError {
            footer = .error(appendError)
        } else if Self.hasReachedEnd(snapshot: snapshot, rule: endRule) {
            footer = .endReached
        } else {
            footer = .hidden
        }
        print("\(PagingDebug.tag)\nPagingPresentation\nitemCount=\(itemCount)\nshowContent=\(showContent)\nshowInitialLoading=\(showInitialLoading)\nrefreshing=\(snapshot.isRefreshing)\nappending=\(snapshot.isAppending)\nfooterState=\(footer)\ntime=\(Date())")
    }

    private static func hasReachedEnd(
        snapshot: PagingState<T>,
        rule: PagingEndRule
    ) -> Bool {
        guard let loadStates = snapshot.loadStates else {
            return snapshot.isAppendEndOfPaginationReached
        }

        switch rule {
        case .home:
            return loadStates.source.append.endOfPaginationReached
                || loadStates.mediator?.append.endOfPaginationReached == true
        case .profile:
            return !snapshot.isAppending
                && !snapshot.isAppendError
                && loadStates.mediator?.append.endOfPaginationReached == true
        case .comments:
            return loadStates.source.append.endOfPaginationReached
                || loadStates.mediator?.append.endOfPaginationReached == true
        }
    }
}

/// SwiftUI equivalent of collecting a Flow<PagingData<T>> as LazyPagingItems.
struct ObservePagingItems<T: AnyObject, Content: View>: View {
    @StateObject private var pagingHolder: PagingItemsHolder<T>

    private let flow: SkieSwiftFlow<Paging_commonPagingData<T>>?
    private let sourceKey: String
    private let content: (PagingState<T>, PagingItemsHolder<T>) -> Content

    init(
        _ type: T.Type = T.self,
        flow: SkieSwiftFlow<Paging_commonPagingData<T>>?,
        sourceKey: String = "default",
        @ViewBuilder content: @escaping (PagingState<T>, PagingItemsHolder<T>) -> Content
    ) {
        self.flow = flow
        self.sourceKey = sourceKey
        self.content = content
        self._pagingHolder = StateObject(
            wrappedValue: PagingItemsHolder(flow: flow, sourceKey: sourceKey)
        )
    }

    var body: some View {
        Group {
            if let items = pagingHolder.items {
                Observing(items.state) { (state: PagingState<T>) in
                    content(state, pagingHolder)
                }
            } else {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                    .frame(maxWidth: .infinity)
                    .padding(.top, IssueSpotSpacing.huge)
            }
        }
        .task(id: sourceKey) {
            pagingHolder.bind(flow: flow, sourceKey: sourceKey)
        }
    }
}

/// Observes a presenter owned by its screen so paging survives temporary UI branches.
struct ObservePagingItemsHolder<T: AnyObject, Content: View>: View {
    @ObservedObject var pagingHolder: PagingItemsHolder<T>
    private let content: (PagingState<T>, PagingItemsHolder<T>) -> Content

    init(
        pagingHolder: PagingItemsHolder<T>,
        @ViewBuilder content: @escaping (PagingState<T>, PagingItemsHolder<T>) -> Content
    ) {
        self.pagingHolder = pagingHolder
        self.content = content
    }

    var body: some View {
        Group {
            if let items = pagingHolder.items {
                Observing(items.state) { (state: PagingState<T>) in
                    let _ = print("\(PagingDebug.tag)\nObservePagingItemsHolder BODY\nitemCount=\(state.itemCount)\nrefreshing=\(state.isRefreshing)\nappending=\(state.isAppending)\ntime=\(Date())")
                    content(state, pagingHolder)
                }
            } else {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                    .frame(maxWidth: .infinity)
                    .padding(.top, IssueSpotSpacing.huge)
            }
        }
    }
}

/// Bridges flows whose generic type is erased by Kotlin's nested StateFlow export.
struct ObserveErasedPagingItems<T: AnyObject, Content: View>: View {
    @StateObject private var pagingHolder: PagingItemsHolder<T>

    private let flow: (any Kotlinx_coroutines_coreFlow)?
    private let sourceKey: String
    private let content: (PagingState<T>, PagingItemsHolder<T>) -> Content

    init(
        _ type: T.Type = T.self,
        flow: (any Kotlinx_coroutines_coreFlow)?,
        sourceKey: String = "default",
        @ViewBuilder content: @escaping (PagingState<T>, PagingItemsHolder<T>) -> Content
    ) {
        self.flow = flow
        self.sourceKey = sourceKey
        self.content = content
        self._pagingHolder = StateObject(
            wrappedValue: PagingItemsHolder(erasedFlow: flow, sourceKey: sourceKey)
        )
    }

    var body: some View {
        Group {
            if let items = pagingHolder.items {
                Observing(items.state) { (state: PagingState<T>) in
                    content(state, pagingHolder)
                }
            } else {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                    .frame(maxWidth: .infinity)
                    .padding(.top, IssueSpotSpacing.huge)
            }
        }
        .task(id: sourceKey) {
            pagingHolder.bind(erasedFlow: flow, sourceKey: sourceKey)
        }
    }
}

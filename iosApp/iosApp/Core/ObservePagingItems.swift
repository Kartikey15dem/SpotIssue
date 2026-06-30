import SwiftUI
import Shared

/**
 * A reusable SwiftUI View that takes a `Flow<PagingData<T>>` from your KMP ViewModel
 * and provides a `PagingState<T>` to your content closure.
 *
 * This is the SwiftUI equivalent of Compose's `collectAsLazyPagingItems()`.
 * It handles:
 * - Creating and managing a `PagingItems` (with its internal CoroutineScope)
 * - Keeping the bridge stable across re-renders via `@StateObject`
 * - Re-creating the bridge when the flow identity changes (tab switch, sort change, etc.)
 * - Observing the bridge's StateFlow via SKIE's `Observing`
 */
struct ObservePagingItems<T: AnyObject, Content: View>: View {
    
    @StateObject private var pagingHolder: PagingItemsHolder<T>
    private let content: (PagingState<T>, PagingItemsHolder<T>) -> Content
    
    init(
        _ type: T.Type = T.self,
        flow: SkieSwiftFlow<Paging_commonPagingData<T>>?,
        @ViewBuilder content: @escaping (PagingState<T>, PagingItemsHolder<T>) -> Content
    ) {
        self._pagingHolder = StateObject(wrappedValue: PagingItemsHolder(flow: flow))
        self.content = content
    }
    
    var body: some View {
        Group {
            if let items = pagingHolder.items {
                Observing(items.state) { (state: PagingState<T>) in
                    content(state, pagingHolder)
                }
            } else {
                // Bridge not bound yet — show loading indicator
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                    .frame(maxWidth: .infinity)
                    .padding(.top, IssueSpotSpacing.huge)
            }
        }
    }
}

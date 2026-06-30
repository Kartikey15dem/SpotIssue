import SwiftUI
import Shared

/**
 * A stable container that keeps a `PagingItems` object alive across SwiftUI re-renders.
 * Use with `@StateObject` to ensure the bridge (and its internal CoroutineScope)
 * survives SwiftUI's view struct re-creation.
 *
 * This is the iOS equivalent of Compose's `collectAsLazyPagingItems()` remember behavior.
 */
class PagingItemsHolder<T: AnyObject>: ObservableObject {
    
    private(set) var items: PagingItems<T>?
    
    init(flow: SkieSwiftFlow<Paging_commonPagingData<T>>? = nil) {
        if let flow = flow {
            self.items = PagingItems(pagingFlow: flow)
        }
    }
    
    /// Trigger a refresh on the current paging source (e.g. for pull-to-refresh).
    func refresh() {
        items?.refresh()
    }
    
    /// Retry a failed page load.
    func retry() {
        items?.retry()
    }
    
    /// Let the bridge know an item was displayed so it can load the next page if necessary.
    func loadNextPageIfNecessary(index: Int) {
        let _ = items?.get(index: Int32(index))
    }
    
    deinit {
        // Ensure resources are released when this holder goes away
        items?.close()
    }
}

import SwiftUI
import Shared

/// Keeps one `PagingItems` presenter alive, matching Compose's remembered
/// `collectAsLazyPagingItems()` ownership and viewport-hint behavior.
final class PagingItemsHolder<T: AnyObject>: ObservableObject {
    @Published private(set) var items: PagingItems<T>?

    private var sourceKey: String?

    init(
        flow: SkieSwiftFlow<Paging_commonPagingData<T>>? = nil,
        sourceKey: String = "default"
    ) {
        print("\(PagingDebug.tag)\nPagingItemsHolder CREATED\nhash: \(ObjectIdentifier(self).hashValue)\nsourceKey: \(sourceKey)")
        bind(flow: flow, sourceKey: sourceKey)
    }

    init(
        erasedFlow: (any Kotlinx_coroutines_coreFlow)?,
        sourceKey: String = "default"
    ) {
        print("\(PagingDebug.tag)\nPagingItemsHolder CREATED\nhash: \(ObjectIdentifier(self).hashValue)\nsourceKey: \(sourceKey)")
        bind(erasedFlow: erasedFlow, sourceKey: sourceKey)
    }

    func bind(
        flow: SkieSwiftFlow<Paging_commonPagingData<T>>?,
        sourceKey: String
    ) {
        print("""
        TEST
        PagingItemsHolder.bind()
        sourceKey=\(sourceKey)
        existingSourceKey=\(self.sourceKey ?? "nil")
        itemsAlreadyExists=\(items != nil)
        """)
        
        guard self.sourceKey != sourceKey || items == nil else { return }

        print("\(PagingDebug.tag)\nbind()\nold source: \(self.sourceKey ?? "nil")\nnew source: \(sourceKey)")
        items?.close()
        self.sourceKey = sourceKey
        
        print("TEST\nCreating PagingItems")
        items = flow.map { PagingItems(pagingFlow: $0) }
    }

    func bind(
        erasedFlow: (any Kotlinx_coroutines_coreFlow)?,
        sourceKey: String
    ) {
        print("""
        TEST
        PagingItemsHolder.bind()
        sourceKey=\(sourceKey)
        existingSourceKey=\(self.sourceKey ?? "nil")
        itemsAlreadyExists=\(items != nil)
        """)
        
        guard self.sourceKey != sourceKey || items == nil else { return }

        print("\(PagingDebug.tag)\nbind()\nold source: \(self.sourceKey ?? "nil")\nnew source: \(sourceKey)")
        items?.close()
        self.sourceKey = sourceKey
        
        print("TEST\nCreating PagingItems")
        items = erasedFlow.map {
            let kotlinFlow = SkieKotlinFlow<Paging_commonPagingData<T>>($0)
            let swiftFlow = SkieSwiftFlow<Paging_commonPagingData<T>>(kotlinFlow)
            return PagingItems(pagingFlow: swiftFlow)
        }
    }

    func refresh() {
        print("\(PagingDebug.tag)\nrefresh()")
        items?.refresh()
    }

    func retry() {
        print("\(PagingDebug.tag)\nretry()")
        items?.retry()
    }

    /// Reading the visible index sends Paging3 the same viewport hint that
    /// `LazyPagingItems[index]` sends on Android and drives append requests.
    func loadNextPageIfNecessary(index: Int) {
        if let itemCount = items?.itemCount, index >= itemCount - 5 {
            print("\(PagingDebug.tag)\nViewport Hint\nindex: \(index)\nitemCount: \(itemCount)")
        }
        _ = items?.get(index: Int32(index))
    }

    deinit {
        print("""
        TEST
        ============================
        PagingItemsHolder DEINIT
        ============================
        """)
        items?.close()
    }
}

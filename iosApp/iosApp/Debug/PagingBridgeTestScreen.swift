import SwiftUI
import Shared

struct PagingBridgeTestScreen: View {
    @StateObject private var viewModel = PagingBridgeTestViewModel()
    @StateObject private var pagingHolder = PagingItemsHolder<Shared.TestPost>(sourceKey: "test-bridge")
    
    init() {
    }

    var body: some View {
        let _ = print("""
        TEST
        ============================
        PagingBridgeTestScreen BODY
        \(Date())
        ============================
        """)
        
        VStack {
            ObservePagingItemsHolder(pagingHolder: pagingHolder) { state, holder in
                let _ = print("""
                ObservePagingItemsHolder BODY
                itemCount=\(state.itemCount)
                refresh=\(state.isRefreshing)
                append=\(state.isAppending)
                time=\(Date())
                ----------------------------
                """)
                
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(0..<Int(state.itemCount), id: \.self) { index in
                            if let kotlinPost = holder.items?.get(index: Int32(index)) {
                                let _ = print("TEST\nRow BODY id=\(kotlinPost.id)\n----------------")
                                
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color.gray.opacity(0.2))
                                    .frame(height: 70)
                                    .overlay(
                                        Text(kotlinPost.title)
                                            .foregroundColor(.primary)
                                    )
                                    .onAppear {
                                        print("TEST\nPresenter get(index) \(index)\n----------------")
                                        holder.loadNextPageIfNecessary(index: index)
                                    }
                            }
                        }
                    }
                }
            }
        }
        .task {
            pagingHolder.bind(flow: viewModel.pagingFlow, sourceKey: "test-bridge")
            
            print("""
            TEST
            ============================
            PagingHolder Identity
            \(ObjectIdentifier(pagingHolder))
            ============================
            """)
            
            if let items = pagingHolder.items {
                print("""
                TEST
                ============================
                PagingItems Identity
                \(ObjectIdentifier(items))
                ============================
                """)
            }
        }
        .onAppear {
            print("""
            TEST
            ============================
            Screen APPEARED
            ============================
            """)
        }
        .onDisappear {
            print("""
            TEST
            ============================
            Screen DISAPPEARED
            ============================
            """)
        }
    }
}

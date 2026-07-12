import SwiftUI
import Shared

struct ObserveCommentsFlow<Content: View>: View {
    @State private var commentsState: PaginationState<Comment>? = nil
    
    // The raw flow from Kotlin
    let flow: any Kotlinx_coroutines_coreFlow
    
    let content: (PaginationState<Comment>?) -> Content
    
    init(flow: any Kotlinx_coroutines_coreFlow, @ViewBuilder content: @escaping (PaginationState<Comment>?) -> Content) {
        self.flow = flow
        self.content = content
    }

    var body: some View {
        content(commentsState)
            .task {
                let kotlinFlow = SkieKotlinFlow<PaginationState<Comment>>(flow)
                let skieFlow = SkieSwiftFlow<PaginationState<Comment>>(kotlinFlow)
                for await item in skieFlow {
                    self.commentsState = item
                }
            }
    }
}

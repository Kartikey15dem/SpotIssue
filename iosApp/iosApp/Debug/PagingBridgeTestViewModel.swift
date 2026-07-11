import Foundation
import Shared

class PagingBridgeTestViewModel: ObservableObject {
    let pagingFlow: SkieSwiftFlow<Paging_commonPagingData<Shared.TestPost>>
    
    init() {
        let kotlinFlow = SkieKotlinFlow(TestPagingRepository.shared.pagingFlow)
        self.pagingFlow = SkieSwiftFlow(kotlinFlow)
    }
}

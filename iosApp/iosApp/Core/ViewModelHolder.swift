import SwiftUI
import Shared

/**
 * A persistent container for Kotlin ViewModels in SwiftUI.
 * This ensures that the ViewModel instance is not recreated when the View struct
 * is re-initialized by SwiftUI's rendering engine.
 */
class ViewModelHolder<VM>: ObservableObject {
    let vm: VM
    
    init(vm: VM) {
        self.vm = vm
    }
}

/**
 * Extension to help creating ViewModels from KoinHelper within a @StateObject
 */
extension KoinHelper {
    func holder<VM>(_ factory: (KoinHelper) -> VM) -> ViewModelHolder<VM> {
        return ViewModelHolder(vm: factory(self))
    }
}

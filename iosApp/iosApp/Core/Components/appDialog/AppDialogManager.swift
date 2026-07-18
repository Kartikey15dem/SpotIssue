import SwiftUI

@MainActor
final class AppDialogManager: ObservableObject {

    static let shared = AppDialogManager()

    @Published var message: String? = nil
    @Published var isVisible = false

    private init() {}

    func show(
        _ message: String,
        duration: Double = 3.0 // kept for signature compatibility
    ) {
        self.message = message
        self.isVisible = true
    }
    
    func hide() {
        self.isVisible = false
        self.message = nil
    }
}

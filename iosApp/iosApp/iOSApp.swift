import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
      koinInit()
    }
    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
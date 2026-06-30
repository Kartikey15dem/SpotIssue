import SwiftUI

@MainActor
final class SnackbarManager: ObservableObject {

    static let shared = SnackbarManager()

    @Published private(set) var message: String?
    @Published private(set) var isVisible = false

    private var hideTask: Task<Void, Never>?

    private init() {}

    func show(
        _ message: String,
        duration: Double = 3.0
    ) {

        hideTask?.cancel()

        self.message = message

        withAnimation(.easeInOut(duration: 0.25)) {
            isVisible = true
        }

        hideTask = Task {

            try? await Task.sleep(
                for: .seconds(duration)
            )

            guard !Task.isCancelled else {
                return
            }

            withAnimation(.easeInOut(duration: 0.25)) {
                isVisible = false
            }

            try? await Task.sleep(
                for: .milliseconds(250)
            )

            guard !Task.isCancelled else {
                return
            }

            self.message = nil
        }
    }
}

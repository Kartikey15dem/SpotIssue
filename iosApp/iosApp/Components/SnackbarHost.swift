import SwiftUI

struct SnackbarHost: View {

    @StateObject
    private var manager = SnackbarManager.shared

    var body: some View {

        VStack {

            Spacer()

            if manager.isVisible,
               let message = manager.message {

                SnackbarView(
                    message: message
                )
                .padding(.bottom, 24)
                .transition(
                    .move(edge: .bottom)
                    .combined(with: .opacity)
                )
            }
        }
        .animation(
            .easeInOut(duration: 0.25),
            value: manager.isVisible
        )
        .allowsHitTesting(false)
    }
}

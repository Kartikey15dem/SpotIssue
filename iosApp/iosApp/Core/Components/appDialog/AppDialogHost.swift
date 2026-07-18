import SwiftUI

struct AppDialogHost: View {

    @StateObject
    private var manager = AppDialogManager.shared

    var body: some View {
        Color.clear
            .frame(width: 0, height: 0)
            .alert(
                "",
                isPresented: $manager.isVisible,
                presenting: manager.message
            ) { _ in
                Button(action: { manager.hide() }) {
                    Text("OK")
                        .font(.title3)
                        .bold()
                }
            } message: { msg in
                Text(msg)
            }
            .allowsHitTesting(false)
    }


}

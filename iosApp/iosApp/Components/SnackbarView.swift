import SwiftUI

struct SnackbarView: View {

    let message: String

    var body: some View {

        HStack {

            Text(message)
                .foregroundColor(.white)
                .font(IssueSpotTypography.bodyMedium)

            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(
            Color(red: 50/255, green: 50/255, blue: 50/255)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(radius: 6)
        .padding(.horizontal, 20)
    }
}

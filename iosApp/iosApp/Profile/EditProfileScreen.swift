import SwiftUI
import Shared
import PhotosUI

struct EditProfileScreen: View {
    @StateObject private var holder = KoinHelper().holder { $0.getEditProfileViewModel() }
    @EnvironmentObject var router: Router
    
    @State private var selectedItem: PhotosPickerItem? = nil
    @State private var otpCode: String = ""

    var body: some View {
        Observing(holder.vm.uiState) { (state: EditProfileState) in
            EditProfileMainView(state: state, vm: holder.vm, router: router, selectedItem: $selectedItem, otpCode: $otpCode)
        }
    }
}

private struct EditProfileMainView: View {
    let state: EditProfileState
    let vm: EditProfileViewModel
    let router: Router
    @Binding var selectedItem: PhotosPickerItem?
    @Binding var otpCode: String
    @State private var isCameraPresented = false
    
    var body: some View {
        VStack(spacing: 0) {
            header
            
            if state.isSaving {
                ProgressView().progressViewStyle(LinearProgressViewStyle(tint: IssueSpotColors.primary))
            }
            
            ScrollView {
                VStack(alignment: .leading, spacing: IssueSpotSpacing.medium) {
                    avatarSection
                    pickerButtons
                    Spacer().frame(height: IssueSpotSpacing.small)
                    formFields
                    Spacer().frame(height: IssueSpotSpacing.large)
                    actionButtons
                }
                .padding()
            }
        }
        .navigationBarHidden(true)
        .sheet(isPresented: $isCameraPresented) {
            ImagePicker(selectedImageURL: Binding(
                get: { nil },
                set: { url in
                    if let url = url {
                        vm.onIntent(intent: EditProfileIntentImageUrlChanged(url: url.absoluteString))
                    }
                }
            ), isPresented: $isCameraPresented, sourceType: .camera)
        }
        .onChange(of: selectedItem) { _, newItem in
            handlePhotoSelection(newItem)
        }
        .alert("Email Change", isPresented: emailDialogBinding) {
            emailDialogButtons
        } message: {
            emailDialogMessage
        }
        .task {
            await observeSideEffects()
        }
    }
    
    private var header: some View {
        HStack {
            Button(action: { router.goBack() }) {
                Image(systemName: "xmark")
                    .font(.system(size: 20))
                    .foregroundColor(IssueSpotColors.onSurface)
            }
            Spacer()
            Text("Edit Profile").font(IssueSpotTypography.titleLarge)
            Spacer()
            Button(action: { vm.onIntent(intent: EditProfileIntentLogoutClicked.shared) }) {
                Image(systemName: "rectangle.portrait.and.arrow.right").foregroundColor(IssueSpotColors.error)
            }
        }
        .padding()
        .background(IssueSpotColors.surface)
    }
    
    private var avatarSection: some View {
        Group {
            Text("Profile Picture")
                .font(IssueSpotTypography.titleMedium)
                .fontWeight(.bold)
            HStack {
                Spacer()
                ZStack(alignment: .bottomTrailing) {
                    if state.isLoadingImage {
                        ProgressView().frame(width: 120, height: 120)
                    } else {
                        avatarImage
                    }
                    Button(action: { vm.onIntent(intent: EditProfileIntentCaptureFromCameraClicked.shared) }) {
                        Image(systemName: "pencil")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(IssueSpotColors.onPrimary)
                            .frame(width: 36, height: 36)
                            .background(IssueSpotColors.primary)
                            .clipShape(Circle())
                    }
                }
                Spacer()
            }
        }
    }
    
    private var avatarImage: some View {
        Group {
            if let url = URL(string: state.imageUrl), !state.imageUrl.isEmpty {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Image(systemName: "person.circle.fill").resizable()
                }
            } else {
                Image(systemName: "person.circle.fill").resizable()
            }
        }
        .frame(width: 120, height: 120)
        .clipShape(Circle())
        .overlay(Circle().stroke(IssueSpotColors.primary, lineWidth: 3))
        .foregroundColor(IssueSpotColors.onSurfaceVariant)
        .background(Circle().fill(IssueSpotColors.surfaceVariant))
    }
    
    private var pickerButtons: some View {
        HStack(spacing: IssueSpotSpacing.small) {
            PhotosPicker(selection: $selectedItem, matching: .images) {
                HStack { Image(systemName: "photo"); Text("Gallery") }
                .frame(maxWidth: .infinity).padding(.vertical, 8)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(IssueSpotColors.outline, lineWidth: 1))
            }
            Button(action: { vm.onIntent(intent: EditProfileIntentCaptureFromCameraClicked.shared) }) {
                HStack { Image(systemName: "camera"); Text("Camera") }
                .frame(maxWidth: .infinity).padding(.vertical, 8)
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(IssueSpotColors.outline, lineWidth: 1))
            }
        }
        .foregroundColor(IssueSpotColors.primary)
        .font(IssueSpotTypography.labelMedium)
    }
    
    private var formFields: some View {
        Group {
            Text("Full Name").font(IssueSpotTypography.bodyMedium).fontWeight(.bold)
            TextField("Name", text: Binding(get: { state.name }, set: { vm.onIntent(intent: EditProfileIntentNameChanged(name: $0)) }))
                .padding().background(RoundedRectangle(cornerRadius: 12).stroke(IssueSpotColors.outline, lineWidth: 1))
            
            Text("Email Address").font(IssueSpotTypography.bodyMedium).fontWeight(.bold)
            HStack {
                TextField("Email", text: Binding(get: { state.email }, set: { vm.onIntent(intent: EditProfileIntentEmailChanged(email: $0)) }))
                    .disabled(true).padding().background(RoundedRectangle(cornerRadius: 12).stroke(IssueSpotColors.outline, lineWidth: 1))
                Button("Update") { vm.onIntent(intent: EditProfileIntentShowEmailChangeDialogClicked.shared) }
                    .font(IssueSpotTypography.labelLarge).padding(.horizontal).padding(.vertical, 10)
                    .background(IssueSpotColors.primary.opacity(0.1)).foregroundColor(IssueSpotColors.primary).cornerRadius(12)
            }
        }
    }
    
    private var actionButtons: some View {
        HStack(spacing: IssueSpotSpacing.small) {
            Button(action: { vm.onIntent(intent: EditProfileIntentResetClicked.shared) }) {
                HStack { Image(systemName: "xmark"); Text("Reset") }
                .frame(maxWidth: .infinity).padding()
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(IssueSpotColors.outline, lineWidth: 1))
                .foregroundColor(IssueSpotColors.onSurface)
            }
            Button(action: { vm.onIntent(intent: EditProfileIntentSaveChangesClicked.shared) }) {
                HStack {
                    if state.isSaving { ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white)) }
                    else { Image(systemName: "pencil"); Text("Save Changes") }
                }
                .frame(maxWidth: .infinity).padding().background(IssueSpotColors.primary).foregroundColor(.white).cornerRadius(12)
            }
            .disabled(state.isSaving || state.name.trimmingCharacters(in: .whitespaces).isEmpty)
        }
    }
    
    private var emailDialogBinding: Binding<Bool> {
        Binding(get: { state.showEmailChangeDialog }, set: { if !$0 { vm.onIntent(intent: EditProfileIntentDismissEmailChangeDialog.shared) } })
    }
    
    @ViewBuilder
    private var emailDialogButtons: some View {
        if state.emailChangeStep == .request {
            TextField("New Email", text: Binding(get: { state.newEmail }, set: { vm.onIntent(intent: EditProfileIntentNewEmailChanged(email: $0)) }))
            Button("Send OTP") { vm.onIntent(intent: EditProfileIntentRequestEmailChangeClicked.shared) }
            Button("Cancel", role: .cancel) { }
        } else {
            TextField("6-digit code", text: $otpCode).keyboardType(.numberPad)
            Button("Verify") { vm.onIntent(intent: EditProfileIntentVerifyEmailChangeClicked(otp: otpCode)) }
            Button("Cancel", role: .cancel) { }
        }
    }
    
    private var emailDialogMessage: Text {
        if state.emailChangeStep == .request { return Text("Enter your new email address. We will send a verification code to it.") }
        else { return Text("Enter the 6-digit code sent to \(state.newEmail)") }
    }
    
    private func handlePhotoSelection(_ item: PhotosPickerItem?) {
        if let item = item {
            Task {
                if let data = try? await item.loadTransferable(type: Data.self) {
                    let tempUrl = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".jpg")
                    try? data.write(to: tempUrl)
                    vm.onIntent(intent: EditProfileIntentImageUrlChanged(url: tempUrl.absoluteString))
                }
            }
        }
    }
    
    private func observeSideEffects() async {
        for await effect in vm.sideEffects {
            switch effect {
            case is EditProfileSideEffectProfileSaved, is EditProfileSideEffectBackPreseed, is EditProfileSideEffectLogoutSuccess:
                router.goBack()
            case is EditProfileSideEffectShowCamera:
                isCameraPresented = true
            case let snackbarEffect as EditProfileSideEffectShowSnackbar:
                SnackbarManager.shared.show(snackbarEffect.message)
            default: break
            }
        }
    }
}

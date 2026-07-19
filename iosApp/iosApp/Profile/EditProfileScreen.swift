import SwiftUI
import Shared
import PhotosUI
import UIKit

struct EditProfileScreen: View {
    @StateObject private var holder = KoinHelper().holder { $0.getEditProfileViewModel() }
    @EnvironmentObject var router: MainRouter
    
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
    let router: MainRouter
    @Binding var selectedItem: PhotosPickerItem?
    @Binding var otpCode: String
    @State private var isCameraPresented = false
    
    @State private var showRequestAlert = false
    @State private var showVerifyAlert = false
    
    var body: some View {
        ZStack {
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
                        Task {
                            let preparedUrl = await prepareProfileImage(from: url)
                            vm.onIntent(intent: EditProfileIntentImageUrlChanged(url: preparedUrl.absoluteString))
                        }
                    }
                }
            ), isPresented: $isCameraPresented, sourceType: .camera)
        }
        .onChange(of: selectedItem) { _, newItem in
            handlePhotoSelection(newItem)
        }
        .task {
            await observeSideEffects()
        }
        
        if state.showEmailChangeDialog {
            Color.black.opacity(0.4)
                .edgesIgnoringSafeArea(.all)
                .onTapGesture {
                    if !state.isEmailUpdating {
                        vm.onIntent(intent: EditProfileIntentDismissEmailChangeDialog.shared)
                    }
                }
            
            VStack(spacing: IssueSpotSpacing.medium) {
                Text(state.emailChangeStep == .request ? "Change Email" : "Verify Email")
                    .font(IssueSpotTypography.titleMedium)
                    .fontWeight(.bold)
                
                Text(state.emailChangeStep == .request ? "Enter your new email address. We will send a verification code to it." : "Enter the 6-digit code sent to \(state.newEmail)")
                    .font(IssueSpotTypography.bodyMedium)
                    .multilineTextAlignment(.center)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
                
                if state.emailChangeStep == .request {
                    TextField("New Email", text: Binding(get: { state.newEmail }, set: { vm.onIntent(intent: EditProfileIntentNewEmailChanged(email: $0)) }))
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                        .padding()
                        .frame(height: 56)
                        .background(IssueSpotColors.surfaceVariant)
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(IssueSpotColors.outline, lineWidth: 1))
                } else {
                    TextField("6-digit code", text: Binding(
                        get: { otpCode },
                        set: { otpCode = String($0.prefix(6)) }
                    ))
                    .keyboardType(.numberPad)
                    .padding()
                    .frame(height: 56)
                    .background(IssueSpotColors.surfaceVariant)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(IssueSpotColors.outline, lineWidth: 1))
                }
                
                HStack(spacing: IssueSpotSpacing.small) {
                    Button(action: {
                        vm.onIntent(intent: EditProfileIntentDismissEmailChangeDialog.shared)
                    }) {
                        Text("Cancel")
                            .frame(maxWidth: .infinity, minHeight: 56)
                            .background(Color.clear)
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(IssueSpotColors.outline, lineWidth: 1))
                            .foregroundColor(IssueSpotColors.primary)
                    }
                    .disabled(state.isEmailUpdating)
                    
                    Button(action: {
                        if state.emailChangeStep == .request {
                            vm.onIntent(intent: EditProfileIntentRequestEmailChangeClicked.shared)
                        } else {
                            vm.onIntent(intent: EditProfileIntentVerifyEmailChangeClicked(otp: otpCode))
                        }
                    }) {
                        HStack {
                            if state.isEmailUpdating {
                                ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Text(state.emailChangeStep == .request ? "Send OTP" : "Verify")
                            }
                        }
                        .frame(maxWidth: .infinity, minHeight: 56)
                        .background((state.isEmailUpdating || (state.emailChangeStep == .request ? state.newEmail.isEmpty : otpCode.count != 6)) ? IssueSpotColors.onSurface.opacity(0.12) : IssueSpotColors.primary)
                        .foregroundColor((state.isEmailUpdating || (state.emailChangeStep == .request ? state.newEmail.isEmpty : otpCode.count != 6)) ? IssueSpotColors.onSurface.opacity(0.38) : .white)
                        .cornerRadius(12)
                    }
                    .disabled(state.isEmailUpdating || (state.emailChangeStep == .request ? state.newEmail.isEmpty : otpCode.count != 6))
                }
            }
            .padding(IssueSpotSpacing.medium)
            .background(IssueSpotColors.surface)
            .cornerRadius(16)
            .padding(.horizontal, IssueSpotSpacing.large)
            .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: 5)
        }
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
                .frame(maxWidth: .infinity).frame(height: 56)
                .background(Color.clear)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(state.isLoadingImage ? IssueSpotColors.onSurface.opacity(0.12) : IssueSpotColors.outline, lineWidth: 1))
                .cornerRadius(12)
            }
            .disabled(state.isLoadingImage)
            
            Button(action: { vm.onIntent(intent: EditProfileIntentCaptureFromCameraClicked.shared) }) {
                HStack { Image(systemName: "camera"); Text("Camera") }
                .frame(maxWidth: .infinity).frame(height: 56)
                .background(Color.clear)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(state.isLoadingImage ? IssueSpotColors.onSurface.opacity(0.12) : IssueSpotColors.outline, lineWidth: 1))
                .cornerRadius(12)
            }
            .disabled(state.isLoadingImage)
        }
        .foregroundColor(state.isLoadingImage ? IssueSpotColors.onSurface.opacity(0.38) : IssueSpotColors.primary)
        .font(IssueSpotTypography.labelMedium)
    }
    
    private var formFields: some View {
        Group {
            Text("Full Name").font(IssueSpotTypography.bodyMedium).fontWeight(.bold)
            TextField("Name", text: Binding(get: { state.name }, set: { vm.onIntent(intent: EditProfileIntentNameChanged(name: $0)) }))
                .padding().frame(height: 56).background(RoundedRectangle(cornerRadius: 12).stroke(IssueSpotColors.outline, lineWidth: 1))
            
            Text("Email Address").font(IssueSpotTypography.bodyMedium).fontWeight(.bold)
            HStack(spacing: IssueSpotSpacing.small) {
                TextField("Email", text: .constant(state.email))
                    .disabled(true)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    .padding()
                    .frame(height: 56)
                    .background(IssueSpotColors.surfaceVariant)
                    .cornerRadius(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(IssueSpotColors.outline, lineWidth: 1))
                
                Button("Update") { vm.onIntent(intent: EditProfileIntentShowEmailChangeDialogClicked.shared) }
                    .font(IssueSpotTypography.labelLarge)
                    .frame(width: 100, height: 56)
                    .background(IssueSpotColors.primary.opacity(0.1)).foregroundColor(IssueSpotColors.primary).cornerRadius(12)
            }
        }
    }
    
    private var actionButtons: some View {
        HStack(spacing: IssueSpotSpacing.small) {
            Button(action: { vm.onIntent(intent: EditProfileIntentResetClicked.shared) }) {
                HStack { Image(systemName: "xmark"); Text("Reset") }
                .frame(maxWidth: .infinity, minHeight: 56)
                .background(Color.clear)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(state.isSaving ? IssueSpotColors.onSurface.opacity(0.12) : IssueSpotColors.outline, lineWidth: 1))
                .foregroundColor(state.isSaving ? IssueSpotColors.onSurface.opacity(0.38) : IssueSpotColors.primary)
            }
            .disabled(state.isSaving)
            
            Button(action: { vm.onIntent(intent: EditProfileIntentSaveChangesClicked.shared) }) {
                HStack {
                    if state.isSaving { ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white)) }
                    else { Image(systemName: "pencil"); Text("Save Changes") }
                }
                .frame(maxWidth: .infinity, minHeight: 56)
                .background((state.isSaving || state.name.trimmingCharacters(in: .whitespaces).isEmpty) ? IssueSpotColors.onSurface.opacity(0.12) : IssueSpotColors.primary)
                .foregroundColor((state.isSaving || state.name.trimmingCharacters(in: .whitespaces).isEmpty) ? IssueSpotColors.onSurface.opacity(0.38) : .white)
                .cornerRadius(12)
            }
            .disabled(state.isSaving || state.name.trimmingCharacters(in: .whitespaces).isEmpty)
        }
    }


    
    private func handlePhotoSelection(_ item: PhotosPickerItem?) {
        if let item = item {
            Task {
                if let data = try? await item.loadTransferable(type: Data.self) {
                    let tempUrl = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".jpg")
                    try? data.write(to: tempUrl)
                    let preparedUrl = await prepareProfileImage(from: tempUrl)
                    vm.onIntent(intent: EditProfileIntentImageUrlChanged(url: preparedUrl.absoluteString))
                }
            }
        }
    }

    private func prepareProfileImage(from url: URL) async -> URL {
        await Task.detached(priority: .userInitiated) {
            guard let imageData = try? Data(contentsOf: url),
                  let image = UIImage(data: imageData) else {
                return url
            }

            let maxDimension: CGFloat = 1024
            var scaledSize = image.size
            if image.size.width > maxDimension || image.size.height > maxDimension {
                let ratio = min(maxDimension / image.size.width, maxDimension / image.size.height)
                scaledSize = CGSize(width: image.size.width * ratio, height: image.size.height * ratio)
            }

            UIGraphicsBeginImageContextWithOptions(scaledSize, false, 1.0)
            image.draw(in: CGRect(origin: .zero, size: scaledSize))
            let resizedImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            guard let compressedData = (resizedImage ?? image).jpegData(compressionQuality: 0.7) else {
                return url
            }

            let compressedUrl = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + "_profile.jpg")
            do {
                try compressedData.write(to: compressedUrl)
                return compressedUrl
            } catch {
                return url
            }
        }.value
    }
    
    private func observeSideEffects() async {
        for await effect in vm.sideEffects {
            switch effect {
            case is EditProfileSideEffectProfileSaved, is EditProfileSideEffectBackPreseed, is EditProfileSideEffectLogoutSuccess:
                router.goBack()
            case is EditProfileSideEffectShowCamera:
                isCameraPresented = true

            case let dialogEffect as EditProfileSideEffectShowDialog:
                AppDialogManager.shared.show(dialogEffect.message)
            default: break
            }
        }
    }
}

import SwiftUI
import Shared
import PhotosUI

struct NameCaptureScreen: View {
    @StateObject private var holder: ViewModelHolder<NameCaptureViewModel>
    @EnvironmentObject var router: Router
    
    init(email: String) {
        _holder = StateObject(wrappedValue: KoinHelper().holder { $0.getNameCaptureViewModel(email: email) })
    }
    
    @State private var selectedItem: PhotosPickerItem? = nil
    @State private var isCameraPresented = false

    var body: some View {
        Observing(holder.vm.uiState) { state in
            VStack(alignment: .center) {
                Spacer().frame(height: IssueSpotSpacing.huge)
                
                Text("Complete Your Profile")
                    .font(IssueSpotTypography.headlineMedium)
                    .fontWeight(.bold)
                    .foregroundColor(IssueSpotColors.onSurface)
                
                Spacer().frame(height: IssueSpotSpacing.large)
                
                // Profile Picture
                ZStack(alignment: .bottomTrailing) {
                    if state.isLoadingImage {
                        ProgressView()
                            .frame(width: 120, height: 120)
                    } else {
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
                        .onTapGesture {
                            holder.vm.handleIntent(intent: NameCaptureIntent.PickFromGalleryClicked.shared)
                        }
                    }
                    
                    // Edit Icon
                    Button(action: {
                        holder.vm.handleIntent(intent: NameCaptureIntent.CaptureFromCameraClicked.shared)
                    }) {
                        Image(systemName: "pencil")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(IssueSpotColors.onPrimary)
                            .frame(width: 36, height: 36)
                            .background(IssueSpotColors.primary)
                            .clipShape(Circle())
                    }
                }
                
                Spacer().frame(height: IssueSpotSpacing.medium)
                
                // Image Source Buttons
                HStack(spacing: IssueSpotSpacing.small) {
                    PhotosPicker(selection: $selectedItem, matching: .images) {
                        HStack {
                            Image(systemName: "photo")
                                .font(.system(size: 16))
                            Text("Gallery")
                                .font(IssueSpotTypography.labelMedium)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(IssueSpotColors.outline, lineWidth: 1))
                        .foregroundColor(IssueSpotColors.primary)
                    }
                    .onChange(of: selectedItem) { _, newItem in
                        if let item = newItem {
                            Task {
                                if let data = try? await item.loadTransferable(type: Data.self) {
                                    let tempUrl = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".jpg")
                                    try? data.write(to: tempUrl)
                                    holder.vm.handleIntent(intent: NameCaptureIntent.ImageUrlChanged(url: tempUrl.absoluteString))
                                }
                            }
                        }
                    }
                    
                    Button(action: {
                        holder.vm.handleIntent(intent: NameCaptureIntent.CaptureFromCameraClicked.shared)
                    }) {
                        HStack {
                            Image(systemName: "camera")
                                .font(.system(size: 16))
                            Text("Camera")
                                .font(IssueSpotTypography.labelMedium)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(IssueSpotColors.outline, lineWidth: 1))
                        .foregroundColor(IssueSpotColors.primary)
                    }
                }
                
                Spacer().frame(height: IssueSpotSpacing.extraLarge)
                
                Text("Tell us the name by which you want to post issues")
                    .font(IssueSpotTypography.bodyLarge)
                    .foregroundColor(IssueSpotColors.onSurfaceVariant)
                    .multilineTextAlignment(.center)
                
                Spacer().frame(height: IssueSpotSpacing.medium)
                
                TextField("Full name", text: Binding(
                    get: { state.name },
                    set: { holder.vm.handleIntent(intent: NameCaptureIntent.NameChanged(name: $0)) }
                ))
                .autocapitalization(.words)
                .font(IssueSpotTypography.bodyLarge)
                .padding()
                .frame(height: 56)
                .background(IssueSpotColors.surface)
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(IssueSpotColors.outline, lineWidth: 1)
                )
                .disabled(state.isLoading)
                
                Spacer().frame(height: IssueSpotSpacing.large)
                
                Button(action: { holder.vm.handleIntent(intent: NameCaptureIntent.SubmitClicked.shared) }) {
                    ZStack {
                        if state.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.onPrimary))
                        } else {
                            Text("Get Started")
                                .font(IssueSpotTypography.labelLarge)
                                .fontWeight(.bold)
                                .foregroundColor(IssueSpotColors.onPrimary)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(!state.name.trimmingCharacters(in: .whitespaces).isEmpty && !state.isLoading ? IssueSpotColors.primary : IssueSpotColors.primary.opacity(0.5))
                    .cornerRadius(12)
                }
                .disabled(state.name.trimmingCharacters(in: .whitespaces).isEmpty || state.isLoading)
                
                Spacer()
            }
            .padding(IssueSpotSpacing.large)
            .background(IssueSpotColors.surface.ignoresSafeArea())
            .navigationBarHidden(true)
            .sheet(isPresented: $isCameraPresented) {
                ImagePicker(selectedImageURL: Binding(
                    get: { nil },
                    set: { url in
                        if let url = url {
                            holder.vm.handleIntent(intent: NameCaptureIntent.ImageUrlChanged(url: url.absoluteString))
                        }
                    }
                ), isPresented: $isCameraPresented, sourceType: .camera)
            }
            .task {
                for await effect in holder.vm.effect {
                    switch effect {
                    case is NameCaptureEffect.ShowImagePicker:
                        break
                    case is NameCaptureEffect.ShowCamera:
                        isCameraPresented = true
                    case let error as NameCaptureEffect.ShowSnackbar:
                        SnackbarManager.shared.show(error.message)
                    default:
                        break
                    }
                }
            }
        }
    }
}

import SwiftUI
import Shared
import PhotosUI

struct CreatePostScreen: View {
    @StateObject private var holder = KoinHelper().holder { $0.getCreatePostViewModel() }
    @EnvironmentObject var router: Router

    @State private var selectedItems: [PhotosPickerItem] = []
    @State private var showDocumentPicker = false

    var body: some View {
        Observing(holder.vm.uiState) { state in
            ZStack(alignment: .bottom) {
                IssueSpotColors.surface.ignoresSafeArea()

                VStack(spacing: 0) {
                    // Header
                    HStack(alignment: .top) {
                        Button(action: { holder.vm.onIntent(intent: CreatePostIntentCloseClicked.shared) }) {
                            Image(systemName: "xmark")
                                .font(.system(size: 20))
                                .foregroundColor(IssueSpotColors.onSurface)
                                .frame(width: 36, height: 36)
                        }

                        Spacer().frame(width: IssueSpotSpacing.small)

                        // Avatar
                        Group {
                            if let userImageUrl = state.userImageUrl, let url = URL(string: userImageUrl) {
                                AsyncImage(url: url) { image in
                                    image.resizable().scaledToFill()
                                } placeholder: {
                                    Image(systemName: "person.circle.fill").resizable()
                                }
                            } else {
                                Image(systemName: "person.circle.fill").resizable()
                            }
                        }
                        .frame(width: 48, height: 48)
                        .clipShape(Circle())
                        .foregroundColor(IssueSpotColors.onSurfaceVariant)
                        .background(Circle().fill(IssueSpotColors.surfaceVariant))

                        Spacer().frame(width: IssueSpotSpacing.smallMedium)

                        // User Info
                        VStack(alignment: .leading, spacing: 2) {
                            Text(state.userName)
                                .font(IssueSpotTypography.titleMedium)
                                .fontWeight(.semibold)
                                .lineLimit(1)
                            Text(state.location)
                                .font(IssueSpotTypography.bodySmall)
                                .foregroundColor(IssueSpotColors.onSurfaceVariant)
                                .lineLimit(3)
                        }

                        Spacer()

                        // Post Button
                        Button(action: { holder.vm.onIntent(intent: CreatePostIntentPostIssueClicked.shared) }) {
                            Text("Post")
                                .font(IssueSpotTypography.labelMedium)
                                .fontWeight(.semibold)
                                .padding(.horizontal, IssueSpotSpacing.medium)
                                .padding(.vertical, IssueSpotSpacing.small)
                                .foregroundColor(IssueSpotColors.postButtonText)
                                .background(IssueSpotColors.postButtonBackground)
                                .cornerRadius(20)
                        }
                    }
                    .padding(IssueSpotSpacing.smallMedium)

                    Divider().background(IssueSpotColors.outline.opacity(0.15))

                    Spacer().frame(height: IssueSpotSpacing.smallMedium)

                    // Main Content Box (Mirroring Android's Bordered Box)
                    VStack(alignment: .leading, spacing: 0) {
                        ScrollView {
                            VStack(alignment: .leading, spacing: 12) {
                                // Input Area
                                ZStack(alignment: .topLeading) {
                                    TextEditor(text: Binding(
                                        get: { state.description_ },
                                        set: { holder.vm.onIntent(intent: CreatePostIntentDescriptionChanged(description: $0)) }
                                    ))
                                        .font(IssueSpotTypography.bodyLarge)
                                        .frame(minHeight: 150)
                                        .scrollContentBackground(.hidden)

                                    if state.description_.isEmpty {
                                        Text("Describe the issue you want to report...")
                                            .font(IssueSpotTypography.bodyLarge)
                                            .foregroundColor(IssueSpotColors.onSurfaceVariant)
                                            .padding(.top, 8)
                                            .padding(.leading, 5)
                                            .allowsHitTesting(false)
                                    }
                                }

                                // Media Preview
                                if let selectedMedia = state.selectedMedia, !selectedMedia.isEmpty {
                                    MediaPreviewContent(
                                        mediaItems: selectedMedia,
                                        onRemove: { holder.vm.onIntent(intent: CreatePostIntentRemoveMedia.shared) },
                                        onRemoveImage: { uri in holder.vm.onIntent(intent: CreatePostIntentRemoveImage(uri: uri)) }
                                    )
                                        .padding(.top, 8)
                                }
                            }
                            .padding(IssueSpotSpacing.small)
                        }
                    }
                    .background(IssueSpotColors.surface)
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(IssueSpotColors.outline, lineWidth: 1)
                    )
                    .padding(.horizontal, IssueSpotSpacing.smallMedium)

                    Spacer()
                }
                .allowsHitTesting(!state.isLoading) // Blocks interactions while loading

                // Loading Overlay
                if state.isLoading {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                        .overlay(
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: IssueSpotColors.primary))
                        )
                }

                // Bottom Toolbar
                if state.selectedMedia == nil || state.selectedMedia!.isEmpty {
                    HStack(spacing: IssueSpotSpacing.small) {
                        Spacer()

                        PhotosPicker(selection: $selectedItems, maxSelectionCount: 5, matching: .any(of: [.images, .videos])) {
                            Image(systemName: "photo")
                                .font(.system(size: 24))
                                .foregroundColor(IssueSpotColors.onSurfaceVariant)
                                .frame(width: 44, height: 44)
                                .background(IssueSpotColors.onSurfaceVariant.opacity(0.1))
                                .cornerRadius(8)
                        }
                        .onChange(of: selectedItems) { _, newItems in
                            if !newItems.isEmpty {
                                Task {
                                    var mediaPairs: [KotlinPair<NSString, NSString>] = []
                                    for item in newItems {
                                        // Safely extract both Video and Image types to temporary URLs
                                        if item.supportedContentTypes.contains(where: { $0.conforms(to: .movie) }) {
                                            if let file = try? await item.loadTransferable(type: VideoTransfer.self) {
                                                mediaPairs.append(KotlinPair(first: file.url.absoluteString as NSString, second: "video/mp4" as NSString))
                                            }
                                        } else if let data = try? await item.loadTransferable(type: Data.self) {
                                            let tempUrl = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".jpg")
                                            try? data.write(to: tempUrl)
                                            mediaPairs.append(KotlinPair(first: tempUrl.absoluteString as NSString, second: "image/jpeg" as NSString))
                                        }
                                    }
                                    holder.vm.onIntent(intent: CreatePostIntentVisualMediaAdded(mediaItems: mediaPairs))
                                    selectedItems.removeAll()
                                }
                            }
                        }

                        Button(action: { showDocumentPicker = true }) {
                            Image(systemName: "plus") // Match Android's ic_add
                                .font(.system(size: 24))
                                .foregroundColor(IssueSpotColors.onSurfaceVariant)
                                .frame(width: 44, height: 44)
                                .background(IssueSpotColors.onSurfaceVariant.opacity(0.1))
                                .cornerRadius(8)
                        }
                        .sheet(isPresented: $showDocumentPicker) {
                            DocumentPicker(isPresented: $showDocumentPicker) { url in
                                holder.vm.onIntent(intent: CreatePostIntentAddPdfClicked.shared)
                                holder.vm.onIntent(intent: CreatePostIntentDocumentUrlAdded(uri: url.absoluteString))
                            }
                        }
                    }
                    .padding(.horizontal, IssueSpotSpacing.large)
                    .padding(.bottom, IssueSpotSpacing.large)
                    .padding(.top, IssueSpotSpacing.extraSmall)
                }
            }
            .navigationBarHidden(true)
            .task {
                for await effect in holder.vm.sideEffects {
                    switch effect {
                    case is CreatePostSideEffectNavigateBack:
                        router.goBack()
                    case let errorEffect as CreatePostSideEffectShowError:
                        print("Error: \(errorEffect.message)")
                    case is CreatePostSideEffectStartBackgroundUpload:
                        PostUploadWorker.shared.enqueue()
                    default:
                        break
                    }
                }
            }
        }
    }
}

// Helper object to load Videos safely from PhotosPicker in iOS
struct VideoTransfer: Transferable {
    let url: URL
    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(contentType: .movie) { video in
            SentTransferredFile(video.url)
        } importing: { received in
            let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent(received.file.lastPathComponent)
            if FileManager.default.fileExists(atPath: tempURL.path) {
                try FileManager.default.removeItem(at: tempURL)
            }
            try FileManager.default.copyItem(at: received.file, to: tempURL)
            return VideoTransfer(url: tempURL)
        }
    }
}
import Foundation
import UIKit
import Shared

@MainActor
class PostUploadWorker {
    static let shared = PostUploadWorker()
    
    func enqueue() {
        var backgroundTask: UIBackgroundTaskIdentifier = .invalid
        backgroundTask = UIApplication.shared.beginBackgroundTask {
            Task {
                await MainActor.run { UIApplication.shared.endBackgroundTask(backgroundTask) }
            }
        }
        
        Task {
            var preparedTemporaryPaths: [String] = []
            do {
                let prefRepository = KoinHelper().getUserPreferencesRepository()
                let postRepository = KoinHelper().getPostRepository()
                let fileManager = FileManager.default
                
                // Get the current user data
                var currentUserData: Shared.UserData? = nil
                for await userData in prefRepository.userData {
                    currentUserData = userData
                    break
                }
                
                guard let userData = currentUserData else {
                    await MainActor.run { UIApplication.shared.endBackgroundTask(backgroundTask) }
                    return
                }
                
                let draft = userData.uploadDraftState
                guard draft.status == Shared.UploadStatus.uploading else {
                    await MainActor.run { UIApplication.shared.endBackgroundTask(backgroundTask) }
                    return
                }
                
                // Validate size
                var totalSize: Int64 = 0
                let selectedMedia = draft.selectedMedia ?? []
                
                for mediaItem in selectedMedia {
                    if let url = URL(string: mediaItem.uri), url.isFileURL {
                        if let attrs = try? fileManager.attributesOfItem(atPath: url.path) {
                            totalSize += attrs[.size] as? Int64 ?? 0
                        }
                    }
                }
                
                if totalSize > 50 * 1024 * 1024 {
                    let updated = Shared.UploadDraftState(
                        status: .error,
                        postText: draft.postText,
                        selectedMedia: draft.selectedMedia,
                        errorMessage: "Total media size exceeds 50MB limit."
                    )
                    try await prefRepository.updateUploadDraftState(state: updated)
                    await MainActor.run { UIApplication.shared.endBackgroundTask(backgroundTask) }
                    return
                }
                
                // Compress / Prepare paths natively
                var mediaPaths: [String] = []
                let mediaType = draft.selectedMedia?.first?.type ?? Shared.MediaType.image
                
                for mediaItem in selectedMedia {
                    guard let url = URL(string: mediaItem.uri) else { continue }
                    
                    if mediaType == Shared.MediaType.image {
                        if let imageData = try? Data(contentsOf: url),
                           let image = UIImage(data: imageData) {
                            
                            // Resize image to max 1024x1024 to ensure it stays under 1MB
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
                            
                            // Compress image (0.7 quality is a solid default)
                            let finalImage = resizedImage ?? image
                            if let compressedData = finalImage.jpegData(compressionQuality: 0.7) {
                                let compressedUrl = fileManager.temporaryDirectory.appendingPathComponent(UUID().uuidString + "_compressed.jpg")
                                do {
                                    try compressedData.write(to: compressedUrl)
                                    mediaPaths.append(compressedUrl.path)
                                    preparedTemporaryPaths.append(compressedUrl.path)
                                } catch {
                                    print("Failed to write compressed image: \(error)")
                                }
                            }
                        }
                    } else {
                        mediaPaths.append(url.path)
                    }
                }
                
                let createPost = Shared.CreatePost(
                    postText: draft.postText,
                    mediaType: mediaType,
                    postLevel: "LOCALITY",
                    mediaFilePaths: mediaPaths,
                    location: userData.userLocation
                )
                
                let result = try await postRepository.createPost(post: createPost)
                
                if result is Shared.DataStateSuccess<AnyObject> || String(describing: type(of: result)).contains("DataStateSuccess") {
                    let updated = Shared.UploadDraftState(
                        status: .success,
                        postText: draft.postText,
                        selectedMedia: draft.selectedMedia,
                        errorMessage: nil
                    )
                    try await prefRepository.updateUploadDraftState(state: updated)
                } else if let errorResult = result as? Shared.DataStateError {
                    let updated = Shared.UploadDraftState(
                        status: .error,
                        postText: draft.postText,
                        selectedMedia: draft.selectedMedia,
                        errorMessage: errorResult.exception.message ?? "Failed to create post on server."
                    )
                    try await prefRepository.updateUploadDraftState(state: updated)
                } else {
                    let updated = Shared.UploadDraftState(
                        status: .error,
                        postText: draft.postText,
                        selectedMedia: draft.selectedMedia,
                        errorMessage: "Unknown error during upload."
                    )
                    try await prefRepository.updateUploadDraftState(state: updated)
                }
                
            } catch {
                print("Background upload failed: \(error)")
                let prefRepository = KoinHelper().getUserPreferencesRepository()
                var currentUserData: Shared.UserData? = nil
                for await userData in prefRepository.userData {
                    currentUserData = userData
                    break
                }
                
                if let userData = currentUserData {
                    let draft = userData.uploadDraftState
                    let updated = Shared.UploadDraftState(
                        status: .error,
                        postText: draft.postText,
                        selectedMedia: draft.selectedMedia,
                        errorMessage: error.localizedDescription
                    )
                    try? await prefRepository.updateUploadDraftState(state: updated)
                }
            }
            
            preparedTemporaryPaths.forEach { path in
                try? FileManager.default.removeItem(atPath: path)
            }
            await MainActor.run { UIApplication.shared.endBackgroundTask(backgroundTask) }
            backgroundTask = .invalid
        }
    }
}

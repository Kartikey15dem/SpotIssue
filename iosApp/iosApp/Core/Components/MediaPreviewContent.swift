import SwiftUI
import Shared
import AVKit

struct MediaPreviewContent: View {
    let mediaItems: [SelectedMediaItem]
    let onRemove: () -> Void
    let onRemoveImage: (String) -> Void
    @EnvironmentObject var overlayController: MediaOverlayController

    var body: some View {
        VStack(alignment: .trailing, spacing: 6) {
            Button(action: onRemove) {
                Image(systemName: "xmark")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 28, height: 28)
                    .background(Color.black)
                    .clipShape(Circle())
            }

            if let first = mediaItems.first {
                Group {
                    switch first.type {
                    case .image:
                        ImageGrid(images: mediaItems, onRemove: onRemoveImage) { index in
                            overlayController.show(type: .image, urls: mediaItems.map { $0.uri }, initialIndex: index)
                        }
                    case .video:
                        VideoPreview(uri: first.uri, forceAspectRatio: true) {
                            overlayController.show(type: .video, urls: mediaItems.map { $0.uri })
                        }
                    case .pdf:
                        PdfPreview(uri: first.uri) {
                            overlayController.show(type: .pdf, urls: mediaItems.map { $0.uri })
                        }
                    default:
                        EmptyView()
                    }
                }
                .frame(maxWidth: .infinity)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
    }
}

// MARK: - Image Grid
struct ImageGrid: View {
    let images: [SelectedMediaItem]
    let onRemove: (String) -> Void
    let onImageClick: (Int) -> Void
    var isEditable: Bool = true

    var body: some View {
        let count = images.count
        let spacing: CGFloat = 4
        let gridHeight: CGFloat = 300

        Group {
            if count == 1 {
                GridImageItem(uri: images[0].uri, onRemove: { onRemove(images[0].uri) }, onClick: { onImageClick(0) }, contentMode: .fit, showRemoveButton: false)
                    .frame(maxWidth: .infinity, maxHeight: UIScreen.main.bounds.width)
            } else if count == 2 {
                HStack(spacing: spacing) {
                    GridImageItem(uri: images[0].uri, onRemove: { onRemove(images[0].uri) }, onClick: { onImageClick(0) }, contentMode: .fill, showRemoveButton: isEditable)
                    GridImageItem(uri: images[1].uri, onRemove: { onRemove(images[1].uri) }, onClick: { onImageClick(1) }, contentMode: .fill, showRemoveButton: isEditable)
                }
                .frame(height: gridHeight)
            } else if count == 3 {
                HStack(spacing: spacing) {
                    GridImageItem(uri: images[0].uri, onRemove: { onRemove(images[0].uri) }, onClick: { onImageClick(0) }, contentMode: .fill, showRemoveButton: isEditable)
                    VStack(spacing: spacing) {
                        GridImageItem(uri: images[1].uri, onRemove: { onRemove(images[1].uri) }, onClick: { onImageClick(1) }, contentMode: .fill, showRemoveButton: isEditable)
                        GridImageItem(uri: images[2].uri, onRemove: { onRemove(images[2].uri) }, onClick: { onImageClick(2) }, contentMode: .fill, showRemoveButton: isEditable)
                    }
                }
                .frame(height: gridHeight)
            } else {
                VStack(spacing: spacing) {
                    HStack(spacing: spacing) {
                        GridImageItem(uri: images[0].uri, onRemove: { onRemove(images[0].uri) }, onClick: { onImageClick(0) }, contentMode: .fill, showRemoveButton: isEditable)
                        GridImageItem(uri: images[1].uri, onRemove: { onRemove(images[1].uri) }, onClick: { onImageClick(1) }, contentMode: .fill, showRemoveButton: isEditable)
                    }
                    HStack(spacing: spacing) {
                        GridImageItem(uri: images[2].uri, onRemove: { onRemove(images[2].uri) }, onClick: { onImageClick(2) }, contentMode: .fill, showRemoveButton: isEditable)
                        
                        GridImageItem(uri: images[3].uri, onRemove: { onRemove(images[3].uri) }, onClick: { onImageClick(3) }, contentMode: .fill, showRemoveButton: isEditable)
                            .overlay(
                                Group {
                                    if count > 4 {
                                        ZStack {
                                            Color.black.opacity(0.5)
                                                .contentShape(Rectangle())
                                            Text("+\(count - 4)")
                                                .font(IssueSpotTypography.headlineMedium)
                                                .foregroundColor(.white)
                                                .fontWeight(.bold)
                                        }
                                        .onTapGesture { onImageClick(3) }
                                    }
                                }
                            )
                    }
                }
                .frame(height: gridHeight)
            }
        }
    }
}

struct GridImageItem: View {
    let uri: String
    let onRemove: () -> Void
    let onClick: () -> Void
    var contentMode: ContentMode = .fill
    var showRemoveButton: Bool = true

    var body: some View {
        ZStack(alignment: .topTrailing) {
            AsyncImage(url: URL(string: uri)) { image in
                if contentMode == .fit {
                    image.resizable().scaledToFit()
                } else {
                    image.resizable().scaledToFill()
                }
            } placeholder: {
                IssueSpotColors.surfaceVariant
            }
            .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: contentMode == .fit ? nil : .infinity)
            .contentShape(Rectangle())
            .onTapGesture(perform: onClick)
            .clipped()

            if showRemoveButton {
                Button(action: onRemove) {
                    Image(systemName: "xmark")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .padding(5)
                        .background(Color.black.opacity(0.6))
                        .clipShape(Circle())
                }
                .padding(4)
            }
        }
        .frame(minWidth: 0, maxWidth: .infinity, minHeight: 0, maxHeight: contentMode == .fit ? nil : .infinity)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

// MARK: - Video Preview
struct VideoPreview: View {
    let uri: String
    var forceAspectRatio: Bool = true
    let onFullscreenClick: () -> Void

    @State private var player: AVPlayer?
    @State private var isPlaying = false
    @State private var isMuted = false
    @State private var showControls = true
    @State private var videoAspectRatio: CGFloat = 1.0

    var body: some View {
        ZStack {
            Color.black

            if let player = player {
                CustomVideoPlayer(player: player)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .onTapGesture {
                        withAnimation { showControls.toggle() }
                    }
            }

            // Center Play/Pause Button
            if showControls {
                Button(action: togglePlayPause) {
                    Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: 32))
                        .foregroundColor(.white)
                        .frame(width: 64, height: 64)
                        .background(Color.black.opacity(0.5))
                        .clipShape(Circle())
                }
                .transition(.opacity)
            }

            // Bottom Controls Layer
            VStack {
                Spacer()
                HStack {
                    Button(action: {
                        isMuted.toggle()
                        player?.isMuted = isMuted
                    }) {
                        Image(systemName: isMuted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                            .font(.system(size: 20))
                            .foregroundColor(.white)
                            .frame(width: 40, height: 40)
                            .background(Color.black.opacity(0.5))
                            .clipShape(Circle())
                    }

                    Spacer()

                    Button(action: onFullscreenClick) {
                        Image(systemName: "arrow.up.left.and.arrow.down.right")
                            .font(.system(size: 20))
                            .foregroundColor(.white)
                            .frame(width: 40, height: 40)
                            .background(Color.black.opacity(0.5))
                            .clipShape(Circle())
                    }
                }
                .padding(8)
            }
        }
        .frame(maxWidth: .infinity)
        .modifier(OptionalAspectRatio(ratio: videoAspectRatio, isEnabled: forceAspectRatio))
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .onAppear {
            if let url = URL(string: uri) {
                let avPlayer = AVPlayer(url: url)
                self.player = avPlayer

                // Extract intrinsic video size to calculate ratio
                Task {
                    if let track = try? await avPlayer.currentItem?.asset.loadTracks(withMediaType: .video).first,
                       let size = try? await track.load(.naturalSize),
                       let transform = try? await track.load(.preferredTransform) {
                        let videoSize = size.applying(transform)
                        let width = abs(videoSize.width)
                        let height = abs(videoSize.height)
                        let ratio = width / height
                        await MainActor.run {
                            self.videoAspectRatio = max(ratio, 1.0)
                        }
                    }
                }

                // Add observer for video end
                NotificationCenter.default.addObserver(forName: .AVPlayerItemDidPlayToEndTime, object: avPlayer.currentItem, queue: .main) { _ in
                    isPlaying = false
                    showControls = true
                    avPlayer.seek(to: .zero)
                }
            }
        }
    }

    private func togglePlayPause() {
        guard let player = player else { return }
        if isPlaying {
            player.pause()
        } else {
            player.play()
            showControls = false
        }
        isPlaying.toggle()
    }
}

// Helper modifier to conditionally apply aspect ratio
struct OptionalAspectRatio: ViewModifier {
    let ratio: CGFloat
    let isEnabled: Bool

    func body(content: Content) -> some View {
        if isEnabled {
            content.aspectRatio(ratio, contentMode: .fit)
        } else {
            content
        }
    }
}

// Wrapper for AVPlayerLayer to hide native iOS video controls
struct CustomVideoPlayer: UIViewRepresentable {
    var player: AVPlayer
    var videoGravity: AVLayerVideoGravity = .resizeAspect

    func makeUIView(context: Context) -> UIView {
        let view = PlayerView()
        view.player = player
        view.playerLayer.videoGravity = videoGravity
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}

class PlayerView: UIView {
    var player: AVPlayer? {
        get { playerLayer.player }
        set { playerLayer.player = newValue }
    }

    var playerLayer: AVPlayerLayer {
        return layer as! AVPlayerLayer
    }

    override static var layerClass: AnyClass {
        return AVPlayerLayer.self
    }
}

// MARK: - PDF Preview
struct PdfPreview: View {
    let uri: String
    let onClick: () -> Void

    @State private var pageImages: [UIImage] = []
    @State private var isLoading = true

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.gray.opacity(0.1)

            if isLoading {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle())
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(pageImages, id: \.self) { image in
                            Image(uiImage: image)
                                .resizable()
                                .scaledToFit()
                                .clipShape(RoundedRectangle(cornerRadius: 4))
                                .shadow(radius: 2)
                        }
                    }
                    .padding(8)
                }
            }

            // "PDF" Badge matching Compose
            Text("PDF")
                .font(IssueSpotTypography.labelSmall)
                .foregroundColor(.white)
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(Color.red)
                .cornerRadius(4)
                .padding(8)
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(1.0, contentMode: .fit) // Perfect square matching Android's aspectRatio(1f)
        .onTapGesture(perform: onClick)
        .onAppear {
            Task {
                if let url = URL(string: uri) {
                    pageImages = await generatePdfImages(from: url)
                    isLoading = false
                }
            }
        }
    }

    // Background task to extract PDF pages to UIImages
    private func generatePdfImages(from url: URL) async -> [UIImage] {
        return await Task.detached(priority: .userInitiated) {
            guard let document = CGPDFDocument(url as CFURL) else { return [] }
            var images: [UIImage] = []

            // Limit to first 10 pages for preview performance
            let pageCount = min(document.numberOfPages, 10)

            for pageNum in 1...pageCount {
                guard let page = document.page(at: pageNum) else { continue }
                let pageRect = page.getBoxRect(.mediaBox)
                let renderer = UIGraphicsImageRenderer(size: pageRect.size)

                let img = renderer.image { ctx in
                    UIColor.white.set()
                    ctx.fill(pageRect)
                    ctx.cgContext.translateBy(x: 0.0, y: pageRect.size.height)
                    ctx.cgContext.scaleBy(x: 1.0, y: -1.0)
                    ctx.cgContext.drawPDFPage(page)
                }
                images.append(img)
            }
            return images
        }.value
    }
}
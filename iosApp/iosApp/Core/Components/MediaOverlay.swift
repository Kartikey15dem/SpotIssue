import SwiftUI
import Shared
import AVKit
import PDFKit

struct PDFKitView: UIViewRepresentable {
    let url: URL
    @Binding var currentPage: Int
    @Binding var totalPages: Int
    
    class Coordinator: NSObject {
        var parent: PDFKitView
        init(_ parent: PDFKitView) {
            self.parent = parent
        }
        @objc func pageChanged(_ notification: Notification) {
            if let pdfView = notification.object as? PDFView,
               let current = pdfView.currentPage,
               let doc = pdfView.document {
                let index = doc.index(for: current)
                DispatchQueue.main.async {
                    self.parent.currentPage = index
                }
            }
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeUIView(context: Context) -> PDFView {
        let pdfView = PDFView()
        pdfView.autoScales = true
        pdfView.displayMode = .singlePage
        pdfView.displayDirection = .horizontal
        pdfView.usePageViewController(true)
        pdfView.backgroundColor = .clear
        
        NotificationCenter.default.addObserver(context.coordinator, selector: #selector(Coordinator.pageChanged(_:)), name: .PDFViewPageChanged, object: pdfView)
        
        DispatchQueue.global(qos: .userInitiated).async {
            if let document = PDFDocument(url: url) {
                DispatchQueue.main.async {
                    pdfView.document = document
                    self.totalPages = document.pageCount
                }
            }
        }
        
        return pdfView
    }

    func updateUIView(_ pdfView: PDFView, context: Context) {
        if pdfView.document?.documentURL != url {
            DispatchQueue.global(qos: .userInitiated).async {
                if let document = PDFDocument(url: url) {
                    DispatchQueue.main.async {
                        pdfView.document = document
                        self.totalPages = document.pageCount
                    }
                }
            }
        } else if let doc = pdfView.document, doc.pageCount > 0 {
            if currentPage >= 0 && currentPage < doc.pageCount {
                if let targetPage = doc.page(at: currentPage), pdfView.currentPage != targetPage {
                    pdfView.go(to: targetPage)
                }
            }
        }
    }
}

import SwiftUI
import Shared
import AVKit

// MARK: - Media Overlay Controller
class MediaOverlayController: ObservableObject {
    @Published var isShowing = false
    @Published var mediaType: Shared.MediaType = .image
    @Published var urls: [String] = []
    @Published var initialIndex: Int = 0

    func show(type: Shared.MediaType, urls: [String], initialIndex: Int = 0) {
        self.mediaType = type
        self.urls = urls
        self.initialIndex = initialIndex
        self.isShowing = true
    }

    func hide() {
        self.isShowing = false
        self.urls = []
    }
}

// MARK: - Main Overlay View
struct MediaOverlayView: View {
    @EnvironmentObject var controller: MediaOverlayController
    @State private var currentIndex: Int = 0
    @State private var currentScale: CGFloat = 1.0 // Tracks zoom to hide controls

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color.black.ignoresSafeArea()

            if !controller.urls.isEmpty {
                // Media Carousel
                TabView(selection: $currentIndex) {
                    ForEach(0..<controller.urls.count, id: \.self) { index in
                        let urlString = controller.urls[index]
                        if let url = URL(string: urlString) {
                            if controller.mediaType == .image {
                                ZoomableImageView(url: url, currentScale: $currentScale)
                                    .tag(index)
                            } else if controller.mediaType == .pdf {
                                ZoomablePdfView(url: url, currentScale: $currentScale)
                                    .tag(index)
                            } else if controller.mediaType == .video {
                                FullScreenVideoPlayer(url: url)
                                    .tag(index)
                            }
                        }
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
                .onAppear {
                    currentIndex = controller.initialIndex
                }

                // Navigation Arrows & Page Indicator (Only for Images/PDFs when NOT zoomed)
                if (controller.mediaType == .image || controller.mediaType == .pdf) && currentScale == 1.0 {
                    OverlayNavigationControls(
                        currentIndex: $currentIndex,
                        totalCount: controller.urls.count
                    )
                }
            }

            // Global Back/Close Button (Top Left)
            Button(action: { controller.hide() }) {
                Image(systemName: "arrow.left")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 36, height: 36)
                    .background(Color.black)
                    .clipShape(Circle())
                    .padding(.leading, 6)
                    .padding(.top, 48) // Account for status bar
            }
            .zIndex(2)
        }
    }
}

// MARK: - Navigation Controls
struct OverlayNavigationControls: View {
    @Binding var currentIndex: Int
    let totalCount: Int

    var body: some View {
        ZStack {
            // Arrows
            if totalCount > 1 {
                HStack {
                    if currentIndex > 0 {
                        Button(action: { withAnimation { currentIndex -= 1 } }) {
                            Image(systemName: "chevron.left")
                                .foregroundColor(.white)
                                .frame(width: 44, height: 44)
                                .background(Color.black.opacity(0.5))
                                .clipShape(Circle())
                        }
                    }
                    Spacer()
                    if currentIndex < totalCount - 1 {
                        Button(action: { withAnimation { currentIndex += 1 } }) {
                            Image(systemName: "chevron.right")
                                .foregroundColor(.white)
                                .frame(width: 44, height: 44)
                                .background(Color.black.opacity(0.5))
                                .clipShape(Circle())
                        }
                    }
                }
                .padding(.horizontal, 16)
            }

            // Page Indicator
            if totalCount > 1 {
                VStack {
                    Spacer()
                    Text("\(currentIndex + 1) / \(totalCount)")
                        .font(IssueSpotTypography.bodyMedium)
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.black.opacity(0.6))
                        .cornerRadius(16)
                        .padding(.bottom, 32)
                }
            }
        }
    }
}

// MARK: - Zoomable Image View
struct ZoomableImageView: View {
    let url: URL
    @Binding var currentScale: CGFloat

    @State private var scale: CGFloat = 1.0
    @State private var offset: CGSize = .zero

    var body: some View {
        AsyncImage(url: url) { image in
            image.resizable().scaledToFit()
        } placeholder: {
            ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white))
        }
        .scaleEffect(scale)
        .offset(offset)
        .gesture(
            MagnificationGesture()
            .onChanged { value in
                let newScale = min(max(value.magnitude, 1.0), 4.0)
                scale = newScale
                currentScale = newScale
            }
            .onEnded { _ in
                if scale == 1.0 { offset = .zero }
            }
            .simultaneously(with: DragGesture()
                .onChanged { value in
                    if scale > 1.0 { offset = value.translation }
                }
            )
        )
    }
}

// MARK: - Zoomable PDF View
struct ZoomablePdfView: View {
    let url: URL
    @Binding var currentScale: CGFloat

    @State private var scale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var currentPage: Int = 0
    @State private var totalPages: Int = 0

    var body: some View {
        ZStack {
            PDFKitView(url: url, currentPage: $currentPage, totalPages: $totalPages)
                .scaleEffect(scale)
                .offset(offset)
                .gesture(
                    MagnificationGesture()
                    .onChanged { value in
                        let newScale = min(max(value.magnitude, 1.0), 4.0)
                        scale = newScale
                        currentScale = newScale
                    }
                    .onEnded { _ in
                        if scale == 1.0 { offset = .zero }
                    }
                    .simultaneously(with: DragGesture()
                        .onChanged { value in
                            if scale > 1.0 { offset = value.translation }
                        }
                    )
                )
                
            if scale == 1.0 && totalPages > 1 {
                OverlayNavigationControls(
                    currentIndex: $currentPage,
                    totalCount: totalPages
                )
            }
        }
    }
}

// MARK: - Full Screen Video Player
struct FullScreenVideoPlayer: View {
    let url: URL
    @State private var player: AVPlayer?
    @State private var isMuted = false

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black

            if let player = player {
                // Reusing the CustomVideoPlayer from MediaPreviewContent
                CustomVideoPlayer(player: player, videoGravity: .resizeAspect)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .edgesIgnoringSafeArea(.all)
            }

            // Custom Mute Button (Top Right)
            Button(action: {
                isMuted.toggle()
                player?.isMuted = isMuted
            }) {
                Image(systemName: isMuted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                    .foregroundColor(.white)
                    .frame(width: 36, height: 36)
                    .background(Color.black.opacity(0.5))
                    .clipShape(Circle())
            }
            .padding(.trailing, 6)
            .padding(.top, 48) // Account for status bar
        }
        .onAppear {
            let avPlayer = AVPlayer(url: url)
            avPlayer.play()
            self.player = avPlayer
        }
        .onDisappear {
            player?.pause()
        }
    }
}
import PDFKit

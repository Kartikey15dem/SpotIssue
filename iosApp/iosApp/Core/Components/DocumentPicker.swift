import SwiftUI
import UniformTypeIdentifiers
import UIKit

// MARK: - Document Picker
struct DocumentPicker: UIViewControllerRepresentable {
    @Binding var isPresented: Bool
    let onDocumentPicked: (URL) -> Void
    
    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.pdf])
        picker.delegate = context.coordinator
        picker.allowsMultipleSelection = false
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, UIDocumentPickerDelegate {
        let parent: DocumentPicker
        
        init(_ parent: DocumentPicker) {
            self.parent = parent
        }
        
        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            parent.isPresented = false
            guard let url = urls.first else { return }
            
            // Create a secure temporary copy
            let tempDir = FileManager.default.temporaryDirectory
            let tempUrl = tempDir.appendingPathComponent(UUID().uuidString + "_" + url.lastPathComponent)
            
            do {
                if url.startAccessingSecurityScopedResource() {
                    try FileManager.default.copyItem(at: url, to: tempUrl)
                    url.stopAccessingSecurityScopedResource()
                    parent.onDocumentPicked(tempUrl)
                }
            } catch {
                print("Error copying document: \(error)")
            }
        }
        
        func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
            parent.isPresented = false
        }
    }
}

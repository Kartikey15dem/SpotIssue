import SwiftUI
import Shared

struct RootView: View {
    @StateObject private var router = Router()
    @StateObject private var mediaOverlayController = MediaOverlayController()
    let prefRepo = KoinHelper().getUserPreferencesRepository()
    
    var body: some View {
        Observing(prefRepo.userData) { userData in
            ZStack {
                NavigationStack(path: $router.path) {
                    Group {
                        if !userData.isLoggedIn {
                            AuthScreen()
                        } else {
                            LocationFetchScreen()
                        }
                    }
                    .navigationDestination(for: Route.self) { route in
                        switch route {
                        case .auth:
                            AuthScreen()
                        case .otp:
                            OtpScreen()
                        case .nameCapture(let email):
                            NameCaptureScreen(email: email)
                        case .locationFetch:
                            LocationFetchScreen()
                        case .home:
                            HomeScreen()
                        case .createPost:
                            CreatePostScreen()
                        case .profile:
                            ProfileScreen()
                        case .editProfile:
                            EditProfileScreen()
                        case .postDetail(let postId):
                            PostDetailScreen(postId: postId)
                        }
                    }
                }
                
                SnackbarHost()
            }
            .environmentObject(router)
            .environmentObject(mediaOverlayController)
            .fullScreenCover(isPresented: $mediaOverlayController.isShowing) {
                MediaOverlayView()
                    .environmentObject(mediaOverlayController)
            }
            .onChange(of: userData.isLoggedIn, perform: { isLoggedIn in
                if isLoggedIn {
                    router.popToRoot()
                }
            })
        }
    }
}

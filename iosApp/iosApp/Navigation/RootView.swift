import SwiftUI
import Shared

struct RootView: View {
    @StateObject private var mediaOverlayController = MediaOverlayController()
    let prefRepo = KoinHelper().getUserPreferencesRepository()
    
    var body: some View {
        Observing(prefRepo.userData) { userData in
            ZStack {
                Group {
                    if !userData.isLoggedIn {
                        AuthCoordinator()
                    } else {
                        MainCoordinator()
                    }
                }
                
                AppDialogHost()
            }
            .environmentObject(mediaOverlayController)
            .fullScreenCover(isPresented: $mediaOverlayController.isShowing) {
                MediaOverlayView()
                    .environmentObject(mediaOverlayController)
            }
        }
    }
}

struct AuthCoordinator: View {
    @StateObject private var router = AuthRouter()
    var body: some View {
        NavigationStack(path: $router.path) {
            AuthScreen()
                .navigationDestination(for: AuthRoute.self) { route in
                    switch route {
                    case .auth:
                        AuthScreen()
                    case .otp:
                        OtpScreen()
                    case .nameCapture(let email):
                        NameCaptureScreen(email: email)
                    }
                }
        }
        .environmentObject(router)
    }
}

struct MainCoordinator: View {
    @StateObject private var router = MainRouter()
    var body: some View {
        NavigationStack(path: $router.path) {
            LocationFetchScreen()
                .navigationDestination(for: MainRoute.self) { route in
                    switch route {
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
        .environmentObject(router)
    }
}

import SwiftUI
import Shared

struct RootView: View {
    @StateObject private var mediaOverlayController = MediaOverlayController()
    let prefRepo = KoinHelper().getUserPreferencesRepository()
    
    @State private var showSplash = true
    
    var body: some View {
        ZStack {
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
            
            if showSplash {
                SplashScreen()
                    .transition(.opacity)
                    .zIndex(1)
            }
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                withAnimation(.easeInOut(duration: 0.4)) {
                    showSplash = false
                }
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
                    }
                }
        }
        .environmentObject(router)
    }
}

struct SplashScreen: View {
    var body: some View {
        ZStack {
            IssueSpotColors.surface.ignoresSafeArea()
            
            VStack {
                Image("logo_issue")
                    .resizable()
                    .scaledToFill()
                    .frame(width: 160, height: 160)
                    .clipShape(RoundedRectangle(cornerRadius: 24))
            }
        }
    }
}

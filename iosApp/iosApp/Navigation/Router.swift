import SwiftUI
import Shared

enum AuthRoute: Hashable {
    case auth
    case otp
    case nameCapture(String)
}

enum MainRoute: Hashable {
    case locationFetch
    case home
    case createPost
    case profile
    case editProfile
    case postDetail(String)
}

class AuthRouter: ObservableObject {
    @Published var path = NavigationPath()
    
    func navigate(to route: AuthRoute) { path.append(route) }
    func goBack() { if !path.isEmpty { path.removeLast() } }
    func popToRoot() { path.removeLast(path.count) }
    func clearAndNavigate(to route: AuthRoute) {
        path.removeLast(path.count)
        path.append(route)
    }
}

class MainRouter: ObservableObject {
    @Published var path = NavigationPath()
    
    func navigate(to route: MainRoute) { path.append(route) }
    func goBack() { if !path.isEmpty { path.removeLast() } }
    func popToRoot() { path.removeLast(path.count) }
    func clearAndNavigate(to route: MainRoute) {
        path.removeLast(path.count)
        path.append(route)
    }
}

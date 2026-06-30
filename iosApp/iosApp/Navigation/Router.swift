import SwiftUI
import Shared

enum Route: Hashable {
    case auth
    case otp
    case nameCapture(String)
    case locationFetch
    case home
    case createPost
    case profile
    case editProfile
    case postDetail(String)
}

class Router: ObservableObject {
    @Published var path = NavigationPath()
    
    func navigate(to route: Route) {
        path.append(route)
    }

    func goBack() {
        if !path.isEmpty {
            path.removeLast()
        }
    }

    func popToRoot() {
        path.removeLast(path.count)
    }
    
    func clearAndNavigate(to route: Route) {
        path.removeLast(path.count)
        path.append(route)
    }
}

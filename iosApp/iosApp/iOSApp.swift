import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
      koinInit()
    }
    var body: some Scene {
        WindowGroup {
            CounterScreen()
        }
    }
}

struct CounterScreen: View {

    let vm = KoinHelper().getCounterViewModel()

    var body: some View {

        Observing(vm.state) { state in

            VStack {

                Text("\(state.count)")

                Button("Increment") {
                    vm.increment()
                }
            }
        }
    }
}
import SwiftUI
import Shared

@main
struct iOSApp: App {

    init() {
        KoinIosKt.initKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
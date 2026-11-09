import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        let baseUrl = Bundle.main.object(forInfoDictionaryKey: "API_BASE_URL") as? String ?? ""
        #if DEBUG
        let isDebug = true
        #else
        let isDebug = false
        #endif
        PlatformModule_iosKt.startKoinIos(baseUrl: baseUrl, isDebug: isDebug)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in Platform_iosKt.handleDeepLink(url: url.absoluteString) }
        }
    }
}

import SwiftUI

@main
struct MorfoApp: App {
    @State private var app = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(app)
                .preferredColorScheme(.dark)
                .tint(MorfoColors.accent)
        }
    }
}

struct RootView: View {
    @Environment(AppModel.self) private var app

    var body: some View {
        @Bindable var app = app
        NavigationStack(path: $app.path) {
            SelectorScreen()
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .compare: CompareScreen(app: app)
                    case .quizHome: QuizHomeScreen()
                    case .quiz: QuizScreen(app: app)
                    case .quizStats: QuizStatsScreen()
                    case .about: AboutScreen()
                    }
                }
        }
    }
}

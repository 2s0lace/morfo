import Observation
import simd

/// One quiz run: shows a single tooth, takes a typed FDI number, grades it and moves on.
/// The tooth lives in its own `Workspace`, so the comparison screen is untouched.
@Observable
final class QuizSession {
    enum Phase { case answering, feedback, finished }

    let workspace: Workspace
    let progress: QuizProgress
    @ObservationIgnored private var flashcards: Flashcards
    @ObservationIgnored private var random: AnyRandomGenerator
    @ObservationIgnored private let onProgressChanged: () -> Void

    var language = AppLanguage.pl
    private(set) var settings = QuizSettings()
    private(set) var phase = Phase.answering
    private(set) var input = ""
    private(set) var inputError: String?
    private(set) var current: ToothRef?
    private(set) var lastGrade: Grade?

    /// Round mode: position and length. Flashcards: answers in this session.
    private(set) var index = 0
    private(set) var total = 0
    private(set) var streak = 0

    /// Grades of this session (round results / flashcard history).
    private(set) var results: [Grade] = []

    @ObservationIgnored private var queue: [Int] = []

    init(
        meta: [String: ModelMeta],
        progress: QuizProgress,
        random: some RandomNumberGenerator = SystemRandomNumberGenerator(),
        onProgressChanged: @escaping () -> Void = {}
    ) {
        workspace = Workspace(meta: meta)
        workspace.labelSpaceMm = 7
        self.progress = progress
        self.random = AnyRandomGenerator(random)
        flashcards = Flashcards(progress: progress, random: AnyRandomGenerator(random))
        self.onProgressChanged = onProgressChanged
    }

    var correctCount: Int { results.filter(\.correct).count }
    var scope: [Int] { settings.scope.teeth() }
    var isLastQuestion: Bool { settings.mode == .round && index + 1 >= total }

    func start(_ newSettings: QuizSettings, onlyTeeth: [Int]? = nil) {
        settings = newSettings
        results = []
        streak = 0
        index = 0
        switch newSettings.mode {
        case .round:
            let pool = onlyTeeth ?? newSettings.scope.teeth()
            let length = onlyTeeth != nil ? pool.count : newSettings.roundLength ?? pool.count
            queue = buildQueue(pool, length: length)
            total = queue.count
            guard let first = queue.first else { phase = .finished; return }
            show(first)
        case .flashcards:
            total = 0
            show(flashcards.next(scope: scope, previous: nil))
        }
    }

    func type(_ digit: Character) {
        guard phase == .answering, input.count < 2 else { return }
        input.append(digit)
        inputError = nil
    }

    func backspace() {
        guard phase == .answering else { return }
        input = String(input.dropLast())
        inputError = nil
    }

    func check() {
        guard phase == .answering, let shown = current?.tooth else { return }
        guard let answered = Grading.parse(input) else {
            let s = language.strings
            inputError = input.count < 2 ? s.enterTwoDigits : s.notPermanentFdi(input)
            return
        }
        let grade = Grading.grade(expected: shown, answered: answered, ignoreSide: settings.ignoreSide)
        lastGrade = grade
        results.append(grade)
        streak = grade.correct ? streak + 1 : 0
        progress.record(grade)
        if settings.mode == .flashcards { flashcards.answer(shown.fdi, correct: grade.correct) }
        onProgressChanged()
        phase = .feedback
        workspace.requestFraming() // the feedback panel is taller than the keypad
    }

    func next() {
        guard phase == .feedback else { return }
        index += 1
        switch settings.mode {
        case .round:
            if index >= queue.count {
                phase = .finished
                return
            }
            show(queue[index])
        case .flashcards:
            show(flashcards.next(scope: scope, previous: current?.fdi))
        }
    }

    /// Teeth answered wrongly in this session (for "repeat mistakes").
    func mistakes() -> [Int] {
        var seen = Set<Int>()
        return results.filter { !$0.correct }.map(\.expected.fdi).filter { seen.insert($0).inserted }
    }

    func flashcardSummary() -> Flashcards.Summary { flashcards.summary(scope: scope) }

    private func show(_ fdi: Int) {
        let tooth = Tooth(fdi)
        // First molars have two specimens (with / without Carabelli cusp); either may appear.
        let ref = ToothRef(fdi: fdi, variant: tooth.variants.randomElement(using: &random)!)
        workspace.setTeeth([])
        workspace.setTeeth([ref])
        if let placed = workspace.teeth.first {
            let rotation: simd_quatf = switch settings.difficulty {
            case .exam: .random(using: &random)
            case .easy: SurfacePreset.facial.orientation(tooth)
            }
            workspace.setRotation(placed.key, rotation)
        }
        workspace.requestFraming()
        current = ref
        input = ""
        inputError = nil
        lastGrade = nil
        phase = .answering
    }

    /// Shuffled teeth, cycling the pool if the round is longer, without the same tooth twice in a row.
    private func buildQueue(_ pool: [Int], length: Int) -> [Int] {
        guard !pool.isEmpty else { return [] }
        var out: [Int] = []
        while out.count < length {
            var batch = pool.shuffled(using: &random)
            if let last = out.last, batch.count > 1, batch.first == last { batch.append(batch.removeFirst()) }
            out += batch
        }
        return Array(out.prefix(length))
    }
}

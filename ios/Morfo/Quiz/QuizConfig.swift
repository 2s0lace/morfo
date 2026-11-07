enum QuizMode: String, CaseIterable, Identifiable {
    case round, flashcards

    var id: Self { self }
    func label(_ lang: AppLanguage) -> String {
        switch self {
        case .round: lang == .en ? "Round" : "Runda"
        case .flashcards: lang == .en ? "Flashcards" : "Fiszki"
        }
    }

    func description(_ lang: AppLanguage) -> String {
        switch self {
        case .round: lang == .en
            ? "Fixed number of questions, score and mistake list at the end"
            : "Określona liczba pytań, na końcu wynik i lista błędów"
        case .flashcards: lang == .en
            ? "Endless. Teeth you confuse return more often, mastered teeth less often (spaced repetition)"
            : "Bez końca. Zęby, które mylisz, wracają częściej, a opanowane coraz rzadziej (jak w Anki)"
        }
    }
}

enum Difficulty: String, CaseIterable, Identifiable {
    case exam, easy

    var id: Self { self }
    func label(_ lang: AppLanguage) -> String {
        switch self {
        case .exam: lang == .en ? "Exam" : "Egzamin"
        case .easy: lang == .en ? "Easy" : "Łatwy"
        }
    }

    func description(_ lang: AppLanguage) -> String {
        switch self {
        case .exam: lang == .en
            ? "Random orientation, determine arch, side and tooth yourself"
            : "Losowa orientacja, trzeba samemu ustalić łuk, stronę i ząb"
        case .easy: lang == .en
            ? "Anatomical position, facial surface facing you"
            : "Pozycja anatomiczna, powierzchnią przedsionkową do Ciebie"
        }
    }
}

/// Which teeth can appear.
enum QuizScope: String, CaseIterable, Identifiable {
    case all, maxillary, mandibular, incisors, canines, premolars, molars

    var id: Self { self }
    func label(_ lang: AppLanguage) -> String {
        switch self {
        case .all: lang == .en ? "All" : "Wszystkie"
        case .maxillary: Arch.maxillary.label(lang)
        case .mandibular: Arch.mandibular.label(lang)
        case .incisors: ToothClass.incisor.label(lang)
        case .canines: ToothClass.canine.label(lang)
        case .premolars: ToothClass.premolar.label(lang)
        case .molars: ToothClass.molar.label(lang)
        }
    }

    func teeth() -> [Int] {
        Dentition.permanent.filter {
            switch self {
            case .all: true
            case .maxillary: $0.arch == .maxillary
            case .mandibular: $0.arch == .mandibular
            case .incisors: $0.toothClass == .incisor
            case .canines: $0.toothClass == .canine
            case .premolars: $0.toothClass == .premolar
            case .molars: $0.toothClass == .molar
            }
        }.map(\.fdi)
    }
}

struct QuizSettings: Equatable {
    var mode = QuizMode.round
    var scope = QuizScope.all
    var difficulty = Difficulty.exam
    /// Round length; nil = every tooth in scope once.
    var roundLength: Int? = 20
    /// Replace the photo texture with a plain enamel colour, so only the shape can be used.
    var uniformColor = false
    /// Accept the right tooth type (arch + position) even with the wrong side.
    var ignoreSide = false
}

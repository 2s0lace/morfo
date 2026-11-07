enum AppLanguage: String, CaseIterable, Identifiable {
    case pl, en

    var id: Self { self }
    var label: String { self == .en ? "English" : "Polski" }
    var strings: AppStrings { self == .en ? .en : .pl }
}

enum Arch: String, CaseIterable, Identifiable {
    case maxillary, mandibular

    var id: Self { self }
    func label(_ lang: AppLanguage) -> String {
        switch self {
        case .maxillary: lang == .en ? "Maxillary" : "Szczęka"
        case .mandibular: lang == .en ? "Mandibular" : "Żuchwa"
        }
    }
}

/// Patient's side.
enum Side: String, CaseIterable, Identifiable {
    case right, left

    var id: Self { self }
    func label(_ lang: AppLanguage) -> String {
        switch self {
        case .right: lang == .en ? "Right" : "Prawa"
        case .left: lang == .en ? "Left" : "Lewa"
        }
    }
}

enum ToothClass: String, CaseIterable, Identifiable {
    case incisor, canine, premolar, molar

    var id: Self { self }
    func label(_ lang: AppLanguage) -> String {
        switch self {
        case .incisor: lang == .en ? "Incisors" : "Siekacze"
        case .canine: lang == .en ? "Canines" : "Kły"
        case .premolar: lang == .en ? "Premolars" : "Przedtrzonowce"
        case .molar: lang == .en ? "Molars" : "Trzonowce"
        }
    }
}

/// Position within a quadrant (the second FDI digit).
enum ToothKind: Int, CaseIterable {
    case centralIncisor = 1, lateralIncisor, canine, firstPremolar, secondPremolar, firstMolar, secondMolar, thirdMolar

    var toothClass: ToothClass {
        switch self {
        case .centralIncisor, .lateralIncisor: .incisor
        case .canine: .canine
        case .firstPremolar, .secondPremolar: .premolar
        case .firstMolar, .secondMolar, .thirdMolar: .molar
        }
    }

    var isAnterior: Bool { toothClass == .incisor || toothClass == .canine }

    var nameEn: String {
        ["Central Incisor", "Lateral Incisor", "Canine", "First Premolar", "Second Premolar",
         "First Molar", "Second Molar", "Third Molar"][rawValue - 1]
    }

    var namePl: String {
        ["siekacz przyśrodkowy", "siekacz boczny", "kieł", "pierwszy przedtrzonowiec", "drugi przedtrzonowiec",
         "pierwszy trzonowiec", "drugi trzonowiec", "trzeci trzonowiec"][rawValue - 1]
    }
}

/// Geometry variant of a tooth. Only the maxillary first molar has an alternative specimen.
enum ModelVariant: String, CaseIterable, Comparable {
    case standard, carabelli

    var suffix: String { self == .carabelli ? "_carabelli" : "" }

    func label(_ lang: AppLanguage) -> String {
        switch self {
        case .standard: lang == .en ? "Standard" : "Klasyczny"
        case .carabelli: lang == .en ? "With cusp of Carabelli" : "Z guzkiem Carabellego"
        }
    }

    static func < (a: Self, b: Self) -> Bool { a == .standard && b == .carabelli }
}

/// A permanent tooth identified by its FDI number (11–48). Pure metadata, no geometry.
struct Tooth: Hashable, Identifiable, CustomStringConvertible {
    let fdi: Int

    init(_ fdi: Int) {
        precondition((1...4).contains(fdi / 10) && (1...8).contains(fdi % 10), "Invalid permanent FDI number \(fdi)")
        self.fdi = fdi
    }

    var id: Int { fdi }
    var description: String { String(fdi) }

    var quadrant: Int { fdi / 10 }
    var kind: ToothKind { ToothKind(rawValue: fdi % 10)! }
    var toothClass: ToothClass { kind.toothClass }
    var arch: Arch { quadrant <= 2 ? .maxillary : .mandibular }
    var side: Side { quadrant == 1 || quadrant == 4 ? .right : .left }

    /// Only left-side teeth have source scans; right-side teeth are rendered as their mirror image.
    var isMirrored: Bool { side == .right }
    var sourceFdi: Int {
        switch quadrant {
        case 1: fdi + 10 // 1x → 2x
        case 4: fdi - 10 // 4x → 3x
        default: fdi
        }
    }

    var nameEn: String { "\(arch.label(.en)) \(side.label(.en)) \(kind.nameEn)" }
    var namePl: String {
        "\(arch == .maxillary ? "Górny" : "Dolny") \(side == .right ? "prawy" : "lewy") \(kind.namePl)"
    }

    func name(_ lang: AppLanguage) -> String { lang == .en ? nameEn : namePl }

    var variants: [ModelVariant] {
        arch == .maxillary && kind == .firstMolar ? ModelVariant.allCases : [.standard]
    }

    /// Runtime asset id produced by tools/asset-pipeline, e.g. "t24" or "t26_carabelli".
    func modelId(_ variant: ModelVariant = .standard) -> String {
        "t\(sourceFdi)" + (variants.contains(variant) ? variant.suffix : "")
    }

    /// Average overall length (crown + root) in mm used to scale the model, per Wheeler's tables.
    var averageLengthMm: Float {
        let lengths: [Float] = arch == .maxillary
            ? [23.5, 22.0, 27.0, 22.5, 22.5, 20.5, 19.0, 17.5]
            : [21.5, 23.5, 27.0, 22.5, 22.5, 21.5, 20.0, 18.0]
        return lengths[kind.rawValue - 1]
    }
}

enum Dentition {
    /// All 32 permanent teeth in FDI order.
    static let permanent: [Tooth] = (1...4).flatMap { q in (1...8).map { Tooth(q * 10 + $0) } }

    /// Chart rows as seen by the clinician facing the patient: patient's right on the left.
    static let upperRow: [Tooth] = (11...18).reversed().map { Tooth($0) } + (21...28).map { Tooth($0) }
    static let lowerRow: [Tooth] = (41...48).reversed().map { Tooth($0) } + (31...38).map { Tooth($0) }
}

/// One tooth in a comparison set: FDI number plus geometry variant.
struct ToothRef: Hashable {
    let fdi: Int
    var variant: ModelVariant = .standard

    var tooth: Tooth { Tooth(fdi) }
}

struct QuickComparison: Identifiable {
    let titlePl: String
    let titleEn: String
    let teeth: [ToothRef]

    var id: String { titleEn }
    func title(_ lang: AppLanguage) -> String { lang == .en ? titleEn : titlePl }
}

/// Shortcuts only; the user can always build any combination manually.
enum QuickComparisons {
    private static func of(_ pl: String, _ en: String, _ fdi: Int...) -> QuickComparison {
        QuickComparison(titlePl: pl, titleEn: en, teeth: fdi.map { ToothRef(fdi: $0) })
    }

    static let all: [QuickComparison] = [
        of("Górny siekacz przyśrodkowy vs boczny", "Maxillary central vs lateral incisor", 11, 12),
        of("Górny 1. vs 2. przedtrzonowiec", "Maxillary 1st vs 2nd premolar", 14, 15),
        of("Dolny 1. vs 2. przedtrzonowiec", "Mandibular 1st vs 2nd premolar", 44, 45),
        of("Górny 1. vs 2. trzonowiec", "Maxillary 1st vs 2nd molar", 16, 17),
        of("Dolny 1. vs 2. trzonowiec", "Mandibular 1st vs 2nd molar", 46, 47),
        of("Kieł górny vs dolny", "Maxillary vs mandibular canine", 13, 43),
        of("Dolny siekacz przyśrodkowy vs boczny", "Mandibular central vs lateral incisor", 41, 42),
        of("Przedtrzonowce górne vs dolne", "Maxillary vs mandibular premolars", 14, 15, 44, 45),
        of("Zęby przeciwstronne (14 vs 24)", "Contralateral teeth (14 vs 24)", 14, 24),
        QuickComparison(
            titlePl: "Górny 1. trzonowiec: klasyczny vs z guzkiem Carabellego",
            titleEn: "Maxillary 1st molar: standard vs with Carabelli's cusp",
            teeth: [ToothRef(fdi: 16), ToothRef(fdi: 16, variant: .carabelli)]
        ),
    ]
}

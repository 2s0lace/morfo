enum LabelMode: String, CaseIterable, Identifiable {
    case fdi, fdiEn, fdiPl, none

    var id: Self { self }
    func label(_ lang: AppLanguage) -> String {
        switch self {
        case .fdi: lang == .en ? "FDI only" : "Tylko FDI"
        case .fdiEn: lang == .en ? "FDI + name" : "FDI + nazwa angielska"
        case .fdiPl: lang == .en ? "FDI + Polish name" : "FDI + nazwa polska"
        case .none: lang == .en ? "No labels" : "Bez etykiet"
        }
    }
}

enum LightingMode: String, CaseIterable, Identifiable {
    case clinical, highContrast, soft, morphology

    var id: Self { self }
    func label(_ lang: AppLanguage) -> String {
        switch self {
        case .clinical: lang == .en ? "Clinical" : "Kliniczne"
        case .highContrast: lang == .en ? "High Contrast" : "Wysoki kontrast"
        case .soft: lang == .en ? "Soft" : "Miękkie"
        case .morphology: lang == .en ? "Morphology" : "Morfologia"
        }
    }

    func description(_ lang: AppLanguage) -> String {
        switch self {
        case .clinical: lang == .en ? "Balanced lighting, clear morphology" : "Zrównoważone światło, czytelna morfologia"
        case .highContrast: lang == .en ? "Strong side lighting, deeper shadows" : "Mocne światło boczne, głębsze cienie"
        case .soft: lang == .en ? "Diffuse lighting, gentle transitions" : "Rozproszone światło, łagodne przejścia"
        case .morphology: lang == .en
            ? "Oblique lighting and contact shadows – enhances fissures and cusps"
            : "Światło skośne i kontaktowe cienie – uwydatnia bruzdy i guzki"
        }
    }
}

enum Arrangement: String, CaseIterable, Identifiable {
    case auto, row, grid, triangle

    var id: Self { self }
    func label(_ lang: AppLanguage) -> String {
        switch self {
        case .auto: lang == .en ? "Auto" : "Automatyczny"
        case .row: lang == .en ? "Row" : "Rząd"
        case .grid: lang == .en ? "Grid" : "Siatka"
        case .triangle: lang == .en ? "Triangle" : "Trójkąt"
        }
    }
}

enum ScaleMode: String, CaseIterable, Identifiable {
    case anatomical, uniform

    var id: Self { self }
    func label(_ lang: AppLanguage) -> String {
        switch self {
        case .anatomical: lang == .en ? "Anatomical" : "Anatomiczna"
        case .uniform: lang == .en ? "Uniform" : "Jednakowa"
        }
    }

    func description(_ lang: AppLanguage) -> String {
        switch self {
        case .anatomical: lang == .en
            ? "Real proportions between teeth (average lengths)"
            : "Realne proporcje między zębami (średnie długości)"
        case .uniform: lang == .en
            ? "Every tooth scaled to equal height – for inspecting details"
            : "Każdy ząb tej samej wysokości – do oglądania szczegółów"
        }
    }
}

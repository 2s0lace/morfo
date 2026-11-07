import simd

/// Standard views of a tooth. The camera always looks along -Z, so a preset is simply the tooth
/// rotation that turns the requested surface towards the viewer.
///
/// Model frame (see tools/asset-pipeline): +Y superior, +Z facial, left teeth have mesial at -X.
/// Teeth stay in anatomical position (maxillary crowns down, mandibular crowns up), as in textbooks.
enum SurfacePreset: CaseIterable, Identifiable {
    case facial, lingual, mesial, distal, occlusal

    var id: Self { self }

    func orientation(_ tooth: Tooth) -> simd_quatf {
        let left: simd_quatf = switch self {
        case .facial: .identity
        case .lingual: .degrees([0, 1, 0], 180)
        case .mesial: .degrees([0, 1, 0], 90)
        case .distal: .degrees([0, 1, 0], -90)
        // Crown towards the viewer, facial surface at the bottom of the screen.
        case .occlusal: tooth.arch == .mandibular
            ? .degrees([1, 0, 0], 90)
            : .degrees([0, 0, 1], 180) * .degrees([1, 0, 0], -90)
        }
        return tooth.isMirrored ? left.mirroredX : left
    }

    /// Polish term, adjusted to the teeth it applies to.
    func labelPl(_ teeth: [Tooth]) -> String {
        let anterior = !teeth.isEmpty && teeth.allSatisfy { $0.kind.isAnterior }
        let posterior = !teeth.isEmpty && teeth.allSatisfy { !$0.kind.isAnterior }
        let upper = !teeth.isEmpty && teeth.allSatisfy { $0.arch == .maxillary }
        let lower = !teeth.isEmpty && teeth.allSatisfy { $0.arch == .mandibular }
        switch self {
        case .facial: return anterior ? "Wargowa" : posterior ? "Policzkowa" : "Przedsionkowa"
        case .lingual: return upper ? "Podniebienna" : lower ? "Językowa" : "Jęz./podn."
        case .mesial: return "Mezjalna"
        case .distal: return "Dystalna"
        case .occlusal: return anterior ? "Sieczna" : posterior ? "Żująca" : "Żująca/sieczna"
        }
    }

    /// English term in capitals, shown under the Polish one.
    func labelEn(_ teeth: [Tooth]) -> String {
        let anterior = !teeth.isEmpty && teeth.allSatisfy { $0.kind.isAnterior }
        let posterior = !teeth.isEmpty && teeth.allSatisfy { !$0.kind.isAnterior }
        let upper = !teeth.isEmpty && teeth.allSatisfy { $0.arch == .maxillary }
        let lower = !teeth.isEmpty && teeth.allSatisfy { $0.arch == .mandibular }
        switch self {
        case .facial: return anterior ? "LABIAL" : posterior ? "BUCCAL" : "FACIAL"
        case .lingual: return upper ? "PALATAL" : lower ? "LINGUAL" : "LINGUAL"
        case .mesial: return "MESIAL"
        case .distal: return "DISTAL"
        case .occlusal: return anterior ? "INCISAL" : posterior ? "OCCLUSAL" : "OCCL./INC."
        }
    }

    func labelEnTitle(_ teeth: [Tooth]) -> String {
        let anterior = !teeth.isEmpty && teeth.allSatisfy { $0.kind.isAnterior }
        let posterior = !teeth.isEmpty && teeth.allSatisfy { !$0.kind.isAnterior }
        let upper = !teeth.isEmpty && teeth.allSatisfy { $0.arch == .maxillary }
        let lower = !teeth.isEmpty && teeth.allSatisfy { $0.arch == .mandibular }
        switch self {
        case .facial: return anterior ? "Labial" : posterior ? "Buccal" : "Facial"
        case .lingual: return upper ? "Palatal" : lower ? "Lingual" : "Lingual/palatal"
        case .mesial: return "Mesial"
        case .distal: return "Distal"
        case .occlusal: return anterior ? "Incisal" : posterior ? "Occlusal" : "Occlusal/incisal"
        }
    }

    func label(_ teeth: [Tooth], _ lang: AppLanguage) -> String {
        lang == .en ? labelEnTitle(teeth) : labelPl(teeth)
    }
}

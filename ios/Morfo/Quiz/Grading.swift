/// Result of comparing a typed FDI number with the tooth shown.
struct Grade: Equatable {
    let expected: Tooth
    let answered: Tooth
    let archCorrect: Bool
    let sideCorrect: Bool
    let kindCorrect: Bool
    let correct: Bool

    /// One-line explanation in Polish, pointing at what exactly was wrong.
    var summaryPl: String {
        if correct && answered == expected { return "Dobrze!" }
        if correct { return "Dobry typ zęba (strona pominięta w tym trybie)" }
        if archCorrect && kindCorrect { return "Dobry typ zęba, ale zła strona" }
        if sideCorrect && kindCorrect { return "Dobry typ i strona, ale zły łuk" }
        if archCorrect && sideCorrect { return "Dobry łuk i strona, ale inny ząb" }
        if kindCorrect { return "Dobry ząb w kwadrancie, ale zły łuk i strona" }
        if archCorrect { return "Dobry łuk, ale inny ząb i strona" }
        if sideCorrect { return "Dobra strona, ale inny łuk i ząb" }
        return "Niepoprawnie"
    }

    var summaryEn: String {
        if correct && answered == expected { return "Correct!" }
        if correct { return "Correct tooth type (side ignored in this mode)" }
        if archCorrect && kindCorrect { return "Correct tooth type, but wrong side" }
        if sideCorrect && kindCorrect { return "Correct type and side, but wrong arch" }
        if archCorrect && sideCorrect { return "Correct arch and side, but wrong tooth" }
        if kindCorrect { return "Correct tooth in quadrant, but wrong arch and side" }
        if archCorrect { return "Correct arch, but wrong tooth and side" }
        if sideCorrect { return "Correct side, but wrong arch and tooth" }
        return "Incorrect"
    }

    func summary(_ lang: AppLanguage) -> String { lang == .en ? summaryEn : summaryPl }
}

enum Grading {
    /// Parses exactly two digits into a permanent FDI number, or nil if it is not one (e.g. "19", "50").
    static func parse(_ input: String) -> Tooth? {
        let digits = input.compactMap(\.wholeNumberValue)
        guard input.count == 2, digits.count == 2, input.allSatisfy(\.isASCII) else { return nil }
        let (q, p) = (digits[0], digits[1])
        return (1...4).contains(q) && (1...8).contains(p) ? Tooth(q * 10 + p) : nil
    }

    static func grade(expected: Tooth, answered: Tooth, ignoreSide: Bool) -> Grade {
        let arch = expected.arch == answered.arch
        let side = expected.side == answered.side
        let kind = expected.kind == answered.kind
        return Grade(
            expected: expected, answered: answered,
            archCorrect: arch, sideCorrect: side, kindCorrect: kind,
            correct: arch && kind && (side || ignoreSide)
        )
    }
}

import Foundation
import Testing
import simd
@testable import Morfo

/// Same cases as app/src/test/java/app/morfo/quiz/QuizLogicTest.kt, plus iOS-specific checks.
@MainActor
struct QuizLogicTests {

    @Test func parseAcceptsOnlyPermanentFdiNumbers() {
        #expect(Grading.parse("14")?.fdi == 14)
        #expect(Grading.parse("48")?.fdi == 48)
        #expect(Grading.parse("19") == nil)
        #expect(Grading.parse("50") == nil)
        #expect(Grading.parse("4") == nil)
        #expect(Grading.parse("144") == nil)
        #expect(Grading.parse("1a") == nil)
    }

    @Test func gradingExplainsWhichPartIsWrong() {
        let wrongSide = Grading.grade(expected: Tooth(24), answered: Tooth(14), ignoreSide: false)
        #expect(!wrongSide.correct)
        #expect(wrongSide.archCorrect && wrongSide.kindCorrect && !wrongSide.sideCorrect)
        #expect(wrongSide.summaryPl == "Dobry typ zęba, ale zła strona")

        let wrongArch = Grading.grade(expected: Tooth(34), answered: Tooth(24), ignoreSide: false)
        #expect(!wrongArch.archCorrect && wrongArch.sideCorrect && wrongArch.kindCorrect)

        let wrongKind = Grading.grade(expected: Tooth(15), answered: Tooth(14), ignoreSide: false)
        #expect(wrongKind.archCorrect && wrongKind.sideCorrect && !wrongKind.kindCorrect)

        #expect(Grading.grade(expected: Tooth(24), answered: Tooth(14), ignoreSide: true).correct)
        #expect(!Grading.grade(expected: Tooth(24), answered: Tooth(34), ignoreSide: true).correct)
    }

    @Test func progressCountsErrorsAndConfusions() {
        let p = QuizProgress()
        p.record(Grading.grade(expected: Tooth(24), answered: Tooth(14), ignoreSide: false))
        p.record(Grading.grade(expected: Tooth(24), answered: Tooth(24), ignoreSide: false))
        #expect(p.statsFor(24).attempts == 2)
        #expect(p.statsFor(24).correct == 1)
        #expect(p.sideErrors == 1)
        #expect(p.archErrors == 0)
        #expect(p.confusions[.init(expected: 24, answered: 14)] == 1)
    }

    @Test func flashcardsBringMistakesBackSoonAndMasteredLater() {
        let p = QuizProgress()
        var cards = Flashcards(progress: p, random: SeededGenerator(seed: 1))
        let scope = [11, 12, 13, 14, 15, 16]

        cards.answer(14, correct: false)
        let wrongDue = p.card(14).due
        cards.answer(15, correct: true)
        let rightDue = p.card(15).due
        #expect(wrongDue < rightDue, "a mistake returns sooner than a correct answer")

        // After enough other answers the missed tooth is due and gets picked before new ones catch up.
        for _ in 0..<5 { cards.answer(11, correct: true) }
        let picks = (1...3).map { _ in cards.next(scope: scope, previous: nil) }
        #expect(picks.contains(14) || p.card(14).due > p.step)

        // Repeated correct answers push the interval out.
        for _ in 0..<4 { cards.answer(16, correct: true) }
        #expect(p.card(16).due - p.step >= Flashcards.intervals[3])
        #expect(p.card(16).isMastered)
    }

    @Test func flashcardsAvoidImmediateRepeat() {
        let p = QuizProgress()
        var cards = Flashcards(progress: p, random: SeededGenerator(seed: 7))
        let scope = [21, 22]
        var prev: Int?
        for i in 0..<20 {
            let n = cards.next(scope: scope, previous: prev)
            if let prev { #expect(n != prev) }
            cards.answer(n, correct: i % 3 != 0)
            prev = n
        }
    }

    @Test func progressSurvivesSaveAndLoad() throws {
        let defaults = try #require(UserDefaults(suiteName: "morfo-tests-\(UUID().uuidString)"))
        let store = QuizStore(defaults: defaults)
        let p = QuizProgress()
        p.record(Grading.grade(expected: Tooth(36), answered: Tooth(46), ignoreSide: false))
        Flashcards(progress: p).answer(36, correct: false)
        store.saveProgress(p)

        let loaded = store.loadProgress()
        #expect(loaded.stats == p.stats)
        #expect(loaded.cards == p.cards)
        #expect(loaded.confusions == p.confusions)
        #expect(loaded.step == p.step)
        #expect(loaded.sideErrors == 1)

        var settings = QuizSettings()
        settings.mode = .flashcards
        settings.roundLength = nil
        settings.ignoreSide = true
        store.saveSettings(settings)
        #expect(store.loadSettings() == settings)
    }

    @Test func roundAsksEachToothAndFinishes() {
        let session = QuizSession(meta: [:], progress: QuizProgress(), random: SeededGenerator(seed: 3))
        var settings = QuizSettings()
        settings.scope = .canines
        settings.roundLength = nil
        session.start(settings)
        #expect(session.total == 4)

        var asked: [Int] = []
        while session.phase != .finished {
            let fdi = session.current!.fdi
            asked.append(fdi)
            for c in String(fdi) { session.type(c) }
            session.check()
            #expect(session.lastGrade?.correct == true)
            session.next()
        }
        #expect(Set(asked) == Set([13, 23, 33, 43]))
        #expect(session.correctCount == 4)
        #expect(session.mistakes().isEmpty)
    }

    @Test func invalidInputIsRejectedWithoutGrading() {
        let session = QuizSession(meta: [:], progress: QuizProgress(), random: SeededGenerator(seed: 5))
        session.start(QuizSettings())
        session.type("1")
        session.check()
        #expect(session.phase == .answering)
        #expect(session.inputError == AppStrings.pl.enterTwoDigits)
        session.type("9")
        session.check()
        #expect(session.inputError == AppStrings.pl.notPermanentFdi("19"))
        #expect(session.results.isEmpty)
    }
}

@MainActor
struct WorkspaceTests {

    @Test func mirrorConjugatesRotation() {
        let q = simd_quatf.degrees([0.3, 1, 0.2], 40)
        let mirror = simd_float3x3(diagonal: [-1, 1, 1])
        let v = SIMD3<Float>(0.2, -0.5, 0.9)
        // Mirror then rotate with the mirrored quaternion == rotate then mirror.
        let a = q.mirroredX.act(mirror * v)
        let b = mirror * q.act(v)
        #expect(simd_distance(a, b) < 1e-5)
    }

    @Test func presetsMatchForContralateralTeeth() {
        // 14 and 24 both show their mesial surface: the same rotation up to the mirror.
        let left = SurfacePreset.mesial.orientation(Tooth(24))
        let right = SurfacePreset.mesial.orientation(Tooth(14))
        #expect(simd_distance(right.vector, left.mirroredX.vector) < 1e-6)
        #expect(SurfacePreset.facial.labelPl([Tooth(11)]) == "Wargowa")
        #expect(SurfacePreset.facial.labelPl([Tooth(16)]) == "Policzkowa")
        #expect(SurfacePreset.lingual.labelPl([Tooth(16)]) == "Podniebienna")
    }

    @Test func autoLayoutNeverOverlaps() {
        let boxes = (0..<7).map { i in
            Box3(min: SIMD3(-Float(i) - 2, -10, -3), max: SIMD3(Float(i) + 1, 12, 4))
        }
        for arrangement in Arrangement.allCases {
            let positions = AutoLayout.arrange(boxes, arrangement: arrangement, gap: 2)
            let placed = zip(boxes, positions).map { $0.translated($1) }
            for i in placed.indices {
                for j in placed.indices where j > i {
                    let a = placed[i], b = placed[j]
                    let overlap = a.min.x < b.max.x && b.min.x < a.max.x && a.min.y < b.max.y && b.min.y < a.max.y
                    #expect(!overlap, "\(arrangement): \(i) overlaps \(j)")
                }
            }
        }
    }

    @Test func workspaceKeepsExistingTeethWhenSelectionChanges() {
        let ws = Workspace(meta: ModelMeta.loadAll(bundle: .main))
        ws.setTeeth([ToothRef(fdi: 24), ToothRef(fdi: 14)])
        #expect(ws.teeth.map(\.tooth.fdi) == [14, 24])
        let key24 = ws.teeth[1].key
        ws.setTeeth([ToothRef(fdi: 24), ToothRef(fdi: 16, variant: .carabelli)])
        #expect(ws.teeth.map(\.tooth.fdi) == [16, 24])
        #expect(ws.teeth[1].key == key24)
        #expect(ws.teeth[0].modelId == "t26_carabelli")
        #expect(ws.refs() == [ToothRef(fdi: 16, variant: .carabelli), ToothRef(fdi: 24)])
    }

    /// The test host is the app, so Bundle.main holds meta.json.
    @Test func modelMetadataCoversEveryTooth() {
        let meta = ModelMeta.loadAll(bundle: .main)
        for tooth in Dentition.permanent {
            for variant in tooth.variants {
                #expect(meta[tooth.modelId(variant)] != nil, "missing \(tooth.modelId(variant))")
            }
            #expect(!Morphology.keyFeatures(tooth, language: .pl).isEmpty)
            #expect(Morphology.keyFeatures(tooth, language: .pl).count == Morphology.keyFeatures(tooth, language: .en).count)
        }
    }

    @Test func pickingFindsTheToothUnderTheFinger() {
        let rig = CameraRig()
        rig.setViewport(CGSize(width: 400, height: 800))
        let box = Box3(min: [-5, -10, -4], max: [5, 10, 4])
        rig.frame(box, jump: true)
        let center = rig.project(box.center)!
        let ray = rig.ray(through: center)
        #expect(box.intersect(origin: ray.origin, direction: ray.direction) != nil)
        let miss = rig.ray(through: CGPoint(x: 5, y: 5))
        #expect(box.intersect(origin: miss.origin, direction: miss.direction) == nil)
    }
}


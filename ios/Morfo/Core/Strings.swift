// Generated from app/src/main/java/app/morfo/i18n/Strings.kt — keep both in sync.

struct AppStrings {
    // Common / Navigation
    let appName: String
    let back: String
    let close: String
    let about: String
    let settings: String
    let language: String
    let clear: String
    let cancel: String
    let reset: String
    let delete: String
    let compare: String
    let info: String
    let duplicate: String
    let hide: String
    let carabelliCusp: String
    let resetRotation: String
    let resetPosition: String
    let auto: String
    let autoLayout: String
    let syncShort: String
    let syncOn: String
    let syncOff: String
    let loadingModels: String

    // Selector
    let selectTeethSubtitle: String
    let quiz: String
    let filters: String
    let selectMatching: String
    let clearFilters: String
    let quickComparisons: String
    let noneSelected: String
    let compareCount: (Int) -> String
    let maxillaryArch: String
    let mandibularArch: String
    let rightSideShort: String
    let leftSideShort: String

    // View Settings
    let viewSettings: String
    let lighting: String
    let labels: String
    let rotation: String
    let layout: String
    let spacing: String
    let scale: String
    let controls: String
    let syncAllTeethTitle: String
    let syncAllTeethSubtitle: String
    let mirrorContralateralTitle: String
    let mirrorContralateralSubtitle: String
    let controlRotate: String
    let controlPanZoom: String
    let controlDragTooth: String
    let controlTap: String

    // Tooth Info Sheet
    let classLabel: String
    let archLabel: String
    let sideLabel: String
    let avgLengthLabel: String
    let crownPlusRoot: String
    let modelLabel: String
    let mirrorScanOf: (Int) -> String
    let scanOf: (Int) -> String
    let carabelliVariantTitle: String
    let carabelliVariantSubtitle: String
    let morphologyTitle: String
    let textbookSummaryFootnote: String

    // Quiz Home
    let quizSubtitle: String
    let stats: String
    let mode: String
    let questionCount: String
    let fullScope: (Int) -> String
    let yourFlashcards: (String) -> String
    let newCards: String
    let dueCards: String
    let masteredCards: String
    let scope: String
    let difficulty: String
    let options: String
    let uniformColorTitle: String
    let uniformColorSubtitle: String
    let ignoreSideTitle: String
    let ignoreSideSubtitle: String
    let startRound: String
    let startFlashcards: String

    // Quiz Screen
    let questionProgress: (Int, Int, Int) -> String
    let flashcardsProgress: (Int, Int) -> String
    let quizGestureHint: String
    let checkAnswer: String
    let enterTwoDigits: String
    let notPermanentFdi: (String) -> String
    let checkArch: String
    let checkSide: String
    let checkTooth: String
    let thisIs: String
    let entered: String
    let next: String
    let results: String

    // Quiz Results
    let roundResults: String
    let correctPercentage: (Int) -> String
    let mistakes: String
    let noMistakes: String
    let enteredMistake: (Int, String) -> String
    let repeatMistakes: (Int) -> String
    let newRound: String

    // Quiz Stats
    let confirmResetTitle: String
    let confirmResetMessage: String
    let emptyStats: String
    let answersLabel: String
    let correctLabel: String
    let masteredLabel: String
    let accuracyOnTeeth: String
    let accuracyForTooth: (Int, String, Int, Int) -> String
    let errorTypes: String
    let wrongArch: String
    let wrongSide: String
    let wrongTooth: String
    let errorPercent: (Int, Int) -> String
    let mostFrequentConfusions: String
    let noConfusions: String
    let shownEntered: (String, Int) -> String

    // About Screen
    let aboutTitle: String
    let modelsSection: String
    let modelsBody1: String
    let modelsBody2: String
    let prepSection: String
    let prep1: String
    let prep2: String
    let prep3: String
    let prep4: String
    let prep5: String
    let prep6: String
    let engineSection: String
    let engineBody: String
}

extension AppStrings {
    static let pl = AppStrings(
        // Common / Navigation
        appName: "Morfo",
        back: "Wstecz",
        close: "Zamknij",
        about: "O aplikacji",
        settings: "Ustawienia",
        language: "Język",
        clear: "Wyczyść",
        cancel: "Anuluj",
        reset: "Wyzeruj",
        delete: "Usuń",
        compare: "Porównaj",
        info: "Informacje",
        duplicate: "Duplikuj",
        hide: "Ukryj",
        carabelliCusp: "Guzek Carabellego",
        resetRotation: "Reset obrotu",
        resetPosition: "Reset pozycji",
        auto: "Auto",
        autoLayout: "Auto-układ",
        syncShort: "Sync",
        syncOn: "Synchronizacja: wł.",
        syncOff: "Synchronizacja: wył.",
        loadingModels: "Wczytywanie modeli",
        // Selector
        selectTeethSubtitle: "Wybierz zęby do porównania",
        quiz: "Quiz",
        filters: "Filtry",
        selectMatching: "Zaznacz pasujące",
        clearFilters: "Wyczyść filtry",
        quickComparisons: "Szybkie porównania",
        noneSelected: "Nic nie wybrano",
        compareCount: { n in "Porównaj (\(n))" },
        maxillaryArch: "SZCZĘKA",
        mandibularArch: "ŻUCHWA",
        rightSideShort: "P",
        leftSideShort: "L",
        // View Settings
        viewSettings: "Ustawienia widoku",
        lighting: "Oświetlenie",
        labels: "Etykiety",
        rotation: "Obrót",
        layout: "Układ",
        spacing: "Odstęp",
        scale: "Skala",
        controls: "Sterowanie",
        syncAllTeethTitle: "Synchronizuj obrót wszystkich zębów",
        syncAllTeethSubtitle: "Gesty i widoki działają na wszystkie zęby naraz",
        mirrorContralateralTitle: "Lustrzany obrót strony przeciwnej",
        mirrorContralateralSubtitle: "Np. 14 i 24 obracają się symetrycznie, więc oba pokazują tę samą powierzchnię",
        controlRotate: "Jeden palec – obrót",
        controlPanZoom: "Dwa palce – przesuwanie widoku i przybliżanie; skręt dwoma palcami obraca wokół osi widoku",
        controlDragTooth: "Przytrzymaj ząb i przeciągnij – przesuwanie zęba",
        controlTap: "Stuknięcie – zaznaczenie zęba; podwójne stuknięcie – przybliżenie / pokaż wszystkie",
        // Tooth Info Sheet
        classLabel: "Grupa",
        archLabel: "Łuk",
        sideLabel: "Strona",
        avgLengthLabel: "Średnia długość",
        crownPlusRoot: "mm (korona + korzeń)",
        modelLabel: "Model",
        mirrorScanOf: { fdi in "lustrzane odbicie skanu zęba \(fdi)" },
        scanOf: { fdi in "skan zęba \(fdi)" },
        carabelliVariantTitle: "Wariant z guzkiem Carabellego",
        carabelliVariantSubtitle: "Osobny okaz; guzek Carabellego to cecha zmienna",
        morphologyTitle: "Kluczowe cechy morfologiczne",
        textbookSummaryFootnote: "Streszczenie podręcznikowe (Wheeler's); cechy opisują typowy ząb – konkretny okaz może się różnić.",
        // Quiz Home
        quizSubtitle: "Rozpoznawanie zębów: pojawia się ząb, a Ty wpisujesz jego numer FDI. Bez odpowiedzi do wyboru.",
        stats: "Statystyki",
        mode: "Tryb",
        questionCount: "Liczba pytań",
        fullScope: { n in "Cały zakres (\(n))" },
        yourFlashcards: { sc in "Twoje fiszki (\(sc))" },
        newCards: "Nowe",
        dueCards: "Do powtórki",
        masteredCards: "Opanowane",
        scope: "Zakres",
        difficulty: "Poziom",
        options: "Opcje",
        uniformColorTitle: "Jednolity kolor",
        uniformColorSubtitle: "Bez tekstury skanu: rozpoznajesz tylko po kształcie, nie po plamkach konkretnego okazu",
        ignoreSideTitle: "Trening bez strony",
        ignoreSideSubtitle: "Wystarczy dobry łuk i typ zęba (np. górny pierwszy przedtrzonowiec); strona nie jest oceniana",
        startRound: "Rozpocznij rundę",
        startFlashcards: "Rozpocznij fiszki",
        // Quiz Screen
        questionProgress: { i, total, corr in "Pytanie \(i)/\(total) · ✓ \(corr)" },
        flashcardsProgress: { n, streak in "Fiszki · \(n) odp. · seria \(streak)" },
        quizGestureHint: "Obracaj ząb jednym palcem, przybliżaj dwoma.",
        checkAnswer: "Sprawdź",
        enterTwoDigits: "Wpisz dwie cyfry",
        notPermanentFdi: { inp in "\(inp) to nie jest numer zęba stałego (11–48)" },
        checkArch: "Łuk",
        checkSide: "Strona",
        checkTooth: "Ząb",
        thisIs: "To jest:",
        entered: "Wpisano:",
        next: "Dalej",
        results: "Wynik",
        // Quiz Results
        roundResults: "Wynik rundy",
        correctPercentage: { pct in "\(pct)% poprawnych odpowiedzi" },
        mistakes: "Błędy",
        noMistakes: "Bez błędów. 🎉",
        enteredMistake: { fdi, sum in "wpisano \(fdi) · \(sum)" },
        repeatMistakes: { n in "Powtórz błędne (\(n))" },
        newRound: "Nowa runda",
        // Quiz Stats
        confirmResetTitle: "Wyzerować statystyki?",
        confirmResetMessage: "Zostaną usunięte wszystkie wyniki i postęp fiszek. Tego nie da się cofnąć.",
        emptyStats: "Brak odpowiedzi. Rozwiąż rundę albo fiszki, a tu pojawią się wyniki.",
        answersLabel: "Odpowiedzi",
        correctLabel: "Poprawne",
        masteredLabel: "Opanowane",
        accuracyOnTeeth: "Skuteczność na zębach",
        accuracyForTooth: { fdi, name, corr, att in "\(fdi) · \(name): \(corr)/\(att) poprawnie" },
        errorTypes: "Rodzaj błędów",
        wrongArch: "Zły łuk (szczęka / żuchwa)",
        wrongSide: "Zła strona (prawa / lewa)",
        wrongTooth: "Zły ząb (typ)",
        errorPercent: { count, pct in "\(count) (\(pct)% błędów)" },
        mostFrequentConfusions: "Najczęstsze pomyłki",
        noConfusions: "Brak pomyłek.",
        shownEntered: { name, ans in "pokazany \(name), wpisany \(ans)" },
        // About Screen
        aboutTitle: "O aplikacji",
        modelsSection: "Modele 3D",
        modelsBody1: "Modele zębów: University of Dundee, School of Dentistry (sketchfab.com/DundeeDental), licencja Creative Commons Attribution 4.0 (CC BY 4.0).",
        modelsBody2: "Wykorzystane modele: Maxillary Central / Lateral Incisor, Canine, First / Second Premolar, First Molar, First Molar with Cusp of Carabelli, Second / Third Molar; Mandibular Central / Lateral Incisor, Canine, First / Second Premolar, First / Second / Third Molar.",
        prepSection: "Jak przygotowano modele",
        prep1: "Geometria nie została uproszczona – liczba wierzchołków i trójkątów jest identyczna jak w oryginałach.",
        prep2: "Każdy ząb ustawiono w jednolitej orientacji (oś długa pionowo, powierzchnia przedsionkowa do widza) i wyśrodkowano.",
        prep3: "Skalę dopasowano do średniej długości zęba (korona + korzeń) wg tablic Wheeler's.",
        prep4: "Materiał ujednolicono (matowy, bez metalicznego połysku), aby odblaski nie zasłaniały morfologii.",
        prep5: "Z modelu z guzkiem Carabellego usunięto dwie niewidoczne siatki leżące wewnątrz zęba.",
        prep6: "Źródłowe skany dotyczą zębów lewej strony; zęby 11–18 i 41–48 są ich lustrzanym odbiciem.",
        engineSection: "Silnik",
        engineBody: "Renderowanie: RealityKit. Interfejs: SwiftUI."
    )

    static let en = AppStrings(
        // Common / Navigation
        appName: "Morfo",
        back: "Back",
        close: "Close",
        about: "About",
        settings: "Settings",
        language: "Language",
        clear: "Clear",
        cancel: "Cancel",
        reset: "Reset",
        delete: "Remove",
        compare: "Compare",
        info: "Info",
        duplicate: "Duplicate",
        hide: "Hide",
        carabelliCusp: "Carabelli's cusp",
        resetRotation: "Reset rotation",
        resetPosition: "Reset position",
        auto: "Auto",
        autoLayout: "Auto layout",
        syncShort: "Sync",
        syncOn: "Sync: on",
        syncOff: "Sync: off",
        loadingModels: "Loading models",
        // Selector
        selectTeethSubtitle: "Select teeth to compare",
        quiz: "Quiz",
        filters: "Filters",
        selectMatching: "Select matching",
        clearFilters: "Clear filters",
        quickComparisons: "Quick comparisons",
        noneSelected: "None selected",
        compareCount: { n in "Compare (\(n))" },
        maxillaryArch: "MAXILLARY",
        mandibularArch: "MANDIBULAR",
        rightSideShort: "R",
        leftSideShort: "L",
        // View Settings
        viewSettings: "View settings",
        lighting: "Lighting",
        labels: "Labels",
        rotation: "Rotation",
        layout: "Layout",
        spacing: "Spacing",
        scale: "Scale",
        controls: "Controls",
        syncAllTeethTitle: "Synchronize rotation of all teeth",
        syncAllTeethSubtitle: "Gestures and views affect all teeth at once",
        mirrorContralateralTitle: "Mirror rotation for contralateral side",
        mirrorContralateralSubtitle: "E.g. 14 and 24 rotate symmetrically, showing the same surface",
        controlRotate: "One finger – rotate",
        controlPanZoom: "Two fingers – pan and zoom; twist to rotate around view axis",
        controlDragTooth: "Hold and drag tooth – reposition tooth",
        controlTap: "Tap – select tooth; double tap – focus / frame all",
        // Tooth Info Sheet
        classLabel: "Class",
        archLabel: "Arch",
        sideLabel: "Side",
        avgLengthLabel: "Average length",
        crownPlusRoot: "mm (crown + root)",
        modelLabel: "Model",
        mirrorScanOf: { fdi in "mirror image of tooth \(fdi) scan" },
        scanOf: { fdi in "scan of tooth \(fdi)" },
        carabelliVariantTitle: "Variant with cusp of Carabelli",
        carabelliVariantSubtitle: "Separate specimen; Carabelli's tubercle is a variable trait",
        morphologyTitle: "Key morphological features",
        textbookSummaryFootnote: "Textbook summary (Wheeler's); features describe a typical tooth – specific specimens may vary.",
        // Quiz Home
        quizSubtitle: "Tooth identification: a tooth appears and you type its FDI number. No multiple choice.",
        stats: "Statistics",
        mode: "Mode",
        questionCount: "Question count",
        fullScope: { n in "Full scope (\(n))" },
        yourFlashcards: { sc in "Your flashcards (\(sc))" },
        newCards: "New",
        dueCards: "Due",
        masteredCards: "Mastered",
        scope: "Scope",
        difficulty: "Difficulty",
        options: "Options",
        uniformColorTitle: "Uniform color",
        uniformColorSubtitle: "No scan texture: identify by shape alone, not surface stains of a specific specimen",
        ignoreSideTitle: "Practice without side",
        ignoreSideSubtitle: "Accept correct arch and tooth type; side is not graded",
        startRound: "Start round",
        startFlashcards: "Start flashcards",
        // Quiz Screen
        questionProgress: { i, total, corr in "Question \(i)/\(total) · ✓ \(corr)" },
        flashcardsProgress: { n, streak in "Flashcards · \(n) ans. · streak \(streak)" },
        quizGestureHint: "Rotate tooth with one finger, zoom with two.",
        checkAnswer: "Check",
        enterTwoDigits: "Enter two digits",
        notPermanentFdi: { inp in "\(inp) is not a permanent tooth FDI number (11–48)" },
        checkArch: "Arch",
        checkSide: "Side",
        checkTooth: "Tooth",
        thisIs: "This is:",
        entered: "Entered:",
        next: "Next",
        results: "Results",
        // Quiz Results
        roundResults: "Round results",
        correctPercentage: { pct in "\(pct)% correct answers" },
        mistakes: "Mistakes",
        noMistakes: "No mistakes! 🎉",
        enteredMistake: { fdi, sum in "entered \(fdi) · \(sum)" },
        repeatMistakes: { n in "Repeat mistakes (\(n))" },
        newRound: "New round",
        // Quiz Stats
        confirmResetTitle: "Reset statistics?",
        confirmResetMessage: "All results and flashcard progress will be cleared. This cannot be undone.",
        emptyStats: "No answers yet. Complete a round or flashcards to see results here.",
        answersLabel: "Answers",
        correctLabel: "Correct",
        masteredLabel: "Mastered",
        accuracyOnTeeth: "Tooth accuracy",
        accuracyForTooth: { fdi, name, corr, att in "\(fdi) · \(name): \(corr)/\(att) correct" },
        errorTypes: "Error types",
        wrongArch: "Wrong arch (maxillary / mandibular)",
        wrongSide: "Wrong side (right / left)",
        wrongTooth: "Wrong tooth (type)",
        errorPercent: { count, pct in "\(count) (\(pct)% errors)" },
        mostFrequentConfusions: "Most frequent confusions",
        noConfusions: "No confusions.",
        shownEntered: { name, ans in "shown \(name), entered \(ans)" },
        // About Screen
        aboutTitle: "About",
        modelsSection: "3D Models",
        modelsBody1: "Tooth models: University of Dundee, School of Dentistry (sketchfab.com/DundeeDental), Creative Commons Attribution 4.0 (CC BY 4.0) licence.",
        modelsBody2: "Models used: Maxillary Central / Lateral Incisor, Canine, First / Second Premolar, First Molar, First Molar with Cusp of Carabelli, Second / Third Molar; Mandibular Central / Lateral Incisor, Canine, First / Second Premolar, First / Second / Third Molar.",
        prepSection: "How models were prepared",
        prep1: "Geometry is not decimated – vertex and triangle counts are identical to originals.",
        prep2: "Each tooth is canonically aligned (long axis vertical, facial surface toward viewer) and centered.",
        prep3: "Scale matches average tooth length (crown + root) per Wheeler's tables.",
        prep4: "Material normalized (matte, non-metallic) so highlights do not obscure morphology.",
        prep5: "Removed two hidden meshes inside the Carabelli model (ZBrush subtool leftovers).",
        prep6: "Source scans are left-side teeth; teeth 11–18 and 41–48 are mirrored at runtime.",
        engineSection: "Engine",
        engineBody: "Rendering: RealityKit. UI: SwiftUI."
    )
}

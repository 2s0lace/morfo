// Generated from app/src/main/java/app/morfo/dentition/Morphology.kt — keep both in sync.

/// Short summary of the classic distinguishing features of each tooth type (Polish and English).
/// Source: standard dental anatomy textbooks (Wheeler's). Meant as a study aid next to the 3D
/// model, not as a replacement for the textbook.
enum Morphology {
    private struct Key: Hashable {
        let arch: Arch
        let kind: ToothKind
        init(_ arch: Arch, _ kind: ToothKind) { self.arch = arch; self.kind = kind }
    }

    static func keyFeatures(_ tooth: Tooth, language: AppLanguage) -> [String] {
        let key = Key(tooth.arch, tooth.kind)
        return (language == .en ? featuresEn[key] : featuresPl[key]) ?? []
    }

    private static let featuresPl: [Key: [String]] = [
        Key(.maxillary, .centralIncisor): [
                "Największy siekacz; najszersza mezjodystalnie korona wśród zębów przednich (ok. 8,5 mm).",
                "Kąt mezjalno-sieczny prawie prosty, dystalno-sieczny wyraźnie bardziej zaokrąglony.",
                "Powierzchnia wargowa lekko wypukła, często z dwiema płytkimi bruzdami rozwojowymi.",
                "Powierzchnia podniebienna: dół podniebienny ograniczony listwami brzeżnymi, guzek (cingulum) w ⅓ przyszyjkowej.",
                "Linia szyjkowa na powierzchniach stycznych silnie wygięta w stronę brzegu siecznego, mocniej po stronie mezjalnej.",
                "Jeden stożkowaty korzeń, na przekroju zaokrąglono-trójkątny.",
        ],
        Key(.maxillary, .lateralIncisor): [
                "Mniejszy i węższy od siekacza przyśrodkowego (ok. 6,5 mm mezjodystalnie).",
                "Kąty sieczne bardziej zaokrąglone, szczególnie dystalny.",
                "Głębszy dół podniebienny; częsty otworek ślepy i bruzda podniebienno-dziąsłowa.",
                "Duża zmienność kształtu (np. postać czopkowata).",
                "Pojedynczy korzeń, długi względem korony; wierzchołek często zagięty dystalnie.",
        ],
        Key(.maxillary, .canine): [
                "Najdłuższy ząb w łuku; bardzo długi, masywny korzeń.",
                "Korona z wyraźnym guzkiem; ramię mezjalne krawędzi siecznej krótsze od dystalnego.",
                "Wyraźna listwa wargowa na wypukłej powierzchni wargowej.",
                "Na powierzchni podniebiennej listwa podniebienna dzieląca dwa płytkie dołki; dobrze rozwinięty guzek (cingulum).",
                "Korzeń spłaszczony mezjodystalnie, z płytkimi bruzdami na powierzchniach stycznych.",
        ],
        Key(.maxillary, .firstPremolar): [
                "Dwa guzki: policzkowy i podniebienny; guzek policzkowy wyższy (ok. 1 mm).",
                "Szczyt guzka policzkowego przesunięty lekko dystalnie (ramię mezjalne dłuższe od dystalnego).",
                "Wklęsłość na mezjalnej powierzchni korony przechodząca na korzeń; bruzda brzeżna mezjalna przecina listwę brzeżną.",
                "Zwykle dwa korzenie: policzkowy i podniebienny.",
                "Od strony żującej zarys sześciokątny, długa bruzda środkowa.",
        ],
        Key(.maxillary, .secondPremolar): [
                "Guzki policzkowy i podniebienny prawie równej wysokości.",
                "Zwykle jeden korzeń.",
                "Krótka bruzda środkowa i liczne bruzdy dodatkowe na powierzchni żującej.",
                "Brak wyraźnej wklęsłości mezjalnej korony.",
                "Od strony żującej zarys bardziej owalny i symetryczny niż w pierwszym przedtrzonowcu.",
        ],
        Key(.maxillary, .firstMolar): [
                "Największy ząb szczęki; 4 główne guzki: mezjalno-policzkowy, dystalno-policzkowy, mezjalno-podniebienny (największy) i dystalno-podniebienny.",
                "Guzek Carabellego (cecha zmienna) na podniebiennej powierzchni guzka mezjalno-podniebiennego.",
                "Listwa skośna łączy guzek mezjalno-podniebienny z dystalno-policzkowym.",
                "Trzy szeroko rozstawione korzenie: mezjalno-policzkowy, dystalno-policzkowy i podniebienny (najdłuższy).",
                "Od strony żującej zarys romboidalny.",
        ],
        Key(.maxillary, .secondMolar): [
                "Mniejszy od pierwszego trzonowca; guzek dystalno-podniebienny zredukowany lub nieobecny.",
                "Zwykle brak guzka Carabellego; listwa skośna słabiej zaznaczona.",
                "Trzy korzenie położone bliżej siebie, nachylone dystalnie; mogą się zrastać.",
                "Od strony żującej zarys romboidalny lub sercowaty (odmiana trójguzkowa).",
        ],
        Key(.maxillary, .thirdMolar): [
                "Najbardziej zmienny ząb szczęki; zwykle trzy guzki, korona w kształcie serca.",
                "Liczne bruzdy dodatkowe na powierzchni żującej.",
                "Korzenie krótkie, często zrośnięte w jeden stożek i zagięte dystalnie.",
        ],
        Key(.mandibular, .centralIncisor): [
                "Najmniejszy ząb uzębienia stałego (korona ok. 5 mm mezjodystalnie).",
                "Korona symetryczna; kąty mezjalno- i dystalno-sieczne ostre, prawie jednakowe.",
                "Brzeg sieczny ustawiony prostopadle do linii wargowo-językowej (widok od strony siecznej).",
                "Gładka powierzchnia językowa, słabo zaznaczony dołek i guzek.",
                "Korzeń spłaszczony mezjodystalnie, z bruzdami na powierzchniach stycznych.",
        ],
        Key(.mandibular, .lateralIncisor): [
                "Nieco większy od siekacza przyśrodkowego (szerszy i dłuższy).",
                "Korona mniej symetryczna: kąt dystalno-sieczny bardziej zaokrąglony.",
                "Brzeg sieczny skręcony dystalno-językowo, zgodnie z krzywizną łuku (widok od strony siecznej).",
                "Guzek (cingulum) przesunięty lekko dystalnie.",
                "Korzeń spłaszczony, dłuższy niż w siekaczu przyśrodkowym.",
        ],
        Key(.mandibular, .canine): [
                "Korona węższa mezjodystalnie niż w kle górnym, ale podobnie długa.",
                "Guzek mniej ostry; ramię mezjalne krawędzi siecznej znacznie krótsze od dystalnego.",
                "Kontur mezjalny korony prawie prosty, przechodzi w kontur korzenia.",
                "Powierzchnia językowa gładsza, słabiej zaznaczone listwy i guzek niż w kle górnym.",
                "Jeden korzeń (rzadko rozdwojony), spłaszczony mezjodystalnie.",
        ],
        Key(.mandibular, .firstPremolar): [
                "Duży, ostry guzek policzkowy i bardzo mały guzek językowy; kształtem przypomina kieł.",
                "Listwa poprzeczna łączy guzki i oddziela dołek mezjalny od dystalnego.",
                "Często obecna bruzda mezjalno-językowa.",
                "Korona wyraźnie nachylona językowo względem korzenia.",
                "Jeden korzeń.",
        ],
        Key(.mandibular, .secondPremolar): [
                "Guzek językowy większy niż w pierwszym przedtrzonowcu.",
                "Częsta odmiana trójguzkowa (dwa guzki językowe) z bruzdą w kształcie litery Y; odmiana dwuguzkowa z bruzdą H lub U.",
                "Korona mniej nachylona językowo; zarys od strony żującej bardziej kwadratowy.",
                "Jeden korzeń, masywniejszy niż w pierwszym przedtrzonowcu.",
        ],
        Key(.mandibular, .firstMolar): [
                "Największy ząb żuchwy; 5 guzków: 3 policzkowe (mezjalny, dystalny, dystalny tylny) i 2 językowe.",
                "Guzki językowe wyższe i ostrzejsze od policzkowych.",
                "Dwie bruzdy policzkowe; na powierzchni żującej wzór bruzd Y5.",
                "Dwa korzenie: mezjalny (szerszy, zakrzywiony) i dystalny (prostszy).",
                "Zarys od strony żującej pięciokątny, wydłużony mezjodystalnie.",
        ],
        Key(.mandibular, .secondMolar): [
                "Cztery guzki (2 policzkowe, 2 językowe) prawie równej wielkości.",
                "Bruzdy tworzą wzór krzyża (+4); jedna bruzda policzkowa.",
                "Dwa korzenie położone bliżej siebie niż w pierwszym trzonowcu, nachylone dystalnie.",
                "Zarys od strony żującej prostokątny.",
        ],
        Key(.mandibular, .thirdMolar): [
                "Bardzo zmienny kształt; 4 lub 5 guzków i liczne bruzdy dodatkowe.",
                "Korona zaokrąglona, zwężająca się dystalnie.",
                "Korzenie krótkie, często zrośnięte i silnie nachylone dystalnie.",
        ],
    ]

    private static let featuresEn: [Key: [String]] = [
        Key(.maxillary, .centralIncisor): [
                "Largest incisor; widest mesiodistal crown among anterior teeth (~8.5 mm).",
                "Mesioincisal angle nearly a right angle; distoincisal angle noticeably more rounded.",
                "Labial surface slightly convex, often with two shallow developmental depressions.",
                "Palatal surface: palatal fossa bounded by marginal ridges, prominent cingulum in cervical third.",
                "Cervical line on proximal surfaces curves strongly toward incisal edge, deeper on mesial.",
                "Single conical root, rounded-triangular in cross-section.",
        ],
        Key(.maxillary, .lateralIncisor): [
                "Smaller and narrower than central incisor (~6.5 mm mesiodistally).",
                "Incisal angles more rounded, especially distoincisal.",
                "Deeper palatal fossa; frequent lingual pit (foramen caecum) and palatogingival groove.",
                "High morphological variability (e.g., peg-shaped lateral).",
                "Single root, long relative to crown; apex frequently curved distally.",
        ],
        Key(.maxillary, .canine): [
                "Longest tooth in the arch; very long, massive root forming the canine eminence.",
                "Crown with prominent cusp; mesial cusp ridge shorter than distal cusp ridge.",
                "Distinct labial ridge on convex labial surface.",
                "Palatal ridge dividing two shallow palatal fossae; well-developed cingulum.",
                "Root flattened mesiodistally, with shallow developmental depressions on proximal surfaces.",
        ],
        Key(.maxillary, .firstPremolar): [
                "Two cusps: buccal and palatal; buccal cusp higher (~1 mm).",
                "Buccal cusp tip shifted slightly distally (mesial cusp ridge longer than distal).",
                "Pronounced mesial developmental depression on crown continuing onto root; mesial marginal groove crosses marginal ridge.",
                "Usually two roots: buccal and palatal.",
                "Hexagonal occlusal outline, long central developmental groove.",
        ],
        Key(.maxillary, .secondPremolar): [
                "Buccal and palatal cusps nearly equal in height.",
                "Usually a single root.",
                "Short central groove with numerous supplemental grooves on occlusal surface.",
                "No pronounced mesial crown concavity.",
                "More oval and symmetrical occlusal outline than the first premolar.",
        ],
        Key(.maxillary, .firstMolar): [
                "Largest maxillary tooth; 4 major cusps: mesiobuccal, distobuccal, mesiopalatal (largest), and distopalatal.",
                "Cusp of Carabelli (variable trait) on palatal surface of mesiopalatal cusp.",
                "Oblique ridge connects mesiopalatal cusp to distobuccal cusp.",
                "Three widely separated roots: mesiobuccal, distobuccal, and palatal (longest).",
                "Rhomboidal occlusal outline.",
        ],
        Key(.maxillary, .secondMolar): [
                "Smaller than first molar; distopalatal cusp reduced or absent.",
                "Usually no Cusp of Carabelli; oblique ridge less prominent.",
                "Three roots positioned closer together, inclined distally; may be fused.",
                "Rhomboidal or heart-shaped (3-cusp variety) occlusal outline.",
        ],
        Key(.maxillary, .thirdMolar): [
                "Most variable maxillary tooth; typically 3 cusps with a heart-shaped crown.",
                "Numerous supplemental grooves on occlusal surface.",
                "Short roots, often fused into a single cone and curved distally.",
        ],
        Key(.mandibular, .centralIncisor): [
                "Smallest permanent tooth (crown ~5 mm mesiodistally).",
                "Bilaterally symmetrical crown; sharp, nearly identical mesio- and distoincisal angles.",
                "Incisal edge perpendicular to labiolingual bisector (incisal view).",
                "Smooth lingual surface with faint fossa and cingulum.",
                "Root flattened mesiodistally with proximal developmental depressions.",
        ],
        Key(.mandibular, .lateralIncisor): [
                "Slightly larger than mandibular central incisor (wider and longer).",
                "Less symmetrical crown: distoincisal angle more rounded.",
                "Incisal edge twisted distolingually, following arch curvature (incisal view).",
                "Cingulum shifted slightly distally.",
                "Root flattened mesiodistally, longer than in central incisor.",
        ],
        Key(.mandibular, .canine): [
                "Crown narrower mesiodistally than maxillary canine, but similarly long.",
                "Cusp less pointed; mesial cusp ridge much shorter than distal.",
                "Mesial crown outline nearly straight and continuous with root outline.",
                "Lingual surface smoother with less prominent ridges and cingulum than maxillary canine.",
                "Single root (rarely bifurcated), flattened mesiodistally.",
        ],
        Key(.mandibular, .firstPremolar): [
                "Large, sharp buccal cusp and very small non-functional lingual cusp; resembles a canine.",
                "Transverse ridge connects cusps and separates mesial and distal fossae.",
                "Mesiolingual developmental groove frequently present.",
                "Crown noticeably tilted lingually over the root.",
                "Single root.",
        ],
        Key(.mandibular, .secondPremolar): [
                "Lingual cusp(s) larger than in first premolar.",
                "Common 3-cusp variety (two lingual cusps) with Y-shaped groove pattern; 2-cusp variety with H or U pattern.",
                "Crown less tilted lingually; squarish occlusal outline.",
                "Single root, broader and sturdier than in first premolar.",
        ],
        Key(.mandibular, .firstMolar): [
                "Largest mandibular tooth; 5 cusps: 3 buccal (mesiobuccal, distobuccal, distal) and 2 lingual.",
                "Lingual cusps higher and more pointed than buccal cusps.",
                "Two buccal developmental grooves; Y5 fissure pattern on occlusal surface.",
                "Two roots: mesial (broader, curved) and distal (straighter).",
                "Pentagonal occlusal outline, elongated mesiodistally.",
        ],
        Key(.mandibular, .secondMolar): [
                "Four cusps (2 buccal, 2 lingual) of nearly equal size.",
                "Cruciform (+4) occlusal groove pattern; single buccal groove.",
                "Two roots closer together than in first molar, inclined distally.",
                "Rectangular occlusal outline.",
        ],
        Key(.mandibular, .thirdMolar): [
                "Highly variable shape; 4 or 5 cusps and numerous supplemental grooves.",
                "Rounded crown tapering distally.",
                "Short roots, often fused and strongly inclined distally.",
        ],
    ]
}

// Morfo Dental Data & Textbook Summaries (Wheeler's Dental Anatomy)

export const TOOTH_CLASSES = {
  INCISOR: { pl: 'Siekacze', en: 'Incisors' },
  CANINE: { pl: 'Kły', en: 'Canines' },
  PREMOLAR: { pl: 'Przedtrzonowce', en: 'Premolars' },
  MOLAR: { pl: 'Trzonowce', en: 'Molars' }
};

export const ARCHES = {
  MAXILLARY: { pl: 'Szczęka (górny)', en: 'Maxillary (upper)' },
  MANDIBULAR: { pl: 'Żuchwa (dolny)', en: 'Mandibular (lower)' }
};

export const SIDES = {
  RIGHT: { pl: 'Prawa', en: 'Right' },
  LEFT: { pl: 'Lewa', en: 'Left' }
};

export const TOOTH_KINDS = [
  { pos: 1, classKey: 'INCISOR', namePl: 'siekacz przyśrodkowy', nameEn: 'Central Incisor', anterior: true },
  { pos: 2, classKey: 'INCISOR', namePl: 'siekacz boczny', nameEn: 'Lateral Incisor', anterior: true },
  { pos: 3, classKey: 'CANINE', namePl: 'kieł', nameEn: 'Canine', anterior: true },
  { pos: 4, classKey: 'PREMOLAR', namePl: 'pierwszy przedtrzonowiec', nameEn: 'First Premolar', anterior: false },
  { pos: 5, classKey: 'PREMOLAR', namePl: 'drugi przedtrzonowiec', nameEn: 'Second Premolar', anterior: false },
  { pos: 6, classKey: 'MOLAR', namePl: 'pierwszy trzonowiec', nameEn: 'First Molar', anterior: false },
  { pos: 7, classKey: 'MOLAR', namePl: 'drugi trzonowiec', nameEn: 'Second Molar', anterior: false },
  { pos: 8, classKey: 'MOLAR', namePl: 'trzeci trzonowiec', nameEn: 'Third Molar', anterior: false }
];

export const WHEELER_LENGTHS_MM = {
  MAXILLARY: [23.5, 22.0, 27.0, 22.5, 22.5, 20.5, 19.0, 17.5],
  MANDIBULAR: [21.5, 23.5, 27.0, 22.5, 22.5, 21.5, 20.0, 18.0]
};

export const MORPHOLOGY_PL = {
  'MAXILLARY_1': [
    'Największy siekacz; najszersza mezjodystalnie korona wśród zębów przednich (ok. 8,5 mm).',
    'Kąt mezjalno-sieczny prawie prosty, dystalno-sieczny wyraźnie bardziej zaokrąglony.',
    'Powierzchnia wargowa lekko wypukła, często z dwiema płytkimi bruzdami rozwojowymi.',
    'Powierzchnia podniebienna: dół podniebienny ograniczony listwami brzeżnymi, guzek (cingulum) w ⅓ przyszyjkowej.',
    'Linia szyjkowa silnie wygięta w stronę brzegu siecznego, mocniej po stronie mezjalnej.',
    'Jeden stożkowaty korzeń, na przekroju zaokrąglono-trójkątny.'
  ],
  'MAXILLARY_2': [
    'Mniejszy i węższy od siekacza przyśrodkowego (ok. 6,5 mm mezjodystalnie).',
    'Kąty sieczne bardziej zaokrąglone, szczególnie dystalny.',
    'Głębszy dół podniebienny; częsty otworek ślepy i bruzda podniebienno-dziąsłowa.',
    'Duża zmienność kształtu (np. postać czopkowata).',
    'Pojedynczy korzeń, długi względem korony; wierzchołek często zagięty dystalnie.'
  ],
  'MAXILLARY_3': [
    'Najdłuższy ząb w łuku; bardzo długi, masywny korzeń.',
    'Korona z wyraźnym guzkiem; ramię mezjalne krawędzi siecznej krótsze od dystalnego.',
    'Wyraźna listwa wargowa na wypukłej powierzchni wargowej.',
    'Na powierzchni podniebiennej listwa dzieląca dwa płytkie dołki; dobrze rozwinięty guzek (cingulum).',
    'Korzeń spłaszczony mezjodystalnie z bruzdami na powierzchniach stycznych.'
  ],
  'MAXILLARY_4': [
    'Dwa guzki: policzkowy i podniebienny; guzek policzkowy wyższy (ok. 1 mm).',
    'Szczyt guzka policzkowego przesunięty lekko dystalnie (ramię mezjalne dłuższe od dystalnego).',
    'Wklęsłość na mezjalnej powierzchni korony przechodząca na korzeń; bruzda brzeżna mezjalna.',
    'Zwykle dwa korzenie: policzkowy i podniebienny.',
    'Od strony żującej zarys sześciokątny, długa bruzda środkowa.'
  ],
  'MAXILLARY_5': [
    'Guzki policzkowy i podniebienny prawie równej wysokości.',
    'Zwykle jeden korzeń.',
    'Krótka bruzda środkowa i liczne bruzdy dodatkowe na powierzchni żującej.',
    'Brak wyraźnej wklęsłości mezjalnej korony.',
    'Od strony żującej zarys bardziej owalny i symetryczny niż w pierwszym przedtrzonowcu.'
  ],
  'MAXILLARY_6': [
    'Największy ząb szczęki; 4 główne guzki (MP, DP, MPol, DPol).',
    'Guzek Carabellego (cecha zmienna) na podniebiennej powierzchni guzka mezjalno-podniebiennego.',
    'Listwa skośna łączy guzek mezjalno-podniebienny z dystalno-policzkowym.',
    'Trzy szeroko rozstawione korzenie: mezjalno-policzkowy, dystalno-policzkowy i podniebienny (najdłuższy).',
    'Od strony żującej zarys romboidalny.'
  ],
  'MAXILLARY_7': [
    'Mniejszy od pierwszego trzonowca; guzek dystalno-podniebienny zredukowany lub nieobecny.',
    'Zwykle brak guzka Carabellego; listwa skośna słabiej zaznaczona.',
    'Trzy korzenie położone bliżej siebie, nachylone dystalnie; mogą się zrastać.',
    'Od strony żującej zarys romboidalny lub sercowaty (odmiana trójguzkowa).'
  ],
  'MAXILLARY_8': [
    'Najbardziej zmienny ząb szczęki; zwykle trzy guzki, korona w kształcie serca.',
    'Liczne bruzdy dodatkowe na powierzchni żującej.',
    'Korzenie krótkie, często zrośnięte w jeden stożek i zagięte dystalnie.'
  ],
  'MANDIBULAR_1': [
    'Najmniejszy ząb uzębienia stałego (korona ok. 5 mm mezjodystalnie).',
    'Korona wybitnie symetryczna; kąty mezjalno- i dystalno-sieczne ostre, prawie jednakowe.',
    'Brzeg sieczny ustawiony prostopadle do linii wargowo-językowej.',
    'Gładka powierzchnia językowa, słabo zaznaczony dołek i guzek.',
    'Korzeń spłaszczony mezjodystalnie, z bruzdami na powierzchniach stycznych.'
  ],
  'MANDIBULAR_2': [
    'Nieco większy od siekacza przyśrodkowego (szerszy i dłuższy).',
    'Korona mniej symetryczna: kąt dystalno-sieczny bardziej zaokrąglony.',
    'Brzeg sieczny skręcony dystalno-językowo, zgodnie z krzywizną łuku.',
    'Guzek (cingulum) przesunięty lekko dystalnie.',
    'Korzeń spłaszczony, dłuższy niż w siekaczu przyśrodkowym.'
  ],
  'MANDIBULAR_3': [
    'Korona węższa mezjodystalnie niż w kle górnym, ale podobnie długa.',
    'Guzek mniej ostry; ramię mezjalne krawędzi siecznej znacznie krótsze od dystalnego.',
    'Kontur mezjalny korony prawie prosty, przechodzi płynnie w kontur korzenia.',
    'Powierzchnia językowa gładsza, słabiej zaznaczone listwy niż w kle górnym.',
    'Jeden korzeń, spłaszczony mezjodystalnie.'
  ],
  'MANDIBULAR_4': [
    'Duży, ostry guzek policzkowy i bardzo mały guzek językowy (przypomina kieł).',
    'Listwa poprzeczna łączy guzki i oddziela dołek mezjalny od dystalnego.',
    'Często obecna bruzda mezjalno-językowa.',
    'Korona wyraźnie nachylona językowo względem korzenia.',
    'Jeden korzeń.'
  ],
  'MANDIBULAR_5': [
    'Guzek językowy większy niż w pierwszym przedtrzonowcu.',
    'Częsta odmiana trójguzkowa (dwa guzki językowe) z bruzdą Y; odmiana dwuguzkowa z bruzdą H lub U.',
    'Korona mniej nachylona językowo; zarys od strony żującej bardziej kwadratowy.',
    'Jeden korzeń, masywniejszy niż w pierwszym przedtrzonowcu.'
  ],
  'MANDIBULAR_6': [
    'Największy ząb żuchwy; 5 guzków: 3 policzkowe (MB, DB, D) i 2 językowe.',
    'Guzki językowe wyższe i ostrzejsze od policzkowych.',
    'Dwie bruzdy policzkowe; na powierzchni żującej wzór bruzd Y5.',
    'Dwa korzenie: mezjalny (szerszy, zakrzywiony) i dystalny (prostszy).',
    'Zarys od strony żującej pięciokątny, wydłużony mezjodystalnie.'
  ],
  'MANDIBULAR_7': [
    'Cztery guzki (2 policzkowe, 2 językowe) prawie równej wielkości.',
    'Bruzdy tworzą wzór krzyża (+4); jedna bruzda policzkowa.',
    'Dwa korzenie położone bliżej siebie niż w pierwszym trzonowcu, nachylone dystalnie.',
    'Zarys od strony żującej prostokątny.'
  ],
  'MANDIBULAR_8': [
    'Bardzo zmienny kształt; 4 lub 5 guzków i liczne bruzdy dodatkowe.',
    'Korona zaokrąglona, zwężająca się dystalnie.',
    'Korzenie krótkie, często zrośnięte i silnie nachylone dystalnie.'
  ]
};

export const MORPHOLOGY_EN = {
  'MAXILLARY_1': [
    'Largest incisor; widest mesiodistal crown among anterior teeth (~8.5 mm).',
    'Mesioincisal angle nearly a right angle; distoincisal angle noticeably more rounded.',
    'Labial surface slightly convex, often with two shallow developmental depressions.',
    'Palatal surface: palatal fossa bounded by marginal ridges, prominent cingulum in cervical third.',
    'Cervical line curves strongly toward incisal edge, deeper on mesial.',
    'Single conical root, rounded-triangular in cross-section.'
  ],
  'MAXILLARY_2': [
    'Smaller and narrower than central incisor (~6.5 mm mesiodistally).',
    'Incisal angles more rounded, especially distoincisal.',
    'Deeper palatal fossa; frequent lingual pit (foramen caecum) and palatogingival groove.',
    'High morphological variability (e.g., peg-shaped lateral).',
    'Single root, long relative to crown; apex frequently curved distally.'
  ],
  'MAXILLARY_3': [
    'Longest tooth in the arch; very long, massive root forming the canine eminence.',
    'Crown with prominent cusp; mesial cusp ridge shorter than distal cusp ridge.',
    'Distinct labial ridge on convex labial surface.',
    'Palatal ridge dividing two shallow palatal fossae; well-developed cingulum.',
    'Root flattened mesiodistally, with shallow developmental depressions on proximal surfaces.'
  ],
  'MAXILLARY_4': [
    'Two cusps: buccal and palatal; buccal cusp higher (~1 mm).',
    'Buccal cusp tip shifted slightly distally (mesial cusp ridge longer than distal).',
    'Pronounced mesial developmental depression on crown continuing onto root; mesial marginal groove.',
    'Usually two roots: buccal and palatal.',
    'Hexagonal occlusal outline, long central developmental groove.'
  ],
  'MAXILLARY_5': [
    'Buccal and palatal cusps nearly equal in height.',
    'Usually a single root.',
    'Short central groove with numerous supplemental grooves on occlusal surface.',
    'No pronounced mesial crown concavity.',
    'More oval and symmetrical occlusal outline than the first premolar.'
  ],
  'MAXILLARY_6': [
    'Largest maxillary tooth; 4 major cusps: mesiobuccal, distobuccal, mesiopalatal (largest), distopalatal.',
    'Cusp of Carabelli (variable trait) on palatal surface of mesiopalatal cusp.',
    'Oblique ridge connects mesiopalatal cusp to distobuccal cusp.',
    'Three widely separated roots: mesiobuccal, distobuccal, and palatal (longest).',
    'Rhomboidal occlusal outline.'
  ],
  'MAXILLARY_7': [
    'Smaller than first molar; distopalatal cusp reduced or absent.',
    'Usually no Cusp of Carabelli; oblique ridge less prominent.',
    'Three roots positioned closer together, inclined distally; may be fused.',
    'Rhomboidal or heart-shaped (3-cusp variety) occlusal outline.'
  ],
  'MAXILLARY_8': [
    'Most variable maxillary tooth; typically 3 cusps with a heart-shaped crown.',
    'Numerous supplemental grooves on occlusal surface.',
    'Short roots, often fused into a single cone and curved distally.'
  ],
  'MANDIBULAR_1': [
    'Smallest permanent tooth (crown ~5 mm mesiodistally).',
    'Bilaterally symmetrical crown; sharp, nearly identical mesio- and distoincisal angles.',
    'Incisal edge perpendicular to labiolingual bisector (incisal view).',
    'Smooth lingual surface with faint fossa and cingulum.',
    'Root flattened mesiodistally with proximal developmental depressions.'
  ],
  'MANDIBULAR_2': [
    'Slightly larger than mandibular central incisor (wider and longer).',
    'Less symmetrical crown: distoincisal angle more rounded.',
    'Incisal edge twisted distolingually, following arch curvature (incisal view).',
    'Cingulum shifted slightly distally.',
    'Root flattened mesiodistally, longer than in central incisor.'
  ],
  'MANDIBULAR_3': [
    'Crown narrower mesiodistally than maxillary canine, but similarly long.',
    'Cusp less pointed; mesial cusp ridge much shorter than distal.',
    'Mesial crown outline nearly straight and continuous with root outline.',
    'Lingual surface smoother with less prominent ridges and cingulum than maxillary canine.',
    'Single root (rarely bifurcated), flattened mesiodistally.'
  ],
  'MANDIBULAR_4': [
    'Large, sharp buccal cusp and very small non-functional lingual cusp; resembles a canine.',
    'Transverse ridge connects cusps and separates mesial and distal fossae.',
    'Mesiolingual developmental groove frequently present.',
    'Crown noticeably tilted lingually over the root.',
    'Single root.'
  ],
  'MANDIBULAR_5': [
    'Lingual cusp(s) larger than in first premolar.',
    'Common 3-cusp variety (two lingual cusps) with Y-shaped groove; 2-cusp variety with H or U pattern.',
    'Crown less tilted lingually; squarish occlusal outline.',
    'Single root, broader and sturdier than in first premolar.'
  ],
  'MANDIBULAR_6': [
    'Largest mandibular tooth; 5 cusps: 3 buccal (MB, DB, D) and 2 lingual.',
    'Lingual cusps higher and more pointed than buccal cusps.',
    'Two buccal developmental grooves; Y5 fissure pattern on occlusal surface.',
    'Two roots: mesial (broader, curved) and distal (straighter).',
    'Pentagonal occlusal outline, elongated mesiodistally.'
  ],
  'MANDIBULAR_7': [
    'Four cusps (2 buccal, 2 lingual) of nearly equal size.',
    'Cruciform (+4) occlusal groove pattern; single buccal groove.',
    'Two roots closer together than in first molar, inclined distally.',
    'Rectangular occlusal outline.'
  ],
  'MANDIBULAR_8': [
    'Highly variable shape; 4 or 5 cusps and numerous supplemental grooves.',
    'Rounded crown tapering distally.',
    'Short roots, often fused and strongly inclined distally.'
  ]
};

// Generates the tooth database
export function getTooth(fdi) {
  const quadrant = Math.floor(fdi / 10);
  const pos = fdi % 10;
  if (quadrant < 1 || quadrant > 4 || pos < 1 || pos > 8) return null;

  const kind = TOOTH_KINDS[pos - 1];
  const isMaxillary = quadrant <= 2;
  const isRight = quadrant === 1 || quadrant === 4;
  const archKey = isMaxillary ? 'MAXILLARY' : 'MANDIBULAR';
  const sideKey = isRight ? 'RIGHT' : 'LEFT';

  const isMirrored = isRight;
  const sourceFdi = quadrant === 1 ? fdi + 10 : (quadrant === 4 ? fdi - 10 : fdi);
  const avgLength = WHEELER_LENGTHS_MM[archKey][pos - 1];

  const archPl = isMaxillary ? 'Górny' : 'Dolny';
  const sidePl = isRight ? 'prawy' : 'lewy';
  const namePl = `${archPl} ${sidePl} ${kind.namePl}`;

  const archEn = isMaxillary ? 'Maxillary' : 'Mandibular';
  const sideEn = isRight ? 'Right' : 'Left';
  const nameEn = `${archEn} ${sideEn} ${kind.nameEn}`;

  const morphKey = `${archKey}_${pos}`;

  return {
    fdi,
    quadrant,
    pos,
    kind,
    archKey,
    sideKey,
    isMaxillary,
    isRight,
    isMirrored,
    sourceFdi,
    modelId: `t${sourceFdi}`,
    carabelliModelId: (archKey === 'MAXILLARY' && pos === 6) ? 't26_carabelli' : null,
    hasCarabelli: (archKey === 'MAXILLARY' && pos === 6),
    avgLength,
    namePl,
    nameEn,
    featuresPl: MORPHOLOGY_PL[morphKey] || [],
    featuresEn: MORPHOLOGY_EN[morphKey] || []
  };
}

// All 32 teeth
export const ALL_TEETH = [];
for (let q = 1; q <= 4; q++) {
  for (let p = 1; p <= 8; p++) {
    ALL_TEETH.push(getTooth(q * 10 + p));
  }
}

export const QUICK_COMPARISONS = [
  { id: 'incisors_up', titlePl: 'Górny siekacz przyśrodkowy vs boczny', titleEn: 'Maxillary central vs lateral incisor', teeth: [11, 12] },
  { id: 'premolars_up', titlePl: 'Górny 1. vs 2. przedtrzonowiec', titleEn: 'Maxillary 1st vs 2nd premolar', teeth: [14, 15] },
  { id: 'premolars_low', titlePl: 'Dolny 1. vs 2. przedtrzonowiec', titleEn: 'Mandibular 1st vs 2nd premolar', teeth: [44, 45] },
  { id: 'molars_up', titlePl: 'Górny 1. vs 2. trzonowiec', titleEn: 'Maxillary 1st vs 2nd molar', teeth: [16, 17] },
  { id: 'molars_low', titlePl: 'Dolny 1. vs 2. trzonowiec', titleEn: 'Mandibular 1st vs 2nd molar', teeth: [46, 47] },
  { id: 'canines', titlePl: 'Kieł górny vs dolny', titleEn: 'Maxillary vs mandibular canine', teeth: [13, 43] },
  { id: 'incisors_low', titlePl: 'Dolny siekacz przyśrodkowy vs boczny', titleEn: 'Mandibular central vs lateral incisor', teeth: [41, 42] },
  { id: 'premolars_all', titlePl: 'Przedtrzonowce górne vs dolne', titleEn: 'Maxillary vs mandibular premolars', teeth: [14, 15, 44, 45] },
  { id: 'contralateral', titlePl: 'Zęby przeciwstronne (14 vs 24)', titleEn: 'Contralateral teeth (14 vs 24)', teeth: [14, 24] },
  { id: 'carabelli', titlePl: 'Górny 1. trzonowiec: klasyczny vs guzek Carabellego', titleEn: 'Maxillary 1st molar: standard vs Carabelli', teeth: [16, 16], variantSecond: 'carabelli' }
];

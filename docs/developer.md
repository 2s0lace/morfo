# Dokumentacja Techniczna Morfo (dla programistów)

Dokumentacja architektury, silnika renderowania i procesu budowy aplikacji **Morfo**.

---

## 🛠️ Wymagania i kompilacja

- **JDK**: 17+
- **Android SDK**: compileSdk 37, minSdk 29 (Android 10+), targetSdk 36
- **Architektura procesora**: `arm64-v8a`

```bash
# Uruchomienie testów jednostkowych
./gradlew testDebugUnitTest

# Zbudowanie instalacyjnego pliku APK (Release)
./gradlew assembleRelease

# Instalacja bezpośrednio na podłączonym urządzeniu
./gradlew installDebug
```

Gotowy plik APK znajduje się w katalogu: `app/build/outputs/apk/release/app-release.apk`.

---

## 🏗️ Architektura i Stack Technologiczny

- **Język**: Kotlin 2.x
- **Interfejs użytkownika**: Jetpack Compose + Material 3
- **Silnik renderowania 3D**: [Google Filament](https://github.com/google/filament) + gltfio (PBR, renderowanie na żądanie)
- **Pipeline modeli**: Node.js + glTF Transform (`tools/asset-pipeline` – zobacz [docs/asset-pipeline.md](asset-pipeline.md))

### Struktura pakietów (`app/src/main/java/app/morfo`)

| Pakiet | Zakres odpowiedzialności |
|---|---|
| `dentition` | Metadane zębów: numery FDI, nazewnictwo PL/EN, klasy/łuki/strony, mapowanie modeli, średnie długości (Wheeler), kluczowe cechy morfologiczne. |
| `model` | Metadane modeli runtime (`meta.json`: granice AABB, punkty podparcia). |
| `math` | Wektory i kwaterniony: `Vec3`, `Box3`, `Quat`. |
| `workspace` | Stan sceny porównania: ustawione zęby, selekcja, obrót synchroniczny/symetryczny, presety powierzchni, automatyczny układ bez kolizji. |
| `render` | Integracja z Filamentem: silnik, repozytorium modeli (`ModelRepository`), kamera (`CameraRig`), oświetlenie (`StudioLighting`), zarządca klatek. |
| `ui` | Ekrany Compose: wybór zębów (diagram łuków, filtry, szybkie porównania), przestrzeń 3D (gesty, etykiety, arkusze), quiz, statystyki, ekran o aplikacji. |
| `quiz` | Silnik quizu: tryb rundy, fiszki z powtórkami przestrzennymi (*spaced repetition*), ocenianie łuk/strona/typ, statystyki w SharedPreferences. |
| `i18n` | Obsługa wielojęzyczności (`AppLanguage`, `AppStrings`, `LocalLanguage`, `LocalStrings`). |

---

## 💡 Kluczowe decyzje projektowe

1. **Każdy ząb to niezależna instancja glTF z własnym transformem**:
   Zęby nie są łączone w jedną siatkę. Instancje tego samego modelu współdzielą bufory GPU i tekstury. Zęby strony prawej wykorzystują lustrzane odbicie modeli zębów lewych w osi X (`isMirrored = true`).
2. **Kamera pozostaje nieruchoma – obracają się zęby**:
   Dzięki temu kąt padania światła pozostaje stały względem obserwatora, a badana powierzchnia wygląda identycznie niezależnie od orientacji zęba. Umożliwia to w 100% rzetelne porównanie cech anatomicznych.
3. **Automatyczny układ bez nakładania się**:
   Zęby są rozmieszczane na podstawie ich aktualnych granic przestrzennych w danej orientacji, przeliczanych na bieżąco. Ręczne przesunięcie zęba przełącza układ w tryb swobodny.
4. **Renderowanie na żądanie (On-Demand)**:
   Klatki są renderowane wyłącznie w momencie interakcji (obrót, zoom, animacja). Przy statycznej scenie zużycie GPU wynosi 0%.

---

## 🍎 iOS

Natywna aplikacja w katalogu `ios/` (SwiftUI + RealityKit, Liquid Glass), z tymi samymi funkcjami co wersja na Androida: porównywanie, rzuty, etykiety, quiz, fiszki i statystyki.

- **Wymagania**: Xcode 26+, iOS / iPadOS 26+.
- **Projekt**: `ios/Morfo.xcodeproj`. Modele są brane bezpośrednio z `web/models` (bez kolejnej kopii).
- **Uruchomienie**: otwórz projekt w Xcode, w *Signing & Capabilities* wybierz swój zespół (wystarczy darmowy *Personal Team*) i kliknij ▶.
- **Testy logiki**:
  ```bash
  xcodebuild test -project ios/Morfo.xcodeproj -scheme Morfo -destination 'platform=iOS Simulator,name=iPhone 17'
  ```
- **Debug**: argumenty startowe `-debugTeeth 14,24 -debugRoute compare|quiz|quizHome|quizStats|about` otwierają od razu wybrany ekran.

Kod odpowiada pakietom z Androida: `Core` (dentition, i18n, math), `Workspace`, `Quiz`, `Render` (RealityKit zamiast Filamenta), `UI`. `Core/Strings.swift` i `Core/Morphology.swift` zostały wygenerowane z plików Kotlin – zmieniając teksty, zmień je w obu miejscach.

Różnice w renderowaniu: zęby prawej strony mają osobną, lustrzaną siatkę (ujemna skala psułaby normalne i oświetlenie), a otoczenie studia jest generowane jako obraz HDR dla oświetlenia IBL zamiast harmonik sferycznych.

---

## 🔒 Podpisywanie wydania (CI/CD)

W workflow GitHub Actions (`.github/workflows/build.yml`) konfiguracja wydania odczytuje następujące GitHub Secrets:
- `RELEASE_KEYSTORE_BASE64` – plik keystore zakodowany w Base64
- `RELEASE_STORE_PASSWORD` – hasło magazynu kluczy
- `RELEASE_KEY_ALIAS` – alias klucza wydania
- `RELEASE_KEY_PASSWORD` – hasło klucza

Jeśli zmienne te nie są zdefiniowane, konfiguracja automatycznie stosuje bezpieczny fallback do klucza `debug`, dzięki czemu kompilacja zawsze kończy się sukcesem.

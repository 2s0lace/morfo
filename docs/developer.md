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

## 🔒 Podpisywanie wydania (CI/CD)

W workflow GitHub Actions (`.github/workflows/build.yml`) konfiguracja wydania odczytuje następujące GitHub Secrets:
- `RELEASE_KEYSTORE_BASE64` – plik keystore zakodowany w Base64
- `RELEASE_STORE_PASSWORD` – hasło magazynu kluczy
- `RELEASE_KEY_ALIAS` – alias klucza wydania
- `RELEASE_KEY_PASSWORD` – hasło klucza

Jeśli zmienne te nie są zdefiniowane, konfiguracja automatycznie stosuje bezpieczny fallback do klucza `debug`, dzięki czemu kompilacja zawsze kończy się sukcesem.

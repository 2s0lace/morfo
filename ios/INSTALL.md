# Morfo na iPhone'a – instalacja bez App Store

Morfo nie jest (jeszcze) w App Store ani w TestFlight. Da się je zainstalować za darmo na dwa sposoby. Oba używają **Twojego** Apple ID, więc z darmowym kontem:

- aplikacja działa **7 dni**, potem trzeba ją zainstalować ponownie (dane quizu zostają),
- można mieć naraz najwyżej 3 takie aplikacje,
- telefon musi mieć **iOS 26 lub nowszy** (iPhone 11 i nowsze).

Jeśli nie chcesz nic instalować: wersja w przeglądarce działa od razu – https://2s0lace.github.io/morfo/ (w Safari: Udostępnij → „Dodaj do ekranu początkowego”).

---

## Sposób 1: plik IPA + Sideloadly (Windows lub Mac, bez Xcode)

1. Pobierz `Morfo.ipa` z [najnowszego wydania](https://github.com/2s0lace/morfo/releases/latest) (albo zbuduj: `ios/scripts/build-ipa.sh`).
2. Zainstaluj **Sideloadly** ze strony sideloadly.io. Na Windowsie potrzebne są też iTunes i iCloud w wersji ze strony Apple (nie z Microsoft Store).
3. Podłącz iPhone'a kablem, odblokuj go i stuknij „Ufaj temu komputerowi”.
4. W Sideloadly: przeciągnij `Morfo.ipa`, wpisz swój Apple ID i kliknij **Start**. Hasło trafia tylko do serwerów Apple; jeśli wolisz, załóż osobny Apple ID do tego celu.
5. Na iPhonie:
   - **Ustawienia → Prywatność i ochrona → Tryb dewelopera** → włącz i uruchom telefon ponownie,
   - **Ustawienia → Ogólne → VPN i urządzenia zarządzane** → wybierz swój Apple ID → **Ufaj**.
6. Otwórz Morfo. Po 7 dniach powtórz krok 4 (albo włącz w Sideloadly automatyczne odświeżanie).

## Sposób 2: zbuduj sam w Xcode (Mac)

1. Zainstaluj **Xcode 26+** z App Store i uruchom go raz.
2. Xcode → Settings → Accounts → „+” → Apple Account → zaloguj się.
3. Sklonuj repozytorium i otwórz `ios/Morfo.xcodeproj`.
4. Kliknij niebieską ikonę **Morfo** → TARGETS **Morfo** → **Signing & Capabilities**:
   - **Team**: Twoje imię (Personal Team),
   - **Bundle Identifier**: zmień na własny, np. `com.twojeimie.morfo` (obecny należy do autora).
5. Podłącz iPhone'a, wybierz go na górnym pasku Xcode i kliknij ▶.
6. Na iPhonie włącz Tryb dewelopera i zaufaj certyfikatowi (jak w kroku 5 powyżej).

---

## Sposób 3: niech zrobi to Claude Code albo Codex

Masz Maca i agenta AI w terminalu (Claude Code, Codex)? Zrób sam tylko kroki, których agent nie może zrobić za Ciebie:

1. zainstaluj Xcode i zaloguj się w nim swoim Apple ID (Settings → Accounts),
2. podłącz i odblokuj iPhone'a, włącz na nim Tryb dewelopera.

Potem wklej agentowi ten tekst:

```text
Zainstaluj na moim podłączonym iPhonie aplikację Morfo z https://github.com/2s0lace/morfo (natywna wersja iOS w katalogu ios/).

1. Sprawdź, że `xcodebuild -version` pokazuje Xcode 26 lub nowszy. Jeśli `xcode-select -p` wskazuje na CommandLineTools,
   poproś mnie o uruchomienie: sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
2. Sklonuj repo do ~/Documents/morfo (jeśli go tam nie ma).
3. Znajdź iPhone'a: `xcrun devicectl list devices` (wiersz z "physical"). Jeśli go nie ma, poproś, żebym go podłączył,
   odblokował i zaufał komputerowi.
4. W ios/Morfo.xcodeproj/project.pbxproj zmień PRODUCT_BUNDLE_IDENTIFIER aplikacji (com.patryktaborek.morfo)
   na unikalny, np. com.<moja-nazwa-użytkownika-macOS>.morfo, a DEVELOPMENT_TEAM usuń.
5. Otwórz projekt (`open ios/Morfo.xcodeproj`) i poproś mnie, żebym w Signing & Capabilities targetu Morfo wybrał
   swój Personal Team. Poczekaj, aż potwierdzę.
6. Zbuduj i zainstaluj:
   xcodebuild -project ios/Morfo.xcodeproj -scheme Morfo -configuration Release \
     -destination 'id=<UDID>' -derivedDataPath ios/build -allowProvisioningUpdates build
   xcrun devicectl device install app --device <UDID> ios/build/Build/Products/Release-iphoneos/Morfo.app
   Jeśli build stoi długo, to pewnie macOS pyta o hasło do pęku kluczy dla codesign – każ mi je wpisać i wybrać
   „Zawsze pozwalaj”.
7. Uruchom: xcrun devicectl device process launch --device <UDID> <bundle id>
   Jeśli zgłosi błąd o niezaufanym profilu, każ mi na iPhonie wejść w Ustawienia → Ogólne → VPN i urządzenia
   zarządzane → mój Apple ID → Ufaj, i spróbuj ponownie.
Nie podawaj mi do wpisania haseł w terminalu poza sudo i oknem pęku kluczy; logowanie do Apple robię sam w Xcode.
```

Po 7 dniach wystarczy powiedzieć agentowi: „zainstaluj Morfo ponownie na moim iPhonie” – projekt i podpisywanie są już ustawione.

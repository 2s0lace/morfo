# Morfo

Aplikacja do nauki anatomii zębów stałych. Zamiast płaskich rycin w atlasie pozwala oglądać zęby w pełnym 3D, porównywać je obok siebie i trenować rozpoznawanie przed szpilkami czy kolokwium. Działa jako aplikacja na Androida oraz w przeglądarce (na iPhone'ach, iPadach i komputerach).

[![Pobierz APK](https://img.shields.io/badge/Pobierz-Morfo.apk-2ea44f?style=for-the-badge&logo=android&logoColor=white)](https://github.com/2s0lace/morfo/releases/latest/download/Morfo.apk)
[![Uruchom w przeglądarce](https://img.shields.io/badge/Wersja_Web-Uruchom_w_przegl%C4%85darce-0969da?style=for-the-badge&logo=googlechrome&logoColor=white)](https://2s0lace.github.io/morfo/)
[![Wersja](https://img.shields.io/github/v/release/2s0lace/morfo?style=for-the-badge&label=Wersja&color=6e7681)](https://github.com/2s0lace/morfo/releases/latest)

---

## Co znajdziesz w aplikacji

- **Porównywanie zębów obok siebie** – wybierasz z diagramu dowolne zęby (np. pierwszy i drugi przedtrzonowiec) i obracasz je razem lub osobno, żeby wyłapać różnice w budowie.
- **Prawdziwe proporcje (Wheeler)** – modele są przeskalowane do średnich długości z podręcznika Wheelera (korona + korzeń).
- **Rzuty na powierzchnie** – jeden przycisk ustawia zęby od strony przedsionkowej (wargowej / policzkowej), językowej / podniebiennej, mezjalnej, dystalnej lub żującej / siecznej.
- **Quiz FDI** – ząb pojawia się w losowym obrocie, a Ty wpisujesz numer FDI z klawiatury. Bez podpowiedzi A/B/C/D, jak na egzaminie.
- **Fiszki (Spaced Repetition)** – zęby, z którymi masz problem, wracają częściej; te dobrze opanowane pojawiają się rzadziej.
- **Wariant Carabellego** – opcjonalny model zęba trzonowego z widocznym guzkiem Carabellego.
- **Polski i angielski** – całe nazewnictwo można przełączyć na angielski w ustawieniach.

## Instalacja

1. Pobierz plik [**Morfo.apk**](https://github.com/2s0lace/morfo/releases/latest/download/Morfo.apk).
2. Otwórz go na telefonie lub tablecie.
3. Jeśli system zapyta o zgodę na instalację z tego źródła, zaakceptuj (standard przy instalowaniu plików `.apk` pobranych przez przeglądarkę).
4. Kliknij „Zainstaluj”.

## Sprawdzone urządzenia

Aplikacja wymaga Androida 10 lub nowszego (procesory 64-bit / arm64). Została sprawdzona i działa m.in. na:

- Samsung Galaxy Tab S9 FE ✅
- Samsung Galaxy S25 Edge ✅
- OPPO Find X9 Pro ✅

## Sterowanie

- **Jeden palec**: obrót zęba
- **Dwa palce**: przesuwanie widoku i przybliżanie (zoom)
- **Skręt dwoma palcami**: obrót widoku
- **Przytrzymanie i przeciągnięcie**: przestawienie pojedynczego zęba
- **Podwójne stuknięcie**: wyśrodkowanie widoku

## Modele 3D i źródła

- Skany 3D zębów: University of Dundee, School of Dentistry ([sketchfab.com/DundeeDental](https://sketchfab.com/DundeeDental)), licencja CC BY 4.0.
- Wymiary i opisy cech anatomicznych: *Wheeler’s Dental Anatomy, Physiology and Occlusion*.

---

Dla programistów: architektura, silnik Filament i kompilacja opisane są w [docs/developer.md](docs/developer.md).

# Morfo: 3D tooth morphology comparison

An Android app for comparing permanent teeth side by side as independent, full-detail 3D models.
It targets one device, a Samsung Galaxy S25 Edge (Android 15+, arm64).

## Build and install

```bash
./gradlew :app:assembleRelease          # app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

Requirements: JDK 17+, Android SDK with platform 37. Unit tests: `./gradlew :app:testDebugUnitTest`.
The release build is signed with the debug key, so the APK can be sent directly and installed.

## Stack

Kotlin, Jetpack Compose (UI) and Google Filament + gltfio (rendering). Models are prepared offline by
`tools/asset-pipeline` (see [docs/asset-pipeline.md](docs/asset-pipeline.md)).

## Code layout (`app/src/main/java/app/morfo`)

| Package | Responsibility |
|---|---|
| `dentition` | Tooth metadata: FDI numbers, EN/PL names, class/arch/side, model mapping, average lengths, morphology notes, quick comparisons. No 3D code. |
| `model` | Runtime model metadata (`meta.json`: bounds and support points). |
| `math` | `Vec3`, `Box3`, `Quat`. |
| `workspace` | Comparison state and rules: placed teeth, selection, sync/mirror rotation, surface presets, automatic non-overlapping layout, view settings. |
| `render` | Filament: engine and model cache (`FilamentRuntime`, `ModelRepository`), per-viewport scene (`ViewportRenderer`), camera (`CameraRig`), lighting modes (`StudioLighting`), frame stats. |
| `ui` | Compose screens: tooth selector (arch chart, filters, quick comparisons), comparison workspace (gestures, labels, controls, sheets), about/credits. |

Key design decisions:

- **Every tooth is its own glTF instance with its own transform.** Teeth are never merged. Instances of the
  same model share GPU buffers and textures (up to 4 per upload); right-side teeth reuse the left model mirrored.
- **The camera never rotates.** Teeth rotate, so lights stay fixed relative to the viewer and a surface
  looks the same whenever it is turned to the same view. This is what makes side-by-side comparison fair.
- **Automatic layout is overlap-free by construction.** Teeth are placed by their bounds in the current
  orientation, recomputed every frame. Once the user drags a tooth, layout switches to free mode until
  "Auto-układ" is tapped again.
- **Rendering is on demand.** Frames are drawn only when something changes (static scene means no GPU work).

## Controls

- One finger: rotate (all teeth when sync is on or nothing is selected; otherwise the selected tooth)
- Two fingers: pan and zoom the view; a deliberate twist rotates around the viewing axis
- Long press on a tooth, then drag: move that tooth
- Tap: select or deselect. Double tap: zoom to a tooth, or back to all teeth

## Quiz (`quiz` + `ui/quiz`)

Recognition practice with a typed FDI answer (no multiple choice), like the exam.

- **Round:** 10 / 20 / whole-scope questions, then score, list of mistakes and "repeat mistakes".
- **Flashcards:** endless, Anki-like spaced repetition counted in answers (`Flashcards`): a miss comes
  back after 3 cards, each correct answer pushes the tooth further out (4 → 8 → 18 → 40 … answers).
  Levels persist between sessions.
- **Levels:** Exam (random orientation) or Easy (anatomical position, facial surface to the viewer).
- **Options:** plain colour (no scan texture, shape only); ignore side.
- **Grading** (`Grading`) reports arch / side / tooth separately. After a mistake, "Porównaj z moją"
  opens the comparison view with the shown tooth and the typed one side by side.
- **Statistics:** accuracy per tooth on the arch chart, error types, most frequent confusions
  (stored in SharedPreferences via `QuizStore`).
- During a question, labels, surface presets and variant switches are hidden, because they would reveal the answer.

## Extending

- **Landmarks / annotations / measurements:** tooth-local coordinates are in millimetres in a fixed
  anatomical frame (+Y superior, +Z facial, mesial −X for left teeth), so points can be stored per model id
  and transformed with the tooth's pose (`PlacedTooth.rotation/position/scale`).
- **Other notations (Universal, Palmer):** add formatting next to `Tooth.fdi`; FDI stays the internal key.
- **Deciduous teeth:** new `Tooth` range + models from the pipeline; the renderer is model-agnostic.
- **Saved comparison sets:** `Workspace.refs()` plus per-tooth poses are plain data.
- **Screenshots / cut-away / root transparency:** extend `ViewportRenderer` (Filament `readPixels`,
  clip planes / material parameters).

## Credits

Tooth models: University of Dundee, School of Dentistry (sketchfab.com/DundeeDental), CC BY 4.0.

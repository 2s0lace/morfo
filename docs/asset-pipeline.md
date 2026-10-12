# Model preparation (asset pipeline)

The source scans in `modele/` are **never modified**. `tools/asset-pipeline/build-models.mjs`
reads them and writes runtime copies to `app/src/main/assets/models/`.

```bash
cd tools/asset-pipeline
npm install
node build-models.mjs          # all teeth
node build-models.mjs t24 t36  # selected teeth only
```

Per-tooth settings (source file, target length, calibration) live in `teeth.config.json`.
Every run writes `out-report.json` with before/after statistics.

## What is done to each model

| Step | What | Why |
|---|---|---|
| 1 | Drop meshes whose material is listed in `dropMaterials` (only `t26_carabelli`: two ZBrush sub-tool meshes, 889 + 3 254 vertices, fully enclosed inside the tooth) | Invisible geometry that would still be picked and rendered |
| 2 | Bake the Sketchfab node hierarchy (root matrices with scale 0.115–1.0 and rotations) into the vertex data | One consistent, identity-transform model per tooth |
| 3 | Rotate the principal axis (PCA of all vertices) onto +Y, keeping the source crown direction | Upright teeth; maxillary crowns point down, mandibular crowns up (anatomical position) |
| 4 | Per-tooth yaw from `teeth.config.json` so the facial surface faces +Z | Makes the BUCCAL/LINGUAL/MESIAL/DISTAL/OCCLUSAL presets exact |
| 5 | Centre on the bounding box, scale so the height along Y equals the average tooth length (Wheeler's) | Consistent pivot for rotation; correct proportions between teeth |
| 6 | Remove the `COLOR_0` vertex colours (16 of 17 sources, inconsistent tints; the Carabelli model has none) | Consistent look across teeth; the base colour texture already carries enamel/root colouring; makes the quiz's plain-colour mode really plain |
| 7 | Material: metallic 0, roughness 0.6, double-sided, `KHR_materials_specular` removed | Source materials were glossy (roughness 0.2) and inconsistent; highlights hid morphology |
| 8 | `prune` unused data; write `meta.json` (bounds + 96 extreme points per model) | Lightweight metadata so layout works before geometry is loaded |

**Not done, on purpose:** no decimation, no vertex quantisation, no mesh compression, no LOD,
no texture resizing. The vertex and triangle counts of every runtime model are identical to the
source (except the removed hidden meshes of `t26_carabelli`). Base colour textures (enamel vs
root colouring) and normal maps (fine fissure detail) are kept unchanged.

## Orientation calibration

Canonical frame (millimetres): +Y superior, +Z facial (buccal/labial), +X the patient's left.
All sources are left-side teeth, so mesial is −X. The yaw values were set by rendering each
model from four sides and identifying the facial surface anatomically:

| Tooth | Yaw | Evidence |
|---|---|---|
| 21, 22, 23, 31, 32, 33 | 0° | Lingual fossa / cingulum / lingual ridge already at −Z |
| 24, 25 | −90° | Single root seen from +X (buccal), bifurcation or broad root seen from ±Z (proximal) |
| 26 Carabelli | 180° | Large palatal root and Carabelli cusp on +Z, so the facial side is −Z |
| 26, 27, 28 | −90° | Palatal root seen between the buccal roots from +X |
| 34, 35 | −60° | Narrowest root and no occlusal surface visible (buccal view) at 60° |
| 36, 37, 38 | −90° | Both roots visible from ±X; lingual cusps taller (seen from +X behind the buccal cusps) |

Mesial/distal was cross-checked on 24 (mesial crown concavity), 26 Carabelli (cusp on the
mesiopalatal cusp) and 26/27/36 (root shapes and distal inclination).

## Right-side teeth

Teeth 11–18 and 41–48 have no scans. They reuse the contralateral model (21–28 / 31–38) with a
mirror transform (`scale x = −1`) at runtime, so no extra files or GPU memory are needed. The UI
states this in the tooth info panel.

## Known source limitations

- 31 and 32: the filenames/Sketchfab titles say central/lateral, but the internal node names are
  `LL2`/`LL1`. The app follows the filenames. Relative lengths support that, but it is worth confirming.
- All meshes have small open boundaries (296–924 boundary edges). They render fine (double-sided)
  but will matter for a future cut-away mode.
- Some base-colour textures have visible stripes near the cemento-enamel junction (e.g. 25). This
  comes from the source texture.

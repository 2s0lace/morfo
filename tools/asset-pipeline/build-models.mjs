// Builds runtime copies of the tooth models. Source files in /modele are only read, never written.
//
// Per model:
//   1. drop meshes listed in `dropMaterials` (hidden internal geometry)
//   2. bake the Sketchfab node hierarchy into vertex data
//   3. align the principal (long) axis with +Y, keeping the source crown direction
//      (maxillary crowns point -Y, mandibular crowns +Y = anatomical position)
//   4. apply the per-tooth calibration (yaw so the facial surface faces +Z, optional tilt)
//   5. centre on the bounding box and scale so the height along Y equals the average
//      anatomical length in millimetres
//   6. drop vertex colours (inconsistent between sources; the texture carries the colour)
//   7. normalise the material (non-metallic, matte, double-sided, no specular extension)
//
// Canonical frame (millimetres): +Y superior, +Z facial (buccal/labial), +X patient's left.
// All sources are left-side teeth, so mesial = -X. Right-side teeth are mirrored at runtime.
//
// Geometry is NOT decimated, quantised or compressed. Vertex count and topology are preserved.

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { NodeIO } from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions';
import { transformMesh, prune } from '@gltf-transform/functions';
import * as m from './mat.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const config = JSON.parse(readFileSync(join(here, 'teeth.config.json'), 'utf8'));
const sourceDir = resolve(here, config.sourceDir);
const outputDir = resolve(here, config.outputDir);
mkdirSync(outputDir, { recursive: true });

const io = new NodeIO().registerExtensions(ALL_EXTENSIONS);
const only = process.argv.slice(2);
const report = [];
const runtimeMeta = {};
const round2 = (x) => Math.round(x * 100) / 100;

for (const tooth of config.teeth) {
  if (only.length && !only.includes(tooth.id)) continue;
  const doc = await io.read(join(sourceDir, tooth.source));
  const root = doc.getRoot();
  const scene = root.getDefaultScene() ?? root.listScenes()[0];

  // 1. hidden internal meshes
  const dropped = [];
  for (const mesh of root.listMeshes()) {
    for (const prim of mesh.listPrimitives()) {
      if ((tooth.dropMaterials ?? []).includes(prim.getMaterial()?.getName())) {
        dropped.push({ mesh: mesh.getName(), vertices: prim.getAttribute('POSITION').getCount() });
        mesh.removePrimitive(prim);
        prim.dispose();
      }
    }
  }

  // 2. bake hierarchy
  const meshNodes = [];
  scene.traverse((node) => { if (node.getMesh()) meshNodes.push([node.getMesh(), node.getWorldMatrix()]); });
  for (const [mesh, world] of meshNodes) transformMesh(mesh, world);
  for (const child of scene.listChildren()) scene.removeChild(child);
  for (const node of root.listNodes()) node.dispose();
  const toothNode = doc.createNode(tooth.id);
  scene.addChild(toothNode);
  for (const [mesh] of meshNodes) if (mesh.listPrimitives().length) toothNode.addChild(doc.createNode(mesh.getName()).setMesh(mesh));
  const meshes = meshNodes.map(([mesh]) => mesh).filter((mesh) => mesh.listPrimitives().length);

  // 3 + 4. orientation
  const pca = m.principalAxis(positions(meshes));
  const axis = pca[1] < 0 ? m.scale3(pca, -1) : pca;
  const align = m.rotationBetween(axis, [0, 1, 0]);
  const calib = m.multiply(m.rotZ(tooth.tiltZDeg), m.multiply(m.rotX(tooth.tiltXDeg), m.rotY(tooth.yawDeg)));
  const rotation = m.multiply(calib, align);
  for (const mesh of meshes) transformMesh(mesh, rotation);

  // 5. centre + anatomical scale
  const [min, max] = m.bounds(positions(meshes));
  const height = max[1] - min[1];
  const s = tooth.lengthMm / height;
  const centre = [0, 1, 2].map((i) => (min[i] + max[i]) / 2);
  const place = m.multiply(m.scaling(s), m.translation(m.scale3(centre, -1)));
  for (const mesh of meshes) transformMesh(mesh, place);

  // 6. vertex colours: present in 16 of 17 sources with inconsistent tints; the base colour texture
  //    already carries the enamel/root colouring, and without them a plain colour mode is truly plain.
  let removedVertexColors = false;
  for (const mesh of meshes) {
    for (const prim of mesh.listPrimitives()) {
      if (prim.getAttribute('COLOR_0')) { prim.setAttribute('COLOR_0', null); removedVertexColors = true; }
    }
  }

  // 7. material
  for (const mat of root.listMaterials()) {
    mat.setMetallicFactor(config.material.metallic)
      .setRoughnessFactor(config.material.roughness)
      .setDoubleSided(true)
      .setExtension('KHR_materials_specular', null);
  }
  for (const ext of root.listExtensionsUsed()) if (ext.extensionName === 'KHR_materials_specular') ext.dispose();

  await doc.transform(prune());
  const outFile = join(outputDir, `${tooth.id}.glb`);
  await io.write(outFile, doc);

  const finalPositions = positions(meshes);
  const [fmin, fmax] = m.bounds(finalPositions);
  runtimeMeta[tooth.id] = {
    boundsMin: fmin.map(round2),
    boundsMax: fmax.map(round2),
    support: m.supportPoints(finalPositions, 96).map((p) => p.map(round2)),
  };
  report.push({
    id: tooth.id,
    source: tooth.source,
    output: `${tooth.id}.glb`,
    vertices: meshes.reduce((n, mesh) => n + mesh.listPrimitives().reduce((k, p) => k + p.getAttribute('POSITION').getCount(), 0), 0),
    triangles: meshes.reduce((n, mesh) => n + mesh.listPrimitives().reduce((k, p) => k + (p.getIndices()?.getCount() ?? 0) / 3, 0), 0),
    sourceAxisTiltDeg: +(Math.acos(Math.abs(pca[1])) * 180 / Math.PI).toFixed(1),
    calibration: { yawDeg: tooth.yawDeg, tiltXDeg: tooth.tiltXDeg, tiltZDeg: tooth.tiltZDeg },
    scaleFactor: +s.toFixed(4),
    sizeMm: [0, 1, 2].map((i) => +(fmax[i] - fmin[i]).toFixed(2)),
    dropped,
    removedVertexColors,
  });
  console.log(`${tooth.id.padEnd(14)} ${report.at(-1).triangles} tris  size ${report.at(-1).sizeMm.join(' x ')} mm`);
}

if (!only.length) {
  writeFileSync(join(here, 'out-report.json'), JSON.stringify(report, null, 2));
  // Lightweight runtime metadata: bounds + extreme points used for tight, rotation-aware layout.
  writeFileSync(join(outputDir, 'meta.json'), JSON.stringify(runtimeMeta));
}

function positions(meshes) {
  const out = [];
  const v = [0, 0, 0];
  for (const mesh of meshes) {
    for (const prim of mesh.listPrimitives()) {
      const acc = prim.getAttribute('POSITION');
      for (let i = 0; i < acc.getCount(); i++) out.push([...acc.getElement(i, v)]);
    }
  }
  return out;
}

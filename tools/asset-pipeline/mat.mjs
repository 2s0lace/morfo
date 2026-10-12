// Minimal column-major 4x4 matrix helpers (glTF convention).

export const identity = () => [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1];

export function multiply(a, b) {
  const out = new Array(16).fill(0);
  for (let c = 0; c < 4; c++)
    for (let r = 0; r < 4; r++)
      for (let k = 0; k < 4; k++) out[c * 4 + r] += a[k * 4 + r] * b[c * 4 + k];
  return out;
}

const fromRows3 = (m) => [m[0][0], m[1][0], m[2][0], 0, m[0][1], m[1][1], m[2][1], 0, m[0][2], m[1][2], m[2][2], 0, 0, 0, 0, 1];
const rad = (deg) => (deg * Math.PI) / 180;

export const rotX = (deg) => { const c = Math.cos(rad(deg)), s = Math.sin(rad(deg)); return fromRows3([[1, 0, 0], [0, c, -s], [0, s, c]]); };
export const rotY = (deg) => { const c = Math.cos(rad(deg)), s = Math.sin(rad(deg)); return fromRows3([[c, 0, s], [0, 1, 0], [-s, 0, c]]); };
export const rotZ = (deg) => { const c = Math.cos(rad(deg)), s = Math.sin(rad(deg)); return fromRows3([[c, -s, 0], [s, c, 0], [0, 0, 1]]); };
export const scaling = (s) => [s, 0, 0, 0, 0, s, 0, 0, 0, 0, s, 0, 0, 0, 0, 1];
export const translation = (t) => [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, t[0], t[1], t[2], 1];
export const scale3 = (v, s) => v.map((x) => x * s);

const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
const normalize = (v) => scale3(v, 1 / Math.hypot(...v));

/** Rotation taking unit vector a onto unit vector b (Rodrigues). */
export function rotationBetween(a, b) {
  a = normalize(a); b = normalize(b);
  const v = cross(a, b), c = dot(a, b);
  if (c > 0.999999) return identity();
  const k = 1 / (1 + c);
  return fromRows3([
    [v[0] * v[0] * k + c, v[0] * v[1] * k - v[2], v[0] * v[2] * k + v[1]],
    [v[1] * v[0] * k + v[2], v[1] * v[1] * k + c, v[1] * v[2] * k - v[0]],
    [v[2] * v[0] * k - v[1], v[2] * v[1] * k + v[0], v[2] * v[2] * k + c],
  ]);
}

export function bounds(points) {
  const min = [Infinity, Infinity, Infinity], max = [-Infinity, -Infinity, -Infinity];
  for (const p of points) for (let i = 0; i < 3; i++) { min[i] = Math.min(min[i], p[i]); max[i] = Math.max(max[i], p[i]); }
  return [min, max];
}

/** Dominant eigenvector of the point covariance (power iteration). */
export function principalAxis(points) {
  const c = [0, 0, 0];
  for (const p of points) for (let i = 0; i < 3; i++) c[i] += p[i] / points.length;
  const cov = [[0, 0, 0], [0, 0, 0], [0, 0, 0]];
  for (const p of points) {
    const d = [p[0] - c[0], p[1] - c[1], p[2] - c[2]];
    for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) cov[i][j] += d[i] * d[j];
  }
  let v = normalize([0.1, 1, 0.1]);
  for (let it = 0; it < 500; it++) v = normalize([0, 1, 2].map((i) => dot(cov[i], v)));
  return v;
}

/** Extreme vertices along evenly spread directions; their rotated AABB closely matches the mesh's. */
export function supportPoints(points, directions) {
  const picked = new Set();
  const golden = Math.PI * (3 - Math.sqrt(5));
  for (let i = 0; i < directions; i++) {
    const y = 1 - (2 * (i + 0.5)) / directions, r = Math.sqrt(1 - y * y), a = golden * i;
    const d = [Math.cos(a) * r, y, Math.sin(a) * r];
    let best = 0, bestDot = -Infinity;
    for (let k = 0; k < points.length; k++) {
      const v = dot(points[k], d);
      if (v > bestDot) { bestDot = v; best = k; }
    }
    picked.add(best);
  }
  return [...picked].map((k) => points[k]);
}

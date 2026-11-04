// Morfo 3D Viewport Engine with Three.js
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/OrbitControls.js';
import { GLTFLoader } from 'three/addons/GLTFLoader.js';

export class ToothViewer {
  constructor(canvasContainer) {
    this.container = canvasContainer;
    this.teeth = []; // Array of { fdi, variant, group, pivot, toothData, targetQuat, currentQuat }
    this.loadedModels = new Map(); // cache of loaded GLTF scenes
    this.loadId = 0;
    this.cameraFitFrame = 0;
    this.selectedFdi = null;

    this.initScene();
    this.initLights();
    this.initLoader();
    this.initEvents();

    this.animate = this.animate.bind(this);
    requestAnimationFrame(this.animate);
  }

  initScene() {
    this.scene = new THREE.Scene();
    this.scene.background = new THREE.Color(0x111214);

    const width = this.container.clientWidth || window.innerWidth;
    const height = this.container.clientHeight || window.innerHeight;
    this.viewportWidth = width;
    this.viewportHeight = height;

    this.camera = new THREE.PerspectiveCamera(36, width / height, 0.1, 1000);
    this.camera.position.set(0, 0, 75);

    this.renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' });
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    this.renderer.setSize(width, height, false);
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
    this.renderer.toneMappingExposure = 1.05;
    this.container.appendChild(this.renderer.domElement);

    this.controls = new OrbitControls(this.camera, this.renderer.domElement);
    this.controls.enableDamping = true;
    this.controls.dampingFactor = 0.08;
    this.controls.rotateSpeed = 0.8;
    this.controls.zoomSpeed = 1.0;
    this.controls.panSpeed = 0.8;
    this.controls.minDistance = 15;
    this.controls.maxDistance = 320;
    this.controls.target.set(0, 0, 0);

    // Root group for all tooth objects
    this.teethGroup = new THREE.Group();
    this.scene.add(this.teethGroup);
  }

  initLights() {
    // Clinical studio lighting matching Filament StudioLighting
    const ambient = new THREE.AmbientLight(0xffffff, 1.1);
    this.scene.add(ambient);

    // Key light (front-right-top)
    const keyLight = new THREE.DirectionalLight(0xfff8ee, 2.4);
    keyLight.position.set(30, 40, 50);
    this.scene.add(keyLight);

    // Fill light (front-left-bottom)
    const fillLight = new THREE.DirectionalLight(0xedf4ff, 1.4);
    fillLight.position.set(-35, 15, 35);
    this.scene.add(fillLight);

    // Rim/Back light (emphasizes enamel silhouettes and margins)
    const rimLight = new THREE.DirectionalLight(0xf2eee3, 1.3);
    rimLight.position.set(0, -30, -45);
    this.scene.add(rimLight);
  }

  initLoader() {
    this.loader = new GLTFLoader();
  }

  initEvents() {
    if (window.ResizeObserver) {
      this.resizeObserver = new ResizeObserver(() => this.onResize());
      this.resizeObserver.observe(this.container);
    } else {
      window.addEventListener('resize', () => this.onResize());
    }

    const canvas = this.renderer.domElement;
    // OrbitControls captures the pointer during a drag, including moves outside the canvas.
    canvas.addEventListener('pointermove', (event) => {
      if (!canvas.hasPointerCapture(event.pointerId)) return;
      const bounds = canvas.getBoundingClientRect();
      if (event.clientX < bounds.left || event.clientX >= bounds.right ||
          event.clientY < bounds.top || event.clientY >= bounds.bottom) {
        event.stopImmediatePropagation();
      }
    }, { capture: true });

    // Double tap / double click to center
    canvas.addEventListener('dblclick', () => this.resetView());
  }

  onResize() {
    const width = this.container.clientWidth;
    const height = this.container.clientHeight;
    if (width === 0 || height === 0 ||
        (width === this.viewportWidth && height === this.viewportHeight)) return;

    this.viewportWidth = width;
    this.viewportHeight = height;
    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(width, height, false);
    this.resetView({ preserveAngle: true, animated: false });
  }

  async loadGLTF(modelId) {
    if (this.loadedModels.has(modelId)) {
      return this.loadedModels.get(modelId);
    }
    const path = `./models/${modelId}.glb`;
    return new Promise((resolve, reject) => {
      this.loader.load(
        path,
        (gltf) => {
          this.loadedModels.set(modelId, gltf);
          resolve(gltf);
        },
        undefined,
        (err) => reject(err)
      );
    });
  }

  /**
   * Set the teeth displayed in the scene and report whether all models loaded.
   * @param {Array<{toothData: Object, variant: string}>} items
   * @param {{randomRotation?: boolean}} options
   * @returns {Promise<boolean>} Whether the request is current and every model loaded.
   */
  async setTeeth(items, { randomRotation = false } = {}) {
    const loadId = ++this.loadId;
    cancelAnimationFrame(this.cameraFitFrame);
    // Clear existing
    while (this.teethGroup.children.length > 0) {
      this.teethGroup.remove(this.teethGroup.children[0]);
    }
    this.teeth = [];
    this.onResize();

    if (!items || items.length === 0) {
      this.controls.target.set(0, 0, 0);
      return false;
    }

    const count = items.length;
    const gap = count > 2 ? 14 : 18;
    const totalWidth = (count - 1) * gap;
    const startX = -totalWidth / 2;

    for (let i = 0; i < count; i++) {
      const item = items[i];
      const tooth = item.toothData;
      const isCarabelli = item.variant === 'carabelli' && tooth.hasCarabelli;
      const modelId = isCarabelli ? tooth.carabelliModelId : tooth.modelId;

      try {
        const gltf = await this.loadGLTF(modelId);
        if (loadId !== this.loadId) return false;
        const toothObj = gltf.scene.clone(true);

        // Configure enamel material
        toothObj.traverse((child) => {
          if (child.isMesh) {
            child.material = child.material.clone();
            child.material.roughness = 0.45;
            child.material.metalness = 0.04;
            child.material.side = THREE.DoubleSide; // Critical for mirrored right teeth
          }
        });

        // Compute local bounding box to center geometry at (0, 0, 0)
        const bbox = new THREE.Box3().setFromObject(toothObj);
        const center = bbox.getCenter(new THREE.Vector3());
        toothObj.position.sub(center);

        // Pivot object handles orientation and mirroring
        const pivot = new THREE.Group();
        pivot.add(toothObj);

        // Apply mirroring for right-side teeth
        if (tooth.isMirrored) {
          pivot.scale.set(-1, 1, 1);
        }

        // Wrapper group handles world placement (side-by-side gap)
        const wrapper = new THREE.Group();
        wrapper.position.set(startX + i * gap, 0, 0);
        wrapper.add(pivot);

        this.teethGroup.add(wrapper);

        const toothEntry = {
          fdi: tooth.fdi,
          variant: item.variant || 'standard',
          toothData: tooth,
          wrapper,
          pivot,
          currentQuat: new THREE.Quaternion().copy(pivot.quaternion),
          targetQuat: new THREE.Quaternion().copy(pivot.quaternion),
          animating: false,
          animStart: 0,
          animDuration: 350,
          fromQuat: new THREE.Quaternion()
        };

        this.teeth.push(toothEntry);
      } catch (err) {
        if (loadId !== this.loadId) return false;
        console.error(`Failed to load model for tooth ${tooth.fdi}:`, err);
      }
    }

    this.resetView();
    if (randomRotation && this.teeth.length > 0) {
      this.setRandomRotation();
      setTimeout(() => {
        if (loadId === this.loadId) this.resetView();
      }, 400);
    }
    return this.teeth.length === items.length;
  }

  /**
   * Set surface orientation preset for all active teeth
   * @param {'FACIAL'|'LINGUAL'|'MESIAL'|'DISTAL'|'OCCLUSAL'} preset
   */
  setSurface(preset) {
    for (const item of this.teeth) {
      const t = item.toothData;
      let leftQuat = new THREE.Quaternion();

      switch (preset) {
        case 'FACIAL':
          leftQuat.set(0, 0, 0, 1);
          break;
        case 'LINGUAL':
          leftQuat.setFromAxisAngle(new THREE.Vector3(0, 1, 0), Math.PI);
          break;
        case 'MESIAL':
          leftQuat.setFromAxisAngle(new THREE.Vector3(0, 1, 0), Math.PI / 2);
          break;
        case 'DISTAL':
          leftQuat.setFromAxisAngle(new THREE.Vector3(0, 1, 0), -Math.PI / 2);
          break;
        case 'OCCLUSAL':
          if (!t.isMaxillary) {
            // Mandibular: tip crown towards viewer (+90 deg around X)
            leftQuat.setFromAxisAngle(new THREE.Vector3(1, 0, 0), Math.PI / 2);
          } else {
            // Maxillary: 180 around Z, then -90 around X
            const qz = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(0, 0, 1), Math.PI);
            const qx = new THREE.Quaternion().setFromAxisAngle(new THREE.Vector3(1, 0, 0), -Math.PI / 2);
            leftQuat = qz.multiply(qx);
          }
          break;
      }

      // If mirrored (right tooth), apply mirrorX conjugate: (x, -y, -z, w)
      const target = new THREE.Quaternion();
      if (t.isMirrored) {
        target.set(leftQuat.x, -leftQuat.y, -leftQuat.z, leftQuat.w);
      } else {
        target.copy(leftQuat);
      }

      // Begin smooth slerp animation
      item.fromQuat.copy(item.pivot.quaternion);
      item.targetQuat.copy(target);
      item.animStart = performance.now();
      item.animating = true;
    }
  }

  resetRotation(fdi = null) {
    for (const item of this.teeth) {
      if (fdi !== null && item.fdi !== fdi) continue;
      item.fromQuat.copy(item.pivot.quaternion);
      item.targetQuat.identity();
      item.animStart = performance.now();
      item.animating = true;
    }
  }

  /**
   * Sets a random rotation on the tooth (used in FDI Quiz)
   */
  setRandomRotation() {
    if (this.teeth.length === 0) return;
    const item = this.teeth[0];

    // Uniform random quaternion (Shoemake method)
    const u1 = Math.random();
    const u2 = Math.random() * Math.PI * 2;
    const u3 = Math.random() * Math.PI * 2;
    const a = Math.sqrt(1 - u1);
    const b = Math.sqrt(u1);
    const randomQ = new THREE.Quaternion(
      a * Math.sin(u2),
      a * Math.cos(u2),
      b * Math.sin(u3),
      b * Math.cos(u3)
    ).normalize();

    item.fromQuat.copy(item.pivot.quaternion);
    item.targetQuat.copy(randomQ);
    item.animStart = performance.now();
    item.animating = true;
  }

  resetView({ preserveAngle = false, animated = true } = {}) {
    if (this.teeth.length === 0) return;

    const box = new THREE.Box3().setFromObject(this.teethGroup);
    if (box.isEmpty()) return;

    const center = box.getCenter(new THREE.Vector3());
    const direction = preserveAngle
      ? this.camera.getWorldDirection(new THREE.Vector3()).negate()
      : new THREE.Vector3(0, 0, 1);
    const viewRotation = new THREE.Matrix4().lookAt(direction, new THREE.Vector3(), this.camera.up).transpose();
    const size = box.applyMatrix4(viewRotation).getSize(new THREE.Vector3());
    const halfFovTan = Math.tan(THREE.MathUtils.degToRad(this.camera.fov) / 2);
    const padding = 1.16;

    const distV = (size.y * padding * 0.5) / halfFovTan;
    const distH = (size.x * padding * 0.5) / (halfFovTan * this.camera.aspect);
    const distance = Math.max(distV, distH) + size.z / 2;
    const dist = THREE.MathUtils.clamp(distance, this.controls.minDistance + 1, this.controls.maxDistance);
    const targetPos = center.clone().addScaledVector(direction, dist);

    cancelAnimationFrame(this.cameraFitFrame);
    this.controls.target.copy(center);
    if (!animated) {
      this.camera.position.copy(targetPos);
      return;
    }

    const startPos = this.camera.position.clone();
    const startTime = performance.now();
    const duration = 320;

    const animateReset = () => {
      const elapsed = performance.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const ease = 0.5 - Math.cos(progress * Math.PI) / 2;
      this.camera.position.lerpVectors(startPos, targetPos, ease);
      if (progress < 1) {
        this.cameraFitFrame = requestAnimationFrame(animateReset);
      }
    };
    animateReset();
  }

  animate() {
    requestAnimationFrame(this.animate);

    const now = performance.now();

    // Slerp orientation animations
    for (const item of this.teeth) {
      if (item.animating) {
        const elapsed = now - item.animStart;
        const t = Math.min(elapsed / item.animDuration, 1);
        // Smooth step easing
        const ease = t * t * (3 - 2 * t);
        item.pivot.quaternion.slerpQuaternions(item.fromQuat, item.targetQuat, ease);
        if (t >= 1) {
          item.animating = false;
          item.pivot.quaternion.copy(item.targetQuat);
        }
      }
    }

    this.controls.update();
    this.renderer.render(this.scene, this.camera);
  }
}

// Launch animation: the server's legendary weapons (real Blockbench models from
// assets/weapons.bbmodel) burst out of the Play button in 3D, then fall away.
import * as THREE from '../../node_modules/three/build/three.module.js';

const DURATION = 1.9; // seconds
const COUNT = 18; // weapons per burst, picked at random from the model file
const SIZE = 95; // on-screen size of a weapon, in px

// Blockbench face name -> BoxGeometry material group order.
const FACES = ['east', 'west', 'up', 'down', 'south', 'north'];

// Builds one THREE.Group per top-level outliner group (= one weapon).
async function loadWeapons() {
  const model = await (await fetch('assets/weapons.bbmodel')).json();
  const hidden = new THREE.MeshBasicMaterial({ visible: false });
  const materials = model.textures.map((t) => {
    const tex = new THREE.TextureLoader().load(t.source);
    tex.colorSpace = THREE.SRGBColorSpace;
    tex.magFilter = tex.minFilter = THREE.NearestFilter;
    // Animated textures are vertical strips of square frames: show only frame 1.
    const frames = t.height / t.width;
    tex.repeat.set(1, 1 / frames);
    tex.offset.set(0, 1 - 1 / frames);
    return new THREE.MeshLambertMaterial({ map: tex, alphaTest: 0.1, side: THREE.DoubleSide });
  });
  const uvSize = model.textures.map((t) => [t.uv_width || model.resolution.width, t.uv_height || model.resolution.height]);
  const byUuid = new Map(model.elements.map((e) => [e.uuid, e]));

  return model.outliner.map((group) => {
    const weapon = new THREE.Group();
    weapon.name = group.name;
    for (const id of group.children) {
      const el = byUuid.get(id);
      if (!el || el.type !== 'cube') continue;
      const size = [0, 1, 2].map((i) => el.to[i] - el.from[i]);
      const geo = new THREE.BoxGeometry(...size);
      const uv = geo.attributes.uv;
      const mats = FACES.map((name, f) => {
        const face = el.faces[name];
        if (!face || face.texture == null || !materials[face.texture]) return hidden;
        const [w, h] = uvSize[face.texture];
        const [u0, v0, u1, v1] = face.uv;
        // Same corner order and rotation handling as Blockbench's preview.
        let c = [[u0, v0], [u1, v0], [u0, v1], [u1, v1]];
        for (let r = face.rotation || 0; r > 0; r -= 90) c = [c[2], c[0], c[3], c[1]];
        c.forEach(([u, v], k) => uv.setXY(f * 4 + k, u / w, 1 - v / h));
        return materials[face.texture];
      });
      const mesh = new THREE.Mesh(geo, mats);
      const origin = el.origin || [0, 0, 0];
      geo.translate(...[0, 1, 2].map((i) => (el.from[i] + el.to[i]) / 2 - origin[i]));
      mesh.position.set(...origin);
      if (el.rotation) mesh.rotation.set(...el.rotation.map(THREE.MathUtils.degToRad), 'ZYX');
      weapon.add(mesh);
    }
    // Center the weapon and normalise its size so every weapon flies at the same scale.
    const box = new THREE.Box3().setFromObject(weapon);
    const center = box.getCenter(new THREE.Vector3());
    const span = Math.max(...box.getSize(new THREE.Vector3()).toArray()) || 1;
    weapon.children.forEach((m) => m.position.sub(center));
    const pivot = new THREE.Group();
    pivot.add(weapon);
    weapon.scale.setScalar(1 / span);
    return pivot;
  });
}

const weaponsReady = loadWeapons();
weaponsReady.catch((err) => console.error('Could not load weapon models:', err));

let renderer = null;

function easeOutBack(x) {
  const c = 2.2;
  return 1 + (c + 1) * (x - 1) ** 3 + c * (x - 1) ** 2;
}

// Plays the burst from the centre of `button`; resolves when it is finished.
async function weaponBurst(button) {
  const all = await weaponsReady;
  const W = window.innerWidth;
  const H = window.innerHeight;
  if (!renderer) {
    renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
    renderer.domElement.id = 'burst-canvas';
    document.body.appendChild(renderer.domElement);
  }
  renderer.setPixelRatio(window.devicePixelRatio);
  renderer.setSize(W, H);
  renderer.domElement.hidden = false;

  // Orthographic camera in CSS pixels (y up), so the maths below is in px.
  const camera = new THREE.OrthographicCamera(0, W, 0, -H, -2000, 2000);
  const scene = new THREE.Scene();
  scene.add(new THREE.AmbientLight(0xffffff, 1.6));
  const sun = new THREE.DirectionalLight(0xffffff, 2.2);
  sun.position.set(0.4, 1, 1);
  scene.add(sun);

  const rect = button.getBoundingClientRect();
  const ox = rect.left + rect.width / 2;
  const oy = -(rect.top + rect.height / 2);
  const picks = [...all].sort(() => Math.random() - 0.5).slice(0, COUNT);
  const flyers = picks.map((obj, i) => {
    // Spread evenly round a circle, biased upward so most weapons fly over the UI.
    const angle = (i / picks.length) * Math.PI * 2 + Math.random() * 0.3;
    const speed = 650 + Math.random() * 550;
    obj.rotation.set(Math.random() * 6, Math.random() * 6, Math.random() * 6);
    scene.add(obj);
    return {
      obj,
      delay: i * 0.015,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed * 0.8 + 350,
      spin: new THREE.Vector3(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).multiplyScalar(16),
      size: SIZE * (0.8 + Math.random() * 0.5),
    };
  });

  button.classList.add('bursting');
  const start = performance.now();
  await new Promise((resolve) => {
    let last = start;
    function frame(now) {
      const dt = (now - last) / 1000;
      last = now;
      for (const f of flyers) {
        const t = (now - start) / 1000 - f.delay;
        if (t < 0) { f.obj.scale.setScalar(0); continue; }
        // Ballistic path with drag, so they shoot out fast then hang and fall.
        const drag = (1 - Math.exp(-2.2 * t)) / 2.2;
        f.obj.position.set(ox + f.vx * drag, oy + f.vy * drag - 420 * t * t, 0);
        f.obj.rotation.x += f.spin.x * dt;
        f.obj.rotation.y += f.spin.y * dt;
        f.obj.rotation.z += f.spin.z * dt;
        const grow = easeOutBack(Math.min(t / 0.3, 1));
        const shrink = Math.min(Math.max((DURATION - t) / 0.35, 0), 1);
        f.obj.scale.setScalar(Math.max(f.size * grow * shrink, 0.0001));
      }
      renderer.render(scene, camera);
      if ((now - start) / 1000 < DURATION) requestAnimationFrame(frame);
      else resolve();
    }
    requestAnimationFrame(frame);
  });
  button.classList.remove('bursting');
  renderer.domElement.hidden = true;
  picks.forEach((obj) => scene.remove(obj));
}

window.weaponBurst = weaponBurst;

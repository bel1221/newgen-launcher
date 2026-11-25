//! Launch animation (README "Launch animation"): the server's legendary weapons, real Blockbench
//! models from `assets/newgen/weapons.bbmodel`, burst out of the PLAY button in 3D, then fall away.
//!
//! A port of the Electron launcher's three.js version. three.js is not needed for this: the
//! camera is orthographic, so each cube face lands on the screen as a parallelogram and is drawn
//! as two textured triangles with Skia, nearest-neighbour sampled, back to front, lit like
//! three.js's Lambert material under the same ambient and directional light.

use std::collections::HashMap;
use std::f32::consts::PI;
use std::sync::Arc;
use std::time::Instant;

use base64::Engine as _;
use serde::Deserialize;
use skia_safe::vertices::VertexMode;
use skia_safe::{BlendMode, Canvas, Data, FilterMode, Image, MipmapMode, Paint, Point, SamplingOptions, Shader, TileMode, Vertices};

pub const DURATION: f32 = 1.9; // seconds
const COUNT: usize = 18; // weapons per burst, picked at random from the model file
const SIZE: f32 = 95.; // on-screen size of a weapon, in px

// Blockbench face name in three.js BoxGeometry group order, with that face's plane:
// (u axis, v axis, w axis, u direction, v direction, w sign)
const FACES: [(&str, usize, usize, usize, f32, f32, f32); 6] = [
    ("east", 2, 1, 0, -1., -1., 1.),
    ("west", 2, 1, 0, 1., -1., -1.),
    ("up", 0, 2, 1, 1., 1., 1.),
    ("down", 0, 2, 1, 1., -1., -1.),
    ("south", 0, 1, 2, 1., -1., 1.),
    ("north", 0, 1, 2, -1., -1., -1.),
];

pub type V3 = [f32; 3];

#[derive(Deserialize)]
struct Model {
    resolution: Resolution,
    textures: Vec<TextureDef>,
    elements: Vec<ElementDef>,
    outliner: Vec<serde_json::Value>,
}

#[derive(Deserialize)]
struct Resolution {
    width: f32,
    height: f32,
}

#[derive(Deserialize)]
struct TextureDef {
    source: String,
    width: Option<f32>,
    height: Option<f32>,
    uv_width: Option<f32>,
    uv_height: Option<f32>,
}

#[derive(Deserialize)]
struct ElementDef {
    uuid: String,
    #[serde(rename = "type", default)]
    kind: Option<String>,
    from: Option<V3>,
    to: Option<V3>,
    origin: Option<V3>,
    rotation: Option<V3>,
    #[serde(default)]
    faces: HashMap<String, FaceDef>,
}

#[derive(Deserialize)]
struct FaceDef {
    uv: Option<[f32; 4]>,
    texture: Option<serde_json::Value>,
    rotation: Option<f32>,
}

/// One textured cube face of a weapon, in the weapon's own space (centred, longest side 1).
struct Quad {
    corners: [V3; 4],
    uvs: [Point; 4],
    normal: V3,
    texture: usize,
}

pub struct Weapon {
    quads: Vec<Quad>,
    /// The outliner group name, tidied for display ("corrupted_crossbow" → "Corrupted Crossbow")
    name: String,
}

pub struct WeaponSet {
    weapons: Vec<Weapon>,
    /// One nearest-neighbour shader per texture, made once
    shaders: Vec<Option<Shader>>,
}

fn rot_x(v: V3, a: f32) -> V3 {
    let (s, c) = a.sin_cos();
    [v[0], v[1] * c - v[2] * s, v[1] * s + v[2] * c]
}
fn rot_y(v: V3, a: f32) -> V3 {
    let (s, c) = a.sin_cos();
    [v[0] * c + v[2] * s, v[1], -v[0] * s + v[2] * c]
}
fn rot_z(v: V3, a: f32) -> V3 {
    let (s, c) = a.sin_cos();
    [v[0] * c - v[1] * s, v[0] * s + v[1] * c, v[2]]
}
/// three.js Euler order 'ZYX': R = Rz * Ry * Rx
fn euler_zyx(v: V3, r: V3) -> V3 {
    rot_z(rot_y(rot_x(v, r[0]), r[1]), r[2])
}
/// three.js default Euler order 'XYZ': R = Rx * Ry * Rz
fn euler_xyz(v: V3, r: V3) -> V3 {
    rot_x(rot_y(rot_z(v, r[2]), r[1]), r[0])
}
fn sub(a: V3, b: V3) -> V3 {
    [a[0] - b[0], a[1] - b[1], a[2] - b[2]]
}
fn cross(a: V3, b: V3) -> V3 {
    [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
}
fn normalize(v: V3) -> V3 {
    let len = (v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).sqrt().max(1e-6);
    [v[0] / len, v[1] / len, v[2] / len]
}

fn decode_texture(source: &str) -> Option<Image> {
    let b64 = source.split_once("base64,")?.1;
    let bytes = base64::engine::general_purpose::STANDARD.decode(b64.trim()).ok()?;
    let image = Image::from_encoded(Data::new_copy(&bytes))?;
    Some(image.make_raster_image(None, None).unwrap_or(image))
}

impl WeaponSet {
    /// Builds one weapon per top-level outliner group (nested sub-groups are ignored).
    pub fn load(json: &[u8]) -> Result<Self, String> {
        let model: Model = serde_json::from_slice(json).map_err(|e| e.to_string())?;
        let textures: Vec<Option<Image>> = model.textures.iter().map(|t| decode_texture(&t.source)).collect();
        // Texture pixels per UV unit. Animated textures are vertical strips of square frames:
        // only frame 1 is shown, so v runs over one frame (as tall as the texture is wide).
        let uv_scale: Vec<(f32, f32)> = model
            .textures
            .iter()
            .zip(&textures)
            .map(|(def, image)| {
                let (w, h) = image.as_ref().map_or((def.width.unwrap_or(16.), def.height.unwrap_or(16.)), |i| (i.width() as f32, i.height() as f32));
                let frame_h = w.min(h);
                let uw = def.uv_width.unwrap_or(model.resolution.width);
                let uh = def.uv_height.unwrap_or(model.resolution.height);
                (w / uw, frame_h / uh)
            })
            .collect();
        let by_uuid: HashMap<&str, &ElementDef> = model.elements.iter().map(|e| (e.uuid.as_str(), e)).collect();

        let mut weapons = Vec::new();
        for group in &model.outliner {
            let Some(children) = group.get("children").and_then(|c| c.as_array()) else { continue };
            let mut quads = Vec::new();
            // Bounds over every corner of every cube, textured or not (three.js Box3.setFromObject)
            let (mut lo, mut hi) = ([f32::MAX; 3], [f32::MIN; 3]);
            for child in children {
                let Some(el) = child.as_str().and_then(|id| by_uuid.get(id)) else { continue };
                if el.kind.as_deref().is_some_and(|k| k != "cube") {
                    continue;
                }
                let (Some(from), Some(to)) = (el.from, el.to) else { continue };
                let size = [to[0] - from[0], to[1] - from[1], to[2] - from[2]];
                let origin = el.origin.unwrap_or([0.; 3]);
                let offset = [0, 1, 2].map(|i| (from[i] + to[i]) / 2. - origin[i]);
                let rotation = el.rotation.unwrap_or([0.; 3]).map(f32::to_radians);
                let place = |local: V3| {
                    let moved = [local[0] + offset[0], local[1] + offset[1], local[2] + offset[2]];
                    let turned = euler_zyx(moved, rotation);
                    [turned[0] + origin[0], turned[1] + origin[1], turned[2] + origin[2]]
                };
                for k in 0..8 {
                    let p = place([0, 1, 2].map(|i| if k >> i & 1 == 1 { size[i] / 2. } else { -size[i] / 2. }));
                    for i in 0..3 {
                        lo[i] = lo[i].min(p[i]);
                        hi[i] = hi[i].max(p[i]);
                    }
                }
                for &(name, u, v, w, udir, vdir, wsign) in &FACES {
                    let Some(face) = el.faces.get(name) else { continue };
                    let Some(texture) = face.texture.as_ref().and_then(serde_json::Value::as_u64).map(|t| t as usize) else { continue };
                    if textures.get(texture).is_none_or(Option::is_none) {
                        continue;
                    }
                    let Some([u0, v0, u1, v1]) = face.uv else { continue };
                    // BoxGeometry's plane: vertex k = (ix, iy) with k = iy * 2 + ix
                    let corners = [(0., 0.), (1., 0.), (0., 1.), (1., 1.)].map(|(ix, iy): (f32, f32)| {
                        let mut p = [0.; 3];
                        p[u] = (ix - 0.5) * size[u] * udir;
                        p[v] = (iy - 0.5) * size[v] * vdir;
                        p[w] = size[w] / 2. * wsign;
                        place(p)
                    });
                    // Same corner order and rotation handling as Blockbench's preview
                    let mut c = [(u0, v0), (u1, v0), (u0, v1), (u1, v1)];
                    let mut r = face.rotation.unwrap_or(0.);
                    while r > 0. {
                        c = [c[2], c[0], c[3], c[1]];
                        r -= 90.;
                    }
                    let (sx, sy) = uv_scale[texture];
                    let uvs = c.map(|(u, v)| Point::new(u * sx, v * sy));
                    let normal = normalize(cross(sub(corners[1], corners[0]), sub(corners[2], corners[0])));
                    quads.push(Quad { corners, uvs, normal: [-normal[0], -normal[1], -normal[2]], texture });
                }
            }
            if quads.is_empty() {
                continue;
            }
            // Centre the weapon and normalise its size so every weapon flies at the same scale
            let center = [0, 1, 2].map(|i| (lo[i] + hi[i]) / 2.);
            let span = (0..3).map(|i| hi[i] - lo[i]).fold(0f32, f32::max).max(1e-3);
            for q in &mut quads {
                for p in &mut q.corners {
                    *p = [0, 1, 2].map(|i| (p[i] - center[i]) / span);
                }
            }
            let name = display_name(group.get("name").and_then(serde_json::Value::as_str).unwrap_or_default());
            weapons.push(Weapon { quads, name });
        }
        if weapons.is_empty() {
            return Err("no weapons in the model file".into());
        }
        let sampling = SamplingOptions::new(FilterMode::Nearest, MipmapMode::None);
        let shaders = textures.iter().map(|t| t.as_ref()?.to_shader((TileMode::Clamp, TileMode::Clamp), sampling, None)).collect();
        Ok(Self { weapons, shaders })
    }
}

/// A tiny random source (xorshift), enough for picking weapons and throws.
struct Rng(u64);
impl Rng {
    fn new() -> Self {
        let seed = std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).map_or(0x9E37_79B9, |d| d.as_nanos() as u64);
        Self(seed | 1)
    }
    fn next(&mut self) -> f32 {
        self.0 ^= self.0 << 13;
        self.0 ^= self.0 >> 7;
        self.0 ^= self.0 << 17;
        (self.0 >> 40) as f32 / (1u64 << 24) as f32
    }
}

struct Flyer {
    weapon: usize,
    delay: f32,
    vx: f32,
    vy: f32,
    rotation: V3,
    spin: V3,
    size: f32,
}

/// One burst, started from the centre of the PLAY button (window coordinates).
pub struct Burst {
    set: Arc<WeaponSet>,
    start: Instant,
    origin: (f32, f32),
    flyers: Vec<Flyer>,
}

fn ease_out_back(x: f32) -> f32 {
    let c = 2.2;
    1. + (c + 1.) * (x - 1.).powi(3) + c * (x - 1.).powi(2)
}

impl Burst {
    pub fn new(set: Arc<WeaponSet>, origin: (f32, f32)) -> Self {
        let mut rng = Rng::new();
        let mut order: Vec<usize> = (0..set.weapons.len()).collect();
        for i in (1..order.len()).rev() {
            let j = ((rng.next() * (i + 1) as f32) as usize).min(i);
            order.swap(i, j);
        }
        order.truncate(COUNT);
        let n = order.len() as f32;
        let flyers = order
            .into_iter()
            .enumerate()
            .map(|(i, weapon)| {
                // Spread evenly round a circle, biased upward so most weapons fly over the UI
                let angle = i as f32 / n * PI * 2. + rng.next() * 0.3;
                let speed = 650. + rng.next() * 550.;
                Flyer {
                    weapon,
                    delay: i as f32 * 0.015,
                    vx: angle.cos() * speed,
                    vy: angle.sin() * speed * 0.8 + 350.,
                    rotation: [rng.next() * 6., rng.next() * 6., rng.next() * 6.],
                    spin: [(rng.next() - 0.5) * 16., (rng.next() - 0.5) * 16., (rng.next() - 0.5) * 16.],
                    size: SIZE * (0.8 + rng.next() * 0.5),
                }
            })
            .collect();
        Self { set, start: Instant::now(), origin, flyers }
    }

    pub fn finished(&self) -> bool {
        self.start.elapsed().as_secs_f32() >= DURATION
    }

    /// Draws the burst as it is now. Window coordinates, y down.
    pub fn draw(&self, canvas: &Canvas) {
        let now = self.start.elapsed().as_secs_f32();
        let mut items = Vec::with_capacity(self.flyers.len());
        for f in &self.flyers {
            let t = now - f.delay;
            if t < 0. {
                continue;
            }
            // Ballistic path with drag, so they shoot out fast then hang and fall
            let drag = (1. - (-2.2 * t).exp()) / 2.2;
            let x = self.origin.0 + f.vx * drag;
            let y = self.origin.1 - f.vy * drag + 420. * t * t;
            let rotation = [0, 1, 2].map(|i| f.rotation[i] + f.spin[i] * t);
            let grow = ease_out_back((t / 0.3).min(1.));
            let shrink = ((DURATION - t) / 0.35).clamp(0., 1.);
            items.push((f.weapon, (x, y), rotation, (f.size * grow * shrink).max(0.0001)));
        }
        self.set.draw(canvas, &items);
    }
}

/// "corrupted_crossbow" → "Corrupted Crossbow"; "Jingu Bang (jingubang_2)" → "Jingu Bang".
fn display_name(raw: &str) -> String {
    let base = raw.split(" (").next().unwrap_or(raw).replace('_', " ");
    base.split_whitespace()
        .map(|word| {
            let mut chars = word.chars();
            chars.next().map_or_else(String::new, |first| first.to_uppercase().chain(chars).collect())
        })
        .collect::<Vec<_>>()
        .join(" ")
}

impl WeaponSet {
    pub fn len(&self) -> usize {
        self.weapons.len()
    }

    pub fn name(&self, weapon: usize) -> &str {
        self.weapons.get(weapon).map_or("", |w| w.name.as_str())
    }

    /// Draws weapons, each `(index, centre in canvas px (y down), rotation, size in px)`, lit
    /// and depth-sorted together (orthographic camera, three.js Lambert look).
    pub fn draw(&self, canvas: &Canvas, items: &[(usize, (f32, f32), V3, f32)]) {
        let light = normalize([0.4, 1., 1.]);
        // Projected faces: (depth, texture, screen corners, uvs, shade)
        let mut faces: Vec<(f32, usize, [Point; 4], [Point; 4], u8)> = Vec::new();
        for &(weapon, (cx, cy), rotation, scale) in items {
            let Some(weapon) = self.weapons.get(weapon) else { continue };
            for q in &weapon.quads {
                let world = q.corners.map(|c| {
                    let r = euler_xyz(c, rotation);
                    [r[0] * scale, r[1] * scale, r[2] * scale]
                });
                let mut n = euler_xyz(q.normal, rotation);
                if n[2] < 0. {
                    n = [-n[0], -n[1], -n[2]]; // DoubleSide: the side facing the camera is lit
                }
                // three.js Lambert, physically correct lights: (ambient + direct * N·L) / π,
                // computed in linear light, shown in sRGB
                let dot = (n[0] * light[0] + n[1] * light[1] + n[2] * light[2]).max(0.);
                let linear = ((1.6 + 2.2 * dot) / PI).min(1.);
                let shade = (linear.powf(1. / 2.2) * 255.).round() as u8;
                let depth = world.iter().map(|p| p[2]).sum::<f32>() / 4.;
                let screen = world.map(|p| Point::new(cx + p[0], cy - p[1]));
                faces.push((depth, q.texture, screen, q.uvs, shade));
            }
        }
        // Back to front
        faces.sort_by(|a, b| a.0.total_cmp(&b.0));

        let mut i = 0;
        while i < faces.len() {
            let texture = faces[i].1;
            let mut positions = Vec::new();
            let mut uvs = Vec::new();
            let mut colors = Vec::new();
            let mut indices: Vec<u16> = Vec::new();
            while i < faces.len() && faces[i].1 == texture && positions.len() + 4 < u16::MAX as usize {
                let (_, _, screen, tex, shade) = &faces[i];
                let base = positions.len() as u16;
                positions.extend_from_slice(screen);
                uvs.extend_from_slice(tex);
                colors.extend([skia_safe::Color::from_rgb(*shade, *shade, *shade); 4]);
                indices.extend_from_slice(&[base, base + 1, base + 2, base + 2, base + 1, base + 3]);
                i += 1;
            }
            let Some(Some(shader)) = self.shaders.get(texture) else { continue };
            let vertices = Vertices::new_copy(VertexMode::Triangles, &positions, &uvs, &colors, Some(&indices));
            let mut paint = Paint::default();
            paint.set_shader(shader.clone());
            canvas.draw_vertices(&vertices, BlendMode::Modulate, &paint);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn shipped_weapons_load() {
        let bytes = crate::ui::assets::Assets::get_bytes("newgen/weapons.bbmodel").expect("model is embedded");
        let set = WeaponSet::load(&bytes).expect("model loads");
        assert_eq!(set.weapons.len(), 49);
        for weapon in &set.weapons {
            for q in &weapon.quads {
                for p in &q.corners {
                    assert!(p.iter().all(|v| v.abs() <= 0.5001), "weapon is centred and normalised");
                }
            }
        }
    }

    #[test]
    fn box_faces_point_outward() {
        // A unit cube at the origin: every face normal points away from the centre
        let json = br#"{"resolution":{"width":16,"height":16},
            "textures":[{"source":"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="}],
            "elements":[{"uuid":"a","type":"cube","from":[0,0,0],"to":[2,2,2],"origin":[1,1,1],
              "faces":{"north":{"uv":[0,0,1,1],"texture":0},"south":{"uv":[0,0,1,1],"texture":0},"east":{"uv":[0,0,1,1],"texture":0},
                       "west":{"uv":[0,0,1,1],"texture":0},"up":{"uv":[0,0,1,1],"texture":0},"down":{"uv":[0,0,1,1],"texture":0}}}],
            "outliner":[{"name":"cube","children":["a"]}]}"#;
        let set = WeaponSet::load(json).unwrap();
        let quads = &set.weapons[0].quads;
        assert_eq!(quads.len(), 6);
        for q in quads {
            let center = [0, 1, 2].map(|i| q.corners.iter().map(|c| c[i]).sum::<f32>() / 4.);
            let outward = center[0] * q.normal[0] + center[1] * q.normal[1] + center[2] * q.normal[2];
            assert!(outward > 0., "normal {:?} at face centre {:?}", q.normal, center);
        }
    }
}

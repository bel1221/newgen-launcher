//! The bottom row of the Play screen (docs/ui-spec.md "Site cards"): one card per section of
//! newgenmc.net, opening that section in the browser. The legendary-weapons card holds a live 3D
//! weapon from `weapons.bbmodel`: it spins slowly, speeds up on hover, can be dragged round, and a
//! click without dragging shows the next weapon.
//!
//! The sections are bundled (`assets/newgen/site/sections.json` + images): the site renders its
//! text from a JavaScript dictionary, so there is nothing stable to read from its HTML.

use std::cell::{Cell, RefCell};
use std::collections::HashMap;
use std::rc::Rc;
use std::sync::LazyLock;
use std::time::{Duration, Instant};

use freya::animation::use_animation_transition;
use freya::prelude::*;
use serde::Deserialize;
use skia_safe::{Data, Image};

use super::app::{self, EDGE, text, use_lang};
use super::assets::Assets;
use super::components::{Icon, IconType, step};
use super::theme::{self, with_alpha as a};
use crate::i18n::{Lang, S};

const HEIGHT: f32 = 148.;
/// The text strip at the bottom of every card
const STRIP: f32 = 60.;

#[derive(Deserialize)]
struct Texts {
    vi: String,
    en: String,
}

impl Texts {
    fn get(&self, lang: Lang) -> &str {
        if lang == Lang::Vi { &self.vi } else { &self.en }
    }
}

#[derive(Deserialize)]
struct Section {
    title: Texts,
    text: Texts,
    url: String,
    image: Option<String>,
    #[serde(default)]
    weapons: bool,
}

fn parse_sections(json: &[u8]) -> Vec<Section> {
    serde_json::from_slice(json).unwrap_or_default()
}

static SECTIONS: LazyLock<Vec<Section>> =
    LazyLock::new(|| Assets::get_bytes("newgen/site/sections.json").map(|bytes| parse_sections(&bytes)).unwrap_or_default());

/// Each card image decoded once.
static IMAGES: LazyLock<HashMap<String, Image>> = LazyLock::new(|| {
    SECTIONS
        .iter()
        .filter_map(|s| s.image.clone())
        .filter_map(|path| {
            let bytes = Assets::get_bytes(&path)?;
            let image = Image::from_encoded(Data::new_copy(&bytes))?;
            Some((path, image.make_raster_image(None, None).unwrap_or(image)))
        })
        .collect()
});

fn open_url(url: &str) {
    // https only, like the navbar links
    match url::Url::parse(url) {
        Ok(parsed) if parsed.scheme() == "https" => {
            if let Err(err) = open::that(parsed.as_str()) {
                tracing::warn!("could not open {url}: {err}");
            }
        }
        _ => tracing::warn!("not opening {url}: not an https address"),
    }
}

#[derive(PartialEq)]
pub struct SiteCards {}

impl Component for SiteCards {
    fn render(&self) -> impl IntoElement {
        let mut row = rect()
            .position(Position::new_absolute().left(EDGE).bottom(EDGE))
            .width(Size::px(app::WIDTH - 2. * EDGE))
            .height(Size::px(HEIGHT))
            .horizontal()
            .content(Content::Flex)
            .spacing(16.);
        for (index, section) in SECTIONS.iter().enumerate() {
            row = row.child(if section.weapons { WeaponCard { index }.into_element() } else { ImageCard { index }.into_element() });
        }
        row
    }
}

fn card(hover: f32) -> Rect {
    rect()
        .width(Size::flex(1.))
        .height(Size::fill())
        .corner_radius(theme::rounded(12.))
        .background(a(theme::page_elevated(), 190))
        .border(
            Border::new()
                .fill(if hover > 0.5 { theme::component_border_hover() } else { theme::component_border() })
                .width(1.)
                .alignment(BorderAlignment::Inner),
        )
        .overflow(Overflow::Clip)
        .offset_y(-2. * hover)
}

/// Title (with an arrow: it opens the site) and one line under it, on a fade to dark.
fn strip(title: &str, line: &str) -> Rect {
    rect()
        .position(Position::new_absolute().left(0.).bottom(0.))
        .width(Size::fill())
        .height(Size::px(STRIP + 20.))
        // Above the card picture (equal depths have no paint order)
        .layer(Layer::Relative(1))
        .main_align(Alignment::End)
        .padding(Gaps::new(0., 14., 12., 14.))
        .spacing(2.)
        .background(LinearGradient::new().angle(0.).stop((a(theme::page(), 0), 0.)).stop((a(theme::page(), 225), 45.)))
        .child(
            rect()
                .horizontal()
                .width(Size::fill())
                .content(Content::Flex)
                .cross_align(Alignment::Center)
                .spacing(6.)
                .child(
                    text(title.to_string(), 15., FontWeight::SEMI_BOLD, theme::on_media())
                        .width(Size::flex(1.))
                        .max_lines(1)
                        .text_overflow(TextOverflow::Ellipsis),
                )
                .child(Icon::new(IconType::ArrowRight).size(16.).color(a(theme::on_media(), 200))),
        )
        .child(text(line.to_string(), 13., FontWeight::NORMAL, a(theme::on_media(), 200)).max_lines(1).text_overflow(TextOverflow::Ellipsis))
}

// ---------- Image card ----------

#[derive(PartialEq)]
struct ImageCard {
    index: usize,
}

impl Component for ImageCard {
    fn render(&self) -> impl IntoElement {
        let lang = use_lang();
        let section = &SECTIONS[self.index];
        let mut hovered = use_state(|| false);
        // Linked card motion: lift, brighter border, image 1.03 (600 ms in / 400 ms out)
        let motion = use_animation_transition(hovered, |_, on: bool| step(on, if on { 600 } else { 400 }));
        let hover = motion.get().value();
        let image = section.image.as_ref().and_then(|path| IMAGES.get(path)).cloned();
        let url = section.url.clone();

        card(hover)
            .cursor(CursorIcon::Pointer)
            .a11y_role(AccessibilityRole::Link)
            .a11y_alt(section.title.get(lang).to_string())
            .on_pointer_over(move |_| hovered.set_if_modified(true))
            .on_pointer_out(move |_| hovered.set_if_modified(false))
            .on_press(move |_| open_url(&url))
            .maybe_child(image.map(|image| {
                rect().width(Size::fill()).height(Size::fill()).background(RenderCallback::new(move |ctx: &mut FillRenderContext| {
                    let RenderTarget::Background { canvas, .. } = &ctx.target else { return };
                    let (w, h) = (ctx.size.width, ctx.size.height);
                    let zoom = 1. + 0.03 * hover;
                    canvas.save();
                    canvas.translate((w / 2., h / 2.));
                    canvas.scale((zoom, zoom));
                    canvas.translate((-w / 2., -h / 2.));
                    app::draw_cover(canvas, &image, w, h, 1.);
                    canvas.restore();
                }))
            }))
            .child(strip(section.title.get(lang), section.text.get(lang)))
    }
}

// ---------- Weapon card ----------

const IDLE_SPEED: f32 = 0.6; // rad/s
const HOVER_SPEED: f32 = 1.8;
const IDLE_PITCH: f32 = 0.35;
const TILT: f32 = -0.55; // a diagonal pose in the card
const DRAG_SENSITIVITY: f32 = 0.012; // rad per px
const SIZE: f32 = 96.;
const FRAME: Duration = Duration::from_millis(33); // ~30 fps

struct Motion {
    yaw: f32,
    pitch: f32,
    speed: f32,
    /// 0..1, eased over ~200 ms
    hover: f32,
    hovered: bool,
    /// Pointer down: (start x, start y, yaw, pitch) and whether it has moved enough to be a drag
    drag: Option<(f64, f64, f32, f32)>,
    moved: bool,
    time: f32,
    /// The weapon area in window px (x0, y0, x1, y1); hover is worked out from it, because the
    /// card changes every frame and pointer over/out events on it are not steady
    area: (f64, f64, f64, f64),
}

impl Motion {
    fn new() -> Self {
        Self { yaw: 0.6, pitch: IDLE_PITCH, speed: IDLE_SPEED, hover: 0., hovered: false, drag: None, moved: false, time: 0., area: (0., 0., 0., 0.) }
    }

    fn advance(&mut self, dt: f32) {
        let ease = |tau: f32| 1. - (-dt / tau).exp();
        let target = if self.hovered || self.drag.is_some() { 1. } else { 0. };
        self.hover += (target - self.hover) * ease(0.07);
        if self.drag.is_none() {
            // Released: the spin builds up again and the pitch settles back
            let speed = IDLE_SPEED + (HOVER_SPEED - IDLE_SPEED) * self.hover;
            self.speed += (speed - self.speed) * ease(0.25);
            self.yaw += self.speed * dt;
            self.pitch += (IDLE_PITCH - self.pitch) * ease(0.4);
        } else {
            self.speed = 0.;
        }
        self.time += dt;
    }
}

#[derive(PartialEq)]
struct WeaponCard {
    index: usize,
}

impl Component for WeaponCard {
    fn render(&self) -> impl IntoElement {
        let lang = use_lang();
        let section = &SECTIONS[self.index];
        let mut weapon = use_state(|| 0usize);
        let mut frame = use_state(|| 0u32);
        let motion = use_hook(|| Rc::new(RefCell::new(Motion::new())));

        // The clock: ~30 redraws a second of this card only, none while minimized
        use_hook({
            let motion = motion.clone();
            move || {
                let window = Platform::window_id();
                let minimized = Rc::new(Cell::new(false));
                spawn(async move {
                    let mut last = Instant::now();
                    loop {
                        tokio::time::sleep(FRAME).await;
                        let flag = minimized.clone();
                        Platform::get().with_window(window, move |w| flag.set(w.is_minimized().unwrap_or(false)));
                        let now = Instant::now();
                        let dt = (now - last).as_secs_f32().min(0.1);
                        last = now;
                        if minimized.get() {
                            continue;
                        }
                        motion.borrow_mut().advance(dt);
                        *frame.write() += 1;
                    }
                });
            }
        });
        let _ = frame.read();

        let set = app::weapons();
        let count = set.as_ref().map_or(0, |s| s.len());
        let current = if count > 0 { *weapon.read() % count } else { 0 };
        let name = set.as_ref().map(|s| s.name(current).to_string()).unwrap_or_default();
        let (yaw, pitch, hover, bob) = {
            let m = motion.borrow();
            (m.yaw, m.pitch, m.hover, (m.time * 1.6).sin() * 3.)
        };

        let draw = RenderCallback::new(move |ctx: &mut FillRenderContext| {
            let RenderTarget::Background { canvas, .. } = &ctx.target else { return };
            let Some(set) = &set else { return };
            let (w, h) = (ctx.size.width, ctx.size.height);
            let scale = SIZE * (1. + 0.08 * hover);
            set.draw(canvas, &[(current, (w / 2., h / 2. + bob), [pitch, yaw, TILT], scale)]);
        });

        let (down, moving, up) = (motion.clone(), motion.clone(), motion.clone());
        let sized = motion.clone();
        let weapon_area = rect()
            .position(Position::new_absolute().left(0.).top(0.))
            .width(Size::fill())
            .height(Size::px(HEIGHT - STRIP + 4.))
            .cursor(CursorIcon::Grab)
            // The drawing changes every frame, so it sits in its own child that takes no pointer
            // events; this element stays the same and keeps a steady hover
            .child(rect().width(Size::fill()).height(Size::fill()).interactive(false).background(draw))
            .on_sized(move |e: Event<SizedEventData>| {
                let a = e.area;
                sized.borrow_mut().area = (f64::from(a.min_x()), f64::from(a.min_y()), f64::from(a.max_x()), f64::from(a.max_y()));
            })
            .on_pointer_down(move |e: Event<PointerEventData>| {
                let mut m = down.borrow_mut();
                // Only after the pointer moved in: the Log in click that mounts this screen sits
                // where the card appears and must not count as a click on it
                if !m.hovered {
                    return;
                }
                let at = e.global_location();
                m.drag = Some((at.x, at.y, m.yaw, m.pitch));
                m.moved = false;
            })
            .on_global_pointer_move(move |e: Event<PointerEventData>| {
                let mut m = moving.borrow_mut();
                let at = e.global_location();
                let (x0, y0, x1, y1) = m.area;
                m.hovered = (x0..x1).contains(&at.x) && (y0..y1).contains(&at.y);
                let Some((x0, y0, yaw0, pitch0)) = m.drag else { return };
                let (dx, dy) = ((at.x - x0) as f32, (at.y - y0) as f32);
                if dx.abs() + dy.abs() > 4. {
                    m.moved = true;
                }
                if m.moved {
                    m.yaw = yaw0 + dx * DRAG_SENSITIVITY;
                    m.pitch = (pitch0 + dy * DRAG_SENSITIVITY).clamp(-1.3, 1.3);
                }
            })
            // Global, so a release outside the card still ends the drag
            .on_global_pointer_up(move |_| {
                let mut m = up.borrow_mut();
                if m.drag.take().is_some() && !m.moved && count > 0 {
                    weapon.set((current + 1) % count);
                }
            });

        let url = section.url.clone();
        let link = strip(section.title.get(lang), &name)
            .cursor(CursorIcon::Pointer)
            .a11y_role(AccessibilityRole::Link)
            .a11y_alt(format!("{} · {}", section.title.get(lang), lang.text(S::OpenSection)))
            .on_press(move |_| open_url(&url));

        card(hover).child(link).child(weapon_area)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn bundled_sections_parse_with_https_links_and_images() {
        let bytes = Assets::get_bytes("newgen/site/sections.json").expect("sections are embedded");
        let sections = parse_sections(&bytes);
        assert!((3..=4).contains(&sections.len()));
        assert_eq!(sections.iter().filter(|s| s.weapons).count(), 1, "one weapons card");
        for s in &sections {
            assert!(!s.title.vi.is_empty() && !s.title.en.is_empty() && !s.text.vi.is_empty() && !s.text.en.is_empty());
            assert!(url::Url::parse(&s.url).is_ok_and(|u| u.scheme() == "https"), "{}", s.url);
            if let Some(path) = &s.image {
                let bytes = Assets::get_bytes(path).expect("image is embedded");
                assert!(Image::from_encoded(Data::new_copy(&bytes)).is_some(), "{path} decodes");
            }
        }
    }

    #[test]
    fn motion_spins_on_idle_and_faster_on_hover() {
        let mut m = Motion::new();
        let yaw = m.yaw;
        for _ in 0..30 {
            m.advance(1. / 30.);
        }
        assert!((m.yaw - yaw - IDLE_SPEED).abs() < 0.05, "about 0.6 rad in a second");
        m.hovered = true;
        for _ in 0..30 {
            m.advance(1. / 30.);
        }
        assert!(m.hover > 0.99 && m.speed > 1.5);
        m.drag = Some((0., 0., m.yaw, m.pitch));
        let held = m.yaw;
        m.advance(0.1);
        assert_eq!(m.yaw, held, "no spin while held");
    }
}

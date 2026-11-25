//! The window: the shared layers (video, scrim, vignette, version, burst), the navbar and the
//! two screens (docs/ui-spec.md).

use std::cell::Cell;
use std::rc::Rc;
use std::sync::{Arc, OnceLock};
use std::time::Duration;

use freya::animation::{AnimNum, Function, OnChange, OnCreation, use_animation_transition, use_animation_with_dependencies};
use freya::prelude::*;
use skia_safe::{FilterMode, Image, MipmapMode, Paint, Rect as SkRect, SamplingOptions};
use tokio::sync::mpsc::UnboundedReceiver;

use super::assets::Assets;
use super::components::{Button, Icon, IconType, step};
use super::home::Home;
use super::login::Login;
use super::theme::{self, with_alpha as a};
use crate::auth::Session;
use crate::burst::{Burst, WeaponSet};
use crate::config::{VERSION, config};
use crate::game::{EventSender, LauncherEvent};
use crate::i18n::{Lang, S, Text};
use crate::video::SharedPicture;

pub const WIDTH: f32 = 1280.;
pub const HEIGHT: f32 = 720.;
/// The one content line on both sides
pub const EDGE: f32 = theme::HOME_PADDING_PX;
const LOG_CAP: usize = 2000;
pub const LOG_LINE: f32 = 16.;
const LOCKUP_RATIO: f32 = 3655. / 900.; // lockup.png's width / height

static SENDER: OnceLock<EventSender> = OnceLock::new();
static WEAPONS: OnceLock<Option<Arc<WeaponSet>>> = OnceLock::new();

pub fn sender() -> EventSender {
    SENDER.get().expect("ui::app::Launcher::new was called").clone()
}

/// The weapon models, loaded once off the UI thread; None until ready or if they failed.
pub fn weapons() -> Option<Arc<WeaponSet>> {
    WEAPONS.get().cloned().flatten()
}

#[derive(Clone, Copy, PartialEq, Eq)]
pub enum PlayState {
    Idle,
    Launching,
    Playing,
}

/// Everything both the event pump and the screens read or write.
#[derive(Clone, Copy, PartialEq)]
pub struct Shared {
    pub session: State<Option<Session>>,
    pub status: State<Option<Text>>,
    pub progress: State<Option<f32>>,
    pub play: State<PlayState>,
    pub error: State<Option<Text>>,
    pub logs: State<Vec<LogRow>>,
    /// Lines ever written, so the log view rebuilds on each new one
    pub log_count: State<u64>,
    pub log_open: State<bool>,
    /// The settings panel; only one of it and the game log is open at a time
    pub settings_open: State<bool>,
    /// Set when the game crashed: the last log rows, shown in the crash panel
    pub crash: State<Option<Vec<String>>>,
    pub log_scroll: ScrollController,
    pub burst: State<Option<Rc<Burst>>>,
    /// Play button centre, window coordinates
    pub play_centre: State<(f32, f32)>,
}

pub struct Launcher {
    events: Cell<Option<UnboundedReceiver<LauncherEvent>>>,
    picture: SharedPicture,
}

impl Launcher {
    pub fn new(events: UnboundedReceiver<LauncherEvent>, sender: EventSender, picture: SharedPicture) -> Self {
        let _ = SENDER.set(sender);
        std::thread::spawn(|| {
            let set = Assets::get_bytes("newgen/weapons.bbmodel")
                .ok_or_else(|| "missing".to_string())
                .and_then(|bytes| WeaponSet::load(&bytes))
                .inspect_err(|err| tracing::warn!("launch animation disabled: {err}"))
                .ok()
                .map(Arc::new);
            let _ = WEAPONS.set(set);
        });
        Self { events: Cell::new(Some(events)), picture }
    }
}

/// Entrance progress of one part: ease-out quart over `dur` ms from `start` ms.
pub fn reveal(t: f32, start: f32, dur: f32) -> f32 {
    let x = ((t - start) / dur).clamp(0., 1.);
    1. - (1. - x).powi(4)
}

/// A block of the screen entrance: rises `dy` px and fades in.
pub fn enter(el: Rect, t: f32, start: f32, dur: f32, dy: f32) -> Rect {
    let p = reveal(t, start, dur);
    el.offset_y(dy * (1. - p)).opacity(p)
}

pub fn lockup(height: f32) -> ImageViewer {
    ImageViewer::new(Assets::get_bytes("newgen/lockup.png").unwrap_or_default())
        .loading_placeholder(rect())
        .width(Size::px(height * LOCKUP_RATIO))
        .height(Size::px(height))
}

pub fn text(value: impl Into<std::borrow::Cow<'static, str>>, size: f32, weight: FontWeight, color: Color) -> Label {
    label().text(value).font_size(size).font_weight(weight).color(color)
}

pub fn border() -> Border {
    Border::new().fill(theme::component_border()).width(1.).alignment(BorderAlignment::Inner)
}

/// The current language; reading it re-renders the calling component when it changes.
pub fn use_lang() -> Lang {
    *use_consume::<State<Lang>>().read()
}

/// Switches the language everywhere at once and remembers it.
pub fn set_language(mut state: State<Lang>, lang: Lang) {
    if *state.peek() != lang {
        crate::i18n::set_lang(lang);
        crate::settings::save_lang(lang.code());
        state.set(lang);
    }
}

/// "EN | VI": two 13px Medium segments; the active one on `component_bg()`.
/// The language state, to switch it (see `set_language`).
pub fn use_lang_state() -> State<Lang> {
    use_consume::<State<Lang>>()
}

pub fn lang_toggle(state: State<Lang>) -> Rect {
    let lang = *state.read();
    let segment = |value: Lang, name: &'static str| {
        let active = lang == value;
        rect()
            .padding(Gaps::new_symmetric(4., 8.))
            .corner_radius(theme::rounded(6.))
            .background(if active { theme::component_bg_pressed() } else { Color::TRANSPARENT })
            .cursor(CursorIcon::Pointer)
            .a11y_role(AccessibilityRole::Button)
            .a11y_alt(name)
            .on_press(move |_| set_language(state, value))
            .child(text(name, 13., FontWeight::MEDIUM, if active { theme::fg_strong() } else { theme::fg_secondary() }))
    };
    rect()
        .horizontal()
        .cross_align(Alignment::Center)
        .padding(2.)
        .corner_radius(theme::rounded(8.))
        .background(a(theme::page_elevated(), 160))
        .border(border())
        .child(segment(Lang::En, "EN"))
        .child(segment(Lang::Vi, "VI"))
}

impl App for Launcher {
    fn render(&self) -> impl IntoElement {
        // The language, for every component through use_lang(); switching re-renders at once
        let lang = use_state(crate::i18n::lang);
        use_provide_context(|| lang);
        let shared = Shared {
            session: use_state(|| None),
            status: use_state(|| None),
            progress: use_state(|| None),
            play: use_state(|| PlayState::Idle),
            error: use_state(|| None),
            logs: use_state(Vec::new),
            log_count: use_state(|| 0),
            log_open: use_state(|| false),
            settings_open: use_state(|| false),
            crash: use_state(|| None),
            log_scroll: use_scroll_controller(ScrollConfig::default),
            burst: use_state(|| None),
            play_centre: use_state(|| (0., 0.)),
        };
        let events = self.events.take();
        use_hook(move || {
            if let Some(events) = events {
                spawn(pump(events, shared));
            }
        });

        let player = shared.session.read().as_ref().map(|s| s.username.clone());
        let on_home = player.is_some();
        // One entrance clock per screen mount: 0 → 1100 ms, linear; each part eases itself
        let entrance = use_animation_with_dependencies(&on_home, |conf, _| {
            conf.on_creation(OnCreation::Run);
            conf.on_change(OnChange::Rerun);
            AnimNum::new(0., 1100.).time(1100).function(Function::Linear)
        });
        let t = entrance.get().value();

        rect()
            .width(Size::fill())
            .height(Size::fill())
            .font_family(theme::DEFAULT_FONT)
            .color(theme::fg_primary())
            .child(Background { picture: self.picture.clone(), scale: 1.05 - 0.05 * reveal(t, 0., 1100.) })
            // Freya paints deeper elements on top and leaves equal depths unordered, so the
            // backdrop layers sit on explicit layers below everything else.
            .child(
                rect()
                    .position(Position::new_absolute())
                    .width(Size::fill())
                    .height(Size::fill())
                    .layer(Layer::Relative(-30))
                    .interactive(false)
                    .background(a(theme::page(), 40)),
            )
            .child(
                rect()
                    .position(Position::new_absolute())
                    .width(Size::fill())
                    .height(Size::fill())
                    .layer(Layer::Relative(-20))
                    .interactive(false)
                    .child(gradient_overlay_radial()),
            )
            // Text guard: darkens only the left side, behind the hero and the login card
            .child(
                rect()
                    .position(Position::new_absolute())
                    .width(Size::px(720.))
                    .height(Size::fill())
                    .layer(Layer::Relative(-15))
                    .interactive(false)
                    .background(LinearGradient::new().angle(-90.).stop((a(theme::page(), 170), 0.)).stop((a(theme::page(), 0), 100.))),
            )
            // The version would sit under the site cards on Home
            .maybe_child((!on_home).then(|| {
                rect()
                    .position(Position::new_absolute().bottom(16.).right(EDGE))
                    .child(text(format!("v{VERSION}"), 13., FontWeight::NORMAL, a(theme::fg_secondary(), 160)))
            }))
            .child(if on_home {
                Home { shared, t }.into_element()
            } else {
                Login { session: shared.session, t }.into_element()
            })
            .child(Navbar { player, session: shared.session })
            .maybe_child(shared.burst.read().clone().map(|burst| BurstLayer { burst, slot: shared.burst }))
    }
}

/// Window-side end of `game`'s events: status line, progress bar, game log, game end.
async fn pump(mut events: UnboundedReceiver<LauncherEvent>, s: Shared) {
    let Shared { mut status, mut progress, mut play, .. } = s;
    while let Some(event) = events.recv().await {
        match event {
            LauncherEvent::Status(line) => {
                status.set(Some(line));
                progress.set(None);
            }
            // Dropped once the launch is past downloading or failed (a late event would stick)
            LauncherEvent::Progress { .. } if !crate::game::accepting_progress() => {}
            LauncherEvent::Progress { task, current, total } => {
                status.set(Some(Text::raw(task)));
                progress.set(Some(current as f32 / total.max(1) as f32));
            }
            LauncherEvent::Log(line) => push_log(s, line),
            LauncherEvent::GameClosed(code, crashed) => {
                if crashed {
                    let rows = s.logs.peek().iter().rev().take(CRASH_LINES).rev().map(|row| row.text.clone()).collect();
                    let mut crash = s.crash;
                    crash.set(Some(rows));
                }
                status.set(Some(match code {
                    Some(c) => Text::key(S::GameClosed, &[&c]),
                    None => Text::with_word(S::GameClosed, S::Unknown),
                }));
                progress.set(None);
                play.set(PlayState::Idle);
            }
        }
    }
}

/// Characters per game log row: the 520 px panel minus padding, in 12 px JetBrains Mono
const LOG_COLS: usize = 68;
/// Log rows shown in the crash panel
const CRASH_LINES: usize = 20;

/// One row of the game log.
#[derive(Clone, Debug, PartialEq)]
pub struct LogRow {
    pub text: String,
    /// The entry this row belongs to mentions an error; every wrapped row of it shares this
    pub error: bool,
}

/// One game output line as log rows: tabs as 4 spaces (the font draws a box for a tab), long
/// lines wrapped onto indented rows (the log is a list of equal rows). The error colour is
/// decided once per entry, before wrapping.
fn log_rows(line: &str) -> Vec<LogRow> {
    let error = line.contains("ERROR") || line.contains("Exception");
    let chars: Vec<char> = line.replace('\t', "    ").chars().collect();
    let mut rows = vec![chars[..chars.len().min(LOG_COLS)].iter().collect::<String>()];
    if chars.len() > LOG_COLS {
        for chunk in chars[LOG_COLS..].chunks(LOG_COLS - 4) {
            rows.push(format!("    {}", chunk.iter().collect::<String>()));
        }
    }
    rows.into_iter().map(|text| LogRow { text, error }).collect()
}

pub fn push_log(s: Shared, line: String) {
    let Shared { mut logs, mut log_count, mut log_scroll, .. } = s;
    // Follows new lines unless the player scrolled up (judged on the content before this line)
    let bounds = *log_scroll.bounds.peek();
    let stuck = bounds.is_none_or(|(content, viewport)| log_scroll.position().y <= viewport.height - content.height + 1.);
    let count = {
        let mut lines = logs.write();
        lines.extend(log_rows(&line));
        let over = lines.len().saturating_sub(LOG_CAP);
        lines.drain(..over);
        lines.len()
    };
    *log_count.write() += 1;
    if let Some((content, viewport)) = bounds {
        // The view only measures the longer content on its next layout; without this the jump
        // below would be clamped to the old end.
        log_scroll.apply_layout(Size2D::new(content.width, count as f32 * LOG_LINE), viewport);
    }
    if stuck {
        log_scroll.scroll_to(ScrollPosition::End, Direction::Vertical);
    }
}

// ---------- Background ----------

#[derive(Clone)]
struct Background {
    picture: SharedPicture,
    scale: f32,
}

impl PartialEq for Background {
    fn eq(&self, other: &Self) -> bool {
        Arc::ptr_eq(&self.picture, &other.picture) && self.scale == other.scale
    }
}

impl Component for Background {
    fn render(&self) -> impl IntoElement {
        let mut generation = use_state(|| 0u64);
        let picture = self.picture.clone();
        use_hook(move || {
            // Woken by the video thread on each new picture (a 16 ms timer ticked every ~31 ms
            // on Windows and dropped frames).
            let Ok(changed) = picture.lock().map(|p| p.changed.clone()) else { return };
            spawn(async move {
                loop {
                    changed.notified().await;
                    let latest = picture.lock().map_or(0, |p| p.generation);
                    if latest != *generation.peek() {
                        generation.set(latest);
                    }
                }
            });
        });
        let _ = generation.read(); // a new video frame redraws this layer

        let picture = self.picture.clone();
        let draw = RenderCallback::new(move |ctx: &mut FillRenderContext| {
            let RenderTarget::Background { canvas, .. } = &ctx.target else { return };
            let Some((front, fading)) = picture.lock().ok().map(|p| (p.front.clone(), p.fading.clone())) else { return };
            let (w, h) = (ctx.size.width, ctx.size.height);
            // shortcut: no poster still; with no playable video the plain page() ground shows
            if let Some(image) = front {
                draw_cover(canvas, &image, w, h, 1.);
            }
            if let Some((image, alpha)) = fading {
                draw_cover(canvas, &image, w, h, alpha);
            }
        });

        rect()
            .position(Position::new_absolute())
            .width(Size::fill())
            .height(Size::fill())
            .layer(Layer::Relative(-40))
            .background(theme::page())
            .overflow(Overflow::Clip)
            .interactive(false)
            .child(rect().width(Size::fill()).height(Size::fill()).scale(self.scale).background(draw))
    }
}

/// Cover-fit: scaled to fill w×h, centred, the overflow cropped by the layer's clip.
pub(super) fn draw_cover(canvas: &skia_safe::Canvas, image: &Image, w: f32, h: f32, alpha: f32) {
    let (iw, ih) = (image.width() as f32, image.height() as f32);
    if iw <= 0. || ih <= 0. {
        return;
    }
    let scale = (w / iw).max(h / ih);
    let (dw, dh) = (iw * scale, ih * scale);
    let mut paint = Paint::default();
    paint.set_alpha_f(alpha);
    canvas.draw_image_rect_with_sampling_options(
        image,
        None,
        SkRect::from_xywh((w - dw) / 2., (h - dh) / 2., dw, dh),
        SamplingOptions::new(FilterMode::Linear, MipmapMode::None),
        &paint,
    );
}

// Copied from OneLauncher `layout/app_shell.rs` (GPL-3.0-only)
const VIGNETTE_SPOTLIGHT_SHADER: &str = r#"
uniform float2 iResolution;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;

    float2 center = float2(0.80, 0.45);
    float2 distVec = uv - center;

    float2 dir = normalize(distVec + float2(0.00001));

    float leftScale = 1.8;
    float rightScale = 4.5;
    float scaleX = mix(leftScale, rightScale, smoothstep(-1.0, 1.0, dir.x));

    float topScale = 2.2;
    float bottomScale = 2.2;
    float scaleY = mix(topScale, bottomScale, smoothstep(-1.0, 1.0, dir.y));

    distVec.x *= scaleX;
    distVec.y *= scaleY;

    float dist = length(distVec);

    float vignette = smoothstep(0.0, 1.2, dist);
    vignette = pow(vignette, 1.3);

    float maxDarkness = 0.65;
    float alpha = vignette * maxDarkness;

    return half4(0.0, 0.0, 0.0, alpha);
}
"#;

fn gradient_overlay_radial() -> impl IntoElement {
    let effect = use_hook(|| {
        freya::engine::prelude::RuntimeEffect::make_for_shader(VIGNETTE_SPOTLIGHT_SHADER, None)
            .expect("Failed to compile vignette shader")
    });

    rect()
        .width(Size::fill())
        .height(Size::fill())
        .position(Position::new_absolute())
        .background(ShaderFill::new(
            VIGNETTE_SPOTLIGHT_SHADER,
            effect,
            move |effect, bounds| {
                let w = bounds.width();
                let h = bounds.height();

                #[rustfmt::skip]
                let uniforms: [f32; 2] = [w, h];

                let bytes = unsafe {
                    std::slice::from_raw_parts(
                        uniforms.as_ptr() as *const u8,
                        std::mem::size_of_val(&uniforms),
                    )
                };

                effect.make_shader(freya::engine::prelude::Data::new_copy(bytes), &[], None)
            },
        ))
}

// ---------- Burst ----------

#[derive(Clone)]
struct BurstLayer {
    burst: Rc<Burst>,
    slot: State<Option<Rc<Burst>>>,
}

impl PartialEq for BurstLayer {
    fn eq(&self, other: &Self) -> bool {
        Rc::ptr_eq(&self.burst, &other.burst)
    }
}

impl Component for BurstLayer {
    fn render(&self) -> impl IntoElement {
        let mut frame = use_state(|| 0u32);
        let mut slot = self.slot;
        use_hook(move || {
            spawn(async move {
                loop {
                    tokio::time::sleep(Duration::from_millis(16)).await;
                    // The hook outlives a burst replaced by a newer one: follow whatever the slot holds
                    let finished = slot.peek().as_ref().map(|burst| burst.finished());
                    if finished != Some(false) {
                        if finished.is_some() {
                            slot.set(None);
                        }
                        break;
                    }
                    *frame.write() += 1;
                }
            });
        });
        let _ = frame.read();

        let burst = self.burst.clone();
        rect()
            .position(Position::new_absolute().top(0.).left(0.))
            .width(Size::px(WIDTH))
            .height(Size::px(HEIGHT))
            .layer(Layer::OverlayLevel(16)) // the highest level Freya has
            .interactive(false)
            .background(RenderCallback::new(move |ctx: &mut FillRenderContext| {
                if let RenderTarget::Background { canvas, .. } = &ctx.target {
                    burst.draw(canvas);
                }
            }))
    }
}

// ---------- Navbar ----------

#[derive(PartialEq)]
struct Navbar {
    player: Option<String>,
    session: State<Option<Session>>,
}

impl Component for Navbar {
    fn render(&self) -> impl IntoElement {
        let config = config();
        let lang = use_lang_state();
        let links = [("discord", IconType::Discord, S::Discord), ("website", IconType::Globe, S::Website)];
        let mut right = rect().horizontal().spacing(8.).cross_align(Alignment::Center);
        if let Some(player) = &self.player {
            right = right
                .child(rect().margin(Gaps::new(0., 4., 0., 0.)).child(text(player.clone(), 14., FontWeight::MEDIUM, theme::on_media())))
                .child(NavIcon { icon: IconType::LogOut, size: 18., name: S::LogOut, pill: true, action: NavAction::LogOut(self.session) })
                .child(rect().width(Size::px(1.)).height(Size::px(20.)).margin(Gaps::new_symmetric(0., 8.)).background(a(theme::fg_primary(), 40)));
        }
        right = right.child(rect().margin(Gaps::new(0., 4., 0., 0.)).child(lang_toggle(lang)));
        for (key, icon, name) in links {
            if config.link_url(key).is_some() {
                right = right.child(NavIcon { icon, size: 20., name, pill: true, action: NavAction::Link(key) });
            }
        }
        right = right
            .child(NavIcon { icon: IconType::Minus, size: 18., name: S::Minimize, pill: false, action: NavAction::Minimize })
            .child(NavIcon { icon: IconType::X, size: 18., name: S::Close, pill: false, action: NavAction::Close });

        rect()
            .position(Position::new_absolute().top(0.).left(0.))
            .width(Size::px(WIDTH))
            .height(Size::px(theme::NAVBAR_HEIGHT_PX))
            .layer(Layer::OverlayLevel(2))
            .child(
                // Drag strip. Not `window_drag()`: its double click maximizes, and this window is fixed.
                rect()
                    .position(Position::new_absolute().top(0.).left(0.))
                    .width(Size::px(WIDTH))
                    .height(Size::px(theme::NAVBAR_HEIGHT_PX))
                    .on_pointer_down(|event: Event<PointerEventData>| {
                        if event.button() == Some(MouseButton::Left) {
                            Platform::get().with_window(Platform::window_id(), |window| {
                                let _ = window.drag_window();
                            });
                        }
                    }),
            )
            .child(
                rect()
                    .width(Size::fill())
                    .height(Size::fill())
                    .horizontal()
                    .content(Content::Flex)
                    .cross_align(Alignment::Center)
                    .padding(Gaps::new_symmetric(0., EDGE))
                    .child(
                        rect().width(Size::flex(1.)).child(
                            ImageViewer::new(Assets::get_bytes("newgen/icon.png").unwrap_or_default())
                                .loading_placeholder(rect())
                                .width(Size::px(32.))
                                .height(Size::px(32.)),
                        ),
                    )
                    .child(right),
            )
    }
}

#[derive(Clone, Copy, PartialEq)]
enum NavAction {
    LogOut(State<Option<Session>>),
    Link(&'static str),
    Minimize,
    Close,
}

impl NavAction {
    fn run(self) {
        match self {
            Self::LogOut(mut session) => session.set(None),
            Self::Link(name) => {
                if let Some(url) = config().link_url(name)
                    && let Err(err) = open::that(&url)
                {
                    tracing::warn!("could not open {url}: {err}");
                }
            }
            Self::Minimize => Platform::get().with_window(Platform::window_id(), |window| window.set_minimized(true)),
            Self::Close => drop(Platform::get().post_callback(|ctx| ctx.exit())),
        }
    }
}

/// Ghost 36×36 icon button; with `pill`, its name fades in below after 150 ms of hover.
#[derive(PartialEq)]
struct NavIcon {
    icon: IconType,
    size: f32,
    name: S,
    pill: bool,
    action: NavAction,
}

impl Component for NavIcon {
    fn render(&self) -> impl IntoElement {
        let mut hovered = use_state(|| false);
        let mut shown = use_state(|| false);
        let fade = use_animation_transition(shown, |_, on: bool| step(on, if on { 150 } else { 120 }));
        let opacity = fade.get().value();
        let action = self.action;
        let pill = self.pill;
        let name = use_lang().text(self.name);

        rect()
            .width(Size::px(36.))
            .height(Size::px(36.))
            .child(
                Button::new()
                    .ghost()
                    .icon()
                    .width(Size::px(36.))
                    .height(Size::px(36.))
                    .alt(name)
                    .on_hover(move |on: bool| {
                        hovered.set_if_modified(on);
                        if !on {
                            shown.set_if_modified(false);
                        } else if pill {
                            spawn(async move {
                                tokio::time::sleep(Duration::from_millis(150)).await;
                                if hovered() {
                                    shown.set_if_modified(true);
                                }
                            });
                        }
                    })
                    .on_press(move |_| action.run())
                    .child(Icon::new(self.icon).size(self.size).color(theme::on_media())),
            )
            .maybe_child((pill && opacity > 0.).then(|| {
                // A 100 px box centred under the 36 px button holds the pill centred
                rect()
                    .position(Position::new_absolute().top(42.).left(-32.))
                    .width(Size::px(100.))
                    .horizontal()
                    .main_align(Alignment::Center)
                    .layer(Layer::OverlayLevel(3))
                    .interactive(false)
                    .opacity(opacity)
                    .child(
                        rect()
                            .padding(Gaps::new_symmetric(4., 8.))
                            .corner_radius(theme::rounded(8.))
                            .background(theme::page_elevated())
                            .border(border())
                            .child(text(name, 13., FontWeight::MEDIUM, theme::fg_primary())),
                    )
            }))
    }
}

#[cfg(test)]
mod tests {
    use super::{LOG_COLS, log_rows};

    #[test]
    fn log_rows_expand_tabs_and_wrap() {
        let texts = |line: &str| log_rows(line).into_iter().map(|r| r.text).collect::<Vec<_>>();
        assert_eq!(texts("\tat net.minecraft.Foo"), vec!["    at net.minecraft.Foo"]);
        let long = "x".repeat(LOG_COLS * 2);
        let rows = texts(&long);
        assert!(rows.iter().all(|r| r.chars().count() <= LOG_COLS));
        assert_eq!(rows.concat().replace(' ', ""), long);
        assert!(rows[1].starts_with("    "));
    }

    #[test]
    fn wrapped_rows_share_their_entry_colour() {
        // "ERROR" lands on the middle row of a three-row entry: the whole entry is red
        let error = format!("[INFO] {} ERROR {}", "a".repeat(LOG_COLS), "b".repeat(LOG_COLS));
        let rows = log_rows(&error);
        assert!(rows.len() >= 3 && rows.iter().all(|r| r.error));
        // A plain INFO entry that wraps stays grey on every row
        let info = format!("[INFO] {}", "c".repeat(LOG_COLS * 3));
        assert!(log_rows(&info).iter().all(|r| !r.error));
    }
}

//! OneLauncher's basic controls: Button, Icon and TextInput.
//! Adapted from OneLauncher (GPL-3.0-only), `packages/oneclient_app/src/components`
//! (`button.rs`, `icons.rs`, `text_input.rs`), without its tooltips, translations and themes.

use std::borrow::Cow;

use freya::animation::{AnimNum, Ease, Function, use_animation_transition};
use freya::prelude::*;

use super::assets::Assets;
use super::theme::{self, rounded};

// ---------- Icon ----------

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum IconType {
    AlertCircle,
    AlertTriangle,
    ArrowRight,
    Discord,
    Folder,
    Globe,
    LogOut,
    Maximize,
    Minimize,
    Minus,
    Play,
    Settings,
    Terminal,
    X,
}

impl IconType {
    fn path(self) -> &'static str {
        match self {
            Self::AlertCircle => "icons/alert-circle.svg",
            Self::AlertTriangle => "icons/alert-triangle.svg",
            Self::ArrowRight => "icons/arrow-right.svg",
            Self::Discord => "icons/discord.svg",
            Self::Folder => "icons/folder.svg",
            Self::Globe => "icons/globe-01.svg",
            Self::LogOut => "icons/log-out-01.svg",
            Self::Maximize => "icons/maximize-01.svg",
            Self::Minimize => "icons/minimize-01.svg",
            Self::Minus => "icons/minus.svg",
            Self::Play => "icons/play.svg",
            Self::Settings => "icons/settings-01.svg",
            Self::Terminal => "icons/terminal.svg",
            Self::X => "icons/x.svg",
        }
    }
}

#[derive(PartialEq)]
pub struct Icon {
    icon: IconType,
    size: f32,
    color: Option<Color>,
}

impl Icon {
    pub fn new(icon: IconType) -> Self {
        Self { icon, size: 24., color: None }
    }
    pub fn size(mut self, size: f32) -> Self {
        self.size = size;
        self
    }
    pub fn color(mut self, color: Color) -> Self {
        self.color = Some(color);
        self
    }
}

impl Component for Icon {
    fn render(&self) -> impl IntoElement {
        let path = self.icon.path();
        let bytes = Assets::get_bytes(path).unwrap_or_default();
        SvgViewer::new((path, bytes))
            .show_loader(false)
            .width(Size::px(self.size))
            .height(Size::px(self.size))
            .color(self.color.unwrap_or_else(theme::fg_primary))
    }
}

// ---------- Button ----------

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub enum ButtonVariant {
    #[default]
    Primary,
    /// The green Play button
    Play,
    Secondary,
    Ghost,
}

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub enum ButtonSize {
    #[default]
    Medium,
    Icon,
}

struct ButtonColors {
    background: Color,
    hover: Color,
    pressed: Color,
    disabled: Color,
    foreground: Color,
    disabled_foreground: Color,
    border: Option<(Color, Color, Color)>,
}

fn variant_colors(variant: ButtonVariant) -> ButtonColors {
    match variant {
        ButtonVariant::Primary => ButtonColors {
            background: theme::brand(),
            hover: theme::brand_hover(),
            pressed: theme::brand_pressed(),
            disabled: theme::brand_disabled(),
            foreground: theme::on_brand(),
            disabled_foreground: theme::on_brand_disabled(),
            border: None,
        },
        ButtonVariant::Play => ButtonColors {
            background: theme::play(),
            hover: theme::play_hover(),
            pressed: theme::play_pressed(),
            disabled: theme::play_track(),
            foreground: theme::on_play(),
            disabled_foreground: theme::on_play(),
            border: None,
        },
        ButtonVariant::Secondary => ButtonColors {
            background: theme::component_bg(),
            hover: theme::component_bg_hover(),
            pressed: theme::component_bg_pressed(),
            disabled: theme::component_bg_disabled(),
            foreground: theme::fg_primary(),
            disabled_foreground: theme::fg_primary_disabled(),
            border: Some((theme::component_border(), theme::component_border_hover(), theme::component_border_pressed())),
        },
        ButtonVariant::Ghost => ButtonColors {
            background: Color::TRANSPARENT,
            hover: theme::ghost_overlay_hover(),
            pressed: theme::ghost_overlay_pressed(),
            disabled: Color::TRANSPARENT,
            foreground: theme::fg_primary(),
            disabled_foreground: theme::fg_primary_disabled(),
            border: None,
        },
    }
}

/// (padding, corner radius, font size, width, height)
fn size_layout(size: ButtonSize) -> (Gaps, f32, f32, Size, Size) {
    match size {
        ButtonSize::Medium => (Gaps::new_symmetric(6., 12.), 8., 14., Size::auto(), Size::auto()),
        ButtonSize::Icon => (Gaps::new_all(6.), 8., 14., Size::px(32.), Size::px(32.)),
    }
}

#[derive(Clone, PartialEq)]
pub struct Button {
    variant: ButtonVariant,
    padding: Gaps,
    radius: f32,
    font_size: f32,
    font_weight: Option<FontWeight>,
    width: Size,
    height: Size,
    enabled: bool,
    /// How far the button rises on hover, px
    lift: f32,
    /// Progress shown as the button filling left to right (0..1); the button is busy meanwhile
    progress: Option<f32>,
    foreground: Option<Color>,
    alt: Option<Cow<'static, str>>,
    on_press: Option<EventHandler<Event<PressEventData>>>,
    on_hover: Option<EventHandler<bool>>,
    elements: Vec<Element>,
    key: DiffKey,
}

impl Default for Button {
    fn default() -> Self {
        Self::new()
    }
}

impl Button {
    pub fn new() -> Self {
        let (padding, radius, font_size, width, height) = size_layout(ButtonSize::Medium);
        Self {
            variant: ButtonVariant::Primary,
            padding,
            radius,
            font_size,
            font_weight: None,
            width,
            height,
            enabled: true,
            lift: 1.,
            progress: None,
            foreground: None,
            alt: None,
            on_press: None,
            on_hover: None,
            elements: Vec::new(),
            key: DiffKey::None,
        }
    }
    pub fn play(mut self) -> Self {
        self.variant = ButtonVariant::Play;
        self
    }
    pub fn secondary(mut self) -> Self {
        self.variant = ButtonVariant::Secondary;
        self
    }
    pub fn ghost(mut self) -> Self {
        self.variant = ButtonVariant::Ghost;
        self
    }
    pub fn size(mut self, size: ButtonSize) -> Self {
        let (padding, radius, font_size, width, height) = size_layout(size);
        self.padding = padding;
        self.radius = radius;
        self.font_size = font_size;
        self.width = width;
        self.height = height;
        self
    }
    pub fn icon(self) -> Self {
        self.size(ButtonSize::Icon)
    }
    pub fn padding(mut self, padding: Gaps) -> Self {
        self.padding = padding;
        self
    }
    pub fn width(mut self, width: Size) -> Self {
        self.width = width;
        self
    }
    pub fn height(mut self, height: Size) -> Self {
        self.height = height;
        self
    }
    pub fn font_size(mut self, size: f32) -> Self {
        self.font_size = size;
        self
    }
    pub fn radius(mut self, radius: f32) -> Self {
        self.radius = radius;
        self
    }
    pub fn lift(mut self, lift: f32) -> Self {
        self.lift = lift;
        self
    }
    pub fn progress(mut self, progress: Option<f32>) -> Self {
        self.progress = progress;
        self
    }
    pub fn font_weight(mut self, weight: FontWeight) -> Self {
        self.font_weight = Some(weight);
        self
    }
    pub fn enabled(mut self, enabled: bool) -> Self {
        self.enabled = enabled;
        self
    }
    pub fn foreground(mut self, color: Color) -> Self {
        self.foreground = Some(color);
        self
    }
    /// Accessible name for icon-only buttons
    pub fn alt(mut self, alt: impl Into<Cow<'static, str>>) -> Self {
        self.alt = Some(alt.into());
        self
    }
    pub fn on_press(mut self, handler: impl Into<EventHandler<Event<PressEventData>>>) -> Self {
        self.on_press = Some(handler.into());
        self
    }
    pub fn on_hover(mut self, handler: impl Into<EventHandler<bool>>) -> Self {
        self.on_hover = Some(handler.into());
        self
    }
    pub fn text(self, text: impl Into<Cow<'static, str>>) -> Self {
        self.child(label().text(text))
    }
}

impl ChildrenExt for Button {
    fn children_mut(&mut self) -> &mut Vec<Element> {
        &mut self.elements
    }
}

impl KeyExt for Button {
    fn key_mut(&mut self) -> &mut DiffKey {
        &mut self.key
    }
}

impl Component for Button {
    fn render(&self) -> impl IntoElement {
        let mut hovering = use_state(|| false);
        let mut pressing = use_state(|| false);
        let a11y_id = use_a11y();
        let focus = use_focus(a11y_id);

        let colors = variant_colors(self.variant);
        let progress = self.progress.map(|p| p.clamp(0., 1.));
        let enabled = self.enabled && progress.is_none();
        let background: Fill = if let Some(p) = progress {
            // Progress: the button fills left to right, a hard edge at `p` (a lighter tint over
            // the disabled one)
            let edge = p * 100.;
            let (fill, track) = if self.variant == ButtonVariant::Play {
                (theme::play_fill(), theme::play_track())
            } else {
                (theme::brand_pressed(), theme::brand_disabled())
            };
            LinearGradient::new()
                .angle(-90.)
                .stop((fill, 0.))
                .stop((fill, edge))
                .stop((track, edge))
                .stop((track, 100.))
                .into()
        } else if !enabled {
            colors.disabled.into()
        } else if pressing() {
            colors.pressed.into()
        } else if hovering() {
            colors.hover.into()
        } else {
            colors.background.into()
        };
        let foreground = if enabled || progress.is_some() {
            self.foreground.unwrap_or(colors.foreground)
        } else {
            colors.disabled_foreground
        };
        let border = match colors.border {
            Some((normal, hover, pressed)) => Some(if !enabled {
                normal
            } else if pressing() {
                pressed
            } else if hovering() {
                hover
            } else if focus().is_focused() {
                theme::brand()
            } else {
                normal
            }),
            None if enabled && focus().is_focused() => Some(theme::fg_strong()),
            None => None,
        };

        // Hover: up `lift` px, 150 ms in / 120 ms out. Press: down 1 px and .985, 90 ms.
        let hover_motion = use_animation_transition(hovering, |_, on: bool| step(on, if on { 150 } else { 120 }));
        let press_motion = use_animation_transition(pressing, |_, on: bool| step(on, 90));
        let (hover, press) = if enabled {
            (hover_motion.get().value(), press_motion.get().value())
        } else {
            (0., 0.)
        };
        let offset = -self.lift * hover * (1. - press) + press;
        let scale = 1. - 0.015 * press;

        let on_press = self.on_press.clone();
        let on_hover = self.on_hover.clone();
        let on_hover_out = self.on_hover.clone();

        let mut el = rect()
            .a11y_id(a11y_id)
            .a11y_focusable(enabled)
            .a11y_role(AccessibilityRole::Button)
            .map(self.alt.clone(), |el, alt| el.a11y_alt(alt))
            .horizontal()
            .main_align(Alignment::Center)
            .cross_align(Alignment::Center)
            .spacing(6.)
            .padding(self.padding)
            .width(self.width.clone())
            .height(self.height.clone())
            .corner_radius(rounded(self.radius))
            .offset_y(offset)
            .scale(scale)
            .overflow(Overflow::Clip)
            .background(background)
            .color(foreground)
            .font_size(self.font_size)
            .map(self.font_weight, |el, weight| el.font_weight(weight))
            .children(self.elements.clone());

        if let Some(border) = border {
            el = el.border(Border::new().fill(border).width(1.).alignment(BorderAlignment::Inner));
        }

        // Hover is tracked while disabled too, so a button re-enabled under a pointer that
        // already left it does not stay lifted.
        let el = el
            .on_pointer_over(move |_| {
                hovering.set_if_modified(true);
                if let Some(handler) = &on_hover {
                    handler.call(true);
                }
            })
            .on_pointer_out(move |_| {
                hovering.set_if_modified(false);
                pressing.set_if_modified(false);
                if let Some(handler) = &on_hover_out {
                    handler.call(false);
                }
            });

        if enabled {
            el.on_pointer_down(move |_| pressing.set(true))
                .on_global_pointer_up(move |_| pressing.set_if_modified(false))
                .cursor(CursorIcon::Pointer)
                .map(on_press, |el, handler| el.on_press(move |event: Event<PressEventData>| handler.call(event)))
        } else if progress.is_some() {
            el.cursor(CursorIcon::Progress)
        } else {
            el.opacity(theme::disabled_opacity()).cursor(CursorIcon::NotAllowed)
        }
    }

    fn render_key(&self) -> DiffKey {
        self.key.clone().or(self.default_key())
    }
}

/// One motion step: 0 → 1 when `on`, back to 0 when not; ease-out quart (house curve).
pub fn step(on: bool, ms: u64) -> AnimNum {
    let (from, to) = if on { (0., 1.) } else { (1., 0.) };
    AnimNum::new(from, to).time(ms).function(Function::Quart).ease(Ease::Out)
}

// ---------- TextInput ----------

/// OneLauncher's text field style on Freya's `Input`.
pub fn text_input(value: impl Into<WritableSetter<String>>) -> Input {
    Input::new(value)
        .padding(Gaps::new_symmetric(8., 12.))
        .corner_radius(rounded(8.))
        .background(theme::component_bg())
        .focus_background(theme::component_bg_pressed())
        .border_fill(theme::component_border())
        .focus_border_fill(theme::component_border_pressed())
        .color(theme::fg_primary())
        .placeholder_color(theme::fg_secondary())
        .width(Size::fill())
}


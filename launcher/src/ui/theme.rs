//! OneLauncher's look: its built-in "PolyGlass Dark" theme (`assets/themes/dark.json`) and the
//! colour formulas of its `theme::colors` module, cut down to the single theme NewGen uses.
//! Adapted from OneLauncher (GPL-3.0-only), `packages/oneclient_app/src/theme`.

use std::sync::LazyLock;

use freya::prelude::*;
use serde::Deserialize;

use super::assets::Assets;

/// Be Vietnam Pro: geometric like OneLauncher's Poppins, with full Vietnamese (Poppins lacks ơ ư ạ ế …)
pub const DEFAULT_FONT: &str = "Be Vietnam Pro";
pub const MONO_FONT: &str = "JetBrains Mono";
pub const NAVBAR_HEIGHT_PX: f32 = 80.;
pub const HOME_PADDING_PX: f32 = 48.;

type Rgb = (u8, u8, u8);

#[derive(Deserialize)]
struct ThemeFile {
    colors: PaletteFile,
}

#[derive(Deserialize)]
struct PaletteFile {
    page: String,
    page_elevated: String,
    component: String,
    border: String,
    text: String,
    text_secondary: String,
    accent: String,
}

struct Palette {
    page: Color,
    page_elevated: Color,
    component: Color,
    border: Color,
    text: Color,
    text_secondary: Color,
    accent: Color,
}

fn parse(value: &str) -> Color {
    let digits = value.trim().trim_start_matches('#');
    let packed = u32::from_str_radix(digits, 16).unwrap_or(0xFF00FF);
    match digits.len() {
        8 => Color::from_argb(packed as u8, (packed >> 24) as u8, (packed >> 16) as u8, (packed >> 8) as u8),
        _ => Color::from_rgb((packed >> 16) as u8, (packed >> 8) as u8, packed as u8),
    }
}

static PALETTE: LazyLock<Palette> = LazyLock::new(|| {
    let bytes = Assets::get_bytes("themes/dark.json").expect("dark theme is embedded");
    let file: ThemeFile = serde_json::from_slice(&bytes).expect("dark theme parses");
    let c = file.colors;
    Palette {
        page: parse(&c.page),
        page_elevated: parse(&c.page_elevated),
        component: parse(&c.component),
        border: parse(&c.border),
        text: parse(&c.text),
        text_secondary: parse(&c.text_secondary),
        accent: parse(&c.accent),
    }
});

fn rgb(color: Color) -> Rgb {
    (color.r(), color.g(), color.b())
}

fn color((r, g, b): Rgb) -> Color {
    Color::from_rgb(r, g, b)
}

fn mix((ar, ag, ab): Rgb, (br, bg, bb): Rgb, amount: f32) -> Rgb {
    let blend = |from: u8, to: u8| (f32::from(from) + (f32::from(to) - f32::from(from)) * amount).round() as u8;
    (blend(ar, br), blend(ag, bg), blend(ab, bb))
}

const STRONG: Rgb = (255, 255, 255);

pub fn rounded(radius: f32) -> CornerRadius {
    CornerRadius::new_all(radius)
}

pub fn page() -> Color {
    PALETTE.page
}
pub fn page_elevated() -> Color {
    PALETTE.page_elevated
}
pub fn fg_primary() -> Color {
    PALETTE.text
}
pub fn fg_primary_disabled() -> Color {
    color(mix(rgb(PALETTE.text), rgb(PALETTE.page), 0.3))
}
pub fn fg_secondary() -> Color {
    PALETTE.text_secondary
}
pub fn fg_strong() -> Color {
    color(STRONG)
}
/// Light text on art, video and media
pub fn on_media() -> Color {
    PALETTE.text
}
pub fn brand() -> Color {
    PALETTE.accent
}
pub fn brand_hover() -> Color {
    color(mix(rgb(PALETTE.accent), (0, 0, 0), 0.13))
}
pub fn brand_pressed() -> Color {
    color(mix(rgb(PALETTE.accent), (255, 255, 255), 0.06))
}
pub fn brand_disabled() -> Color {
    color(mix(rgb(PALETTE.accent), rgb(PALETTE.page), 0.5))
}
pub fn on_brand() -> Color {
    Color::WHITE
}
pub fn on_brand_disabled() -> Color {
    color(mix(rgb(PALETTE.text), rgb(brand_disabled()), 0.06))
}
/// The Play button's green: the client mod's "Moss" palette, so launcher and game match.
/// Shades are derived like the brand ones; text on it is the dark page colour.
const MOSS: Rgb = (0x9C, 0xCB, 0x8F);
pub fn play() -> Color {
    color(MOSS)
}
pub fn play_hover() -> Color {
    color(mix(MOSS, (0, 0, 0), 0.13))
}
pub fn play_pressed() -> Color {
    color(mix(MOSS, (255, 255, 255), 0.06))
}
/// Disabled, and the not-yet-filled part while updating (stays light enough for dark text)
pub fn play_track() -> Color {
    color(mix(MOSS, rgb(PALETTE.page), 0.25))
}
/// The "Updating NN%" fill: a lighter tint
pub fn play_fill() -> Color {
    color(mix(MOSS, (255, 255, 255), 0.2))
}
pub fn on_play() -> Color {
    PALETTE.page
}
pub fn component_bg() -> Color {
    PALETTE.component
}
pub fn component_bg_hover() -> Color {
    color(mix(rgb(PALETTE.component), STRONG, 0.04))
}
pub fn component_bg_pressed() -> Color {
    color(mix(rgb(PALETTE.component), STRONG, 0.09))
}
pub fn component_bg_disabled() -> Color {
    color(mix(rgb(PALETTE.component), rgb(PALETTE.page), 0.5))
}
fn border_scaled(scale: f32) -> Color {
    let border = PALETTE.border;
    with_alpha(border, (f32::from(border.a()) * scale).min(255.) as u8)
}
pub fn component_border() -> Color {
    border_scaled(1.)
}
pub fn component_border_hover() -> Color {
    border_scaled(1.6)
}
pub fn component_border_pressed() -> Color {
    border_scaled(2.2)
}
pub fn ghost_overlay_hover() -> Color {
    Color::from_argb(26, 255, 255, 255)
}
pub fn ghost_overlay_pressed() -> Color {
    Color::from_argb(38, 255, 255, 255)
}
pub fn danger() -> Color {
    Color::from_rgb(255, 68, 68)
}
pub fn success() -> Color {
    Color::from_rgb(35, 154, 96)
}
pub fn disabled_opacity() -> f32 {
    0.5
}
pub fn with_alpha(color: Color, alpha: u8) -> Color {
    Color::from_argb(alpha, color.r(), color.g(), color.b())
}

/// The fonts the look needs, for `LaunchConfig::with_font`.
pub fn load_fonts() -> Vec<(&'static str, bytes::Bytes)> {
    const FONTS: [(&str, &str); 5] = [
        (DEFAULT_FONT, "fonts/BeVietnamPro/BeVietnamPro-Bold.ttf"),
        (DEFAULT_FONT, "fonts/BeVietnamPro/BeVietnamPro-SemiBold.ttf"),
        (DEFAULT_FONT, "fonts/BeVietnamPro/BeVietnamPro-Medium.ttf"),
        (DEFAULT_FONT, "fonts/BeVietnamPro/BeVietnamPro-Regular.ttf"),
        (MONO_FONT, "fonts/JetBrainsMono/JetBrainsMonoVariable.ttf"),
    ];
    FONTS
        .iter()
        .filter_map(|&(family, file)| Some((family, Assets::get_bytes(file)?)))
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::i18n::{Lang, S};

    /// Every character the launcher writes, in both languages, plus the bundled website text.
    fn launcher_chars() -> std::collections::BTreeSet<char> {
        let mut text = String::new();
        for &s in S::ALL {
            text.push_str(Lang::En.text(s));
            text.push_str(Lang::Vi.text(s));
        }
        for lang in [Lang::En, Lang::Vi] {
            text.push_str(crate::config::config().tagline.text(lang));
        }
        let sections = Assets::get_bytes("newgen/site/sections.json").expect("sections are embedded");
        text.push_str(std::str::from_utf8(&sections).expect("utf-8"));
        let changelog = Assets::get_bytes("newgen/whats-new.json").expect("changelog is embedded");
        text.push_str(std::str::from_utf8(&changelog).expect("utf-8"));
        text.chars().filter(|c| !c.is_control() && *c != ' ').collect()
    }

    /// WCAG contrast ratio of two opaque colours
    fn contrast(a: Color, b: Color) -> f32 {
        let lum = |c: Color| {
            let lin = |v: u8| {
                let v = f32::from(v) / 255.;
                if v <= 0.04045 { v / 12.92 } else { ((v + 0.055) / 1.055).powf(2.4) }
            };
            0.2126 * lin(c.r()) + 0.7152 * lin(c.g()) + 0.0722 * lin(c.b())
        };
        let (x, y) = (lum(a), lum(b));
        (x.max(y) + 0.05) / (x.min(y) + 0.05)
    }

    #[test]
    fn play_label_is_readable_on_every_play_colour() {
        for bg in [play(), play_hover(), play_pressed(), play_track(), play_fill()] {
            assert!(contrast(on_play(), bg) >= 4.5, "{bg:?}: {:.1}:1", contrast(on_play(), bg));
        }
    }

    #[test]
    fn shipped_fonts_cover_every_launcher_character() {
        let chars = launcher_chars();
        let fonts = skia_safe::FontMgr::new();
        for (family, bytes) in load_fonts() {
            let face = fonts.new_from_data(skia_safe::Data::new_copy(&bytes), None).expect("font loads");
            let missing: String = chars.iter().filter(|&&c| face.unichar_to_glyph(c as i32) == 0).collect();
            assert!(missing.is_empty(), "{family} ({}) has no glyph for: {missing}", face.family_name());
        }
    }
}

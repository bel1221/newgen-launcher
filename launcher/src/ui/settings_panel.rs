//! Settings panel (docs/ui-spec.md "Settings panel"): RAM, keep the launcher open, language,
//! and the game and log folders. Styled like the game log panel; one of the two is open at a time.

use freya::prelude::*;

use super::app::{Shared, lang_toggle, text, use_lang, use_lang_state};
use super::components::{Button, Icon, IconType};
use super::home::{side_panel, toggle_settings};
use super::theme;
use crate::config::{config, memory_mb};
use crate::i18n::S;
use crate::{paths, settings};

#[derive(PartialEq)]
pub struct SettingsPanel {
    pub shared: Shared,
}

fn open_folder(path: &std::path::Path) {
    let _ = std::fs::create_dir_all(path);
    if let Err(err) = open::that(path) {
        tracing::warn!("could not open {}: {err}", path.display());
    }
}

/// A row: `title` (with an optional line under it) on the left, `control` on the right.
fn row(title: &'static str, hint: Option<&'static str>, control: impl IntoElement) -> Rect {
    rect()
        .horizontal()
        .width(Size::fill())
        .content(Content::Flex)
        .cross_align(Alignment::Center)
        .spacing(16.)
        .child(
            rect()
                .width(Size::flex(1.))
                .spacing(2.)
                .child(text(title, 14., FontWeight::MEDIUM, theme::fg_primary()))
                .maybe_child(hint.map(|hint| text(hint, 13., FontWeight::NORMAL, theme::fg_secondary()))),
        )
        .child(control)
}

impl Component for SettingsPanel {
    fn render(&self) -> impl IntoElement {
        let s = self.shared;
        let lang = use_lang();
        let (min_gb, max_gb) = settings::MEMORY_GB;
        let mut memory = use_state(|| settings::memory_gb(memory_mb(&config().memory.max).unwrap_or(4096)));
        let mut keep_open = use_state(settings::keep_open);

        let gb = memory();
        let span = f64::from(max_gb - min_gb);
        let slider = Slider::new(move |percent: f64| {
            let value = (f64::from(min_gb) + percent / 100. * span).round() as u32;
            if value != *memory.peek() {
                memory.set(value);
                // Saved once the slider rests for 300 ms, not on every step of a drag
                spawn(async move {
                    tokio::time::sleep(std::time::Duration::from_millis(300)).await;
                    if *memory.peek() == value {
                        settings::save_memory_gb(value);
                    }
                });
            }
        })
        .value(f64::from(gb - min_gb) / span * 100.)
        .step(100. / span)
        .size(Size::fill())
        .background(theme::component_bg_pressed())
        .thumb_background(theme::brand())
        .thumb_inner_background(theme::on_brand())
        .border_fill(theme::brand());

        let switch = Switch::new(WritableSetter::new(keep_open, move |on: bool| {
            settings::save_keep_open(on);
            keep_open.set(on);
        }))
        .toggled_background(theme::brand())
        .toggled_thumb_background(theme::on_brand())
        .background(theme::component_bg_pressed())
        .thumb_background(theme::fg_secondary());

        let folder_button = |name: &'static str, icon: IconType, path: std::path::PathBuf| {
            Button::new()
                .secondary()
                .padding(Gaps::new_symmetric(0., 16.))
                .height(Size::px(40.))
                .radius(8.)
                .font_size(14.)
                .font_weight(FontWeight::MEDIUM)
                .on_press(move |_| open_folder(&path))
                .child(
                    rect()
                        .horizontal()
                        .cross_align(Alignment::Center)
                        .spacing(8.)
                        .child(Icon::new(icon).size(16.))
                        .child(label().text(name)),
                )
        };

        let body = rect()
            .width(Size::fill())
            .padding(Gaps::new_symmetric(20., 20.))
            .spacing(24.)
            .child(
                rect()
                    .width(Size::fill())
                    .spacing(12.)
                    .child(row(lang.text(S::Memory), None, text(format!("{gb} GB"), 14., FontWeight::SEMI_BOLD, theme::fg_primary())))
                    .child(slider),
            )
            .child(row(lang.text(S::KeepOpen), Some(lang.text(S::KeepOpenHint)), switch))
            .child(row(lang.text(S::Language), None, lang_toggle(use_lang_state())))
            .child(
                rect()
                    .horizontal()
                    .spacing(12.)
                    .child(folder_button(lang.text(S::OpenGameFolder), IconType::Folder, paths::game_dir().to_path_buf()))
                    .child(folder_button(lang.text(S::OpenLogs), IconType::Terminal, paths::game_dir().join("logs"))),
            );

        side_panel(480., lang.text(S::Settings), lang.text(S::CloseSettings), move || toggle_settings(s), body)
    }
}

//! "What's new" card on Home (docs/ui-spec.md): the bundled changelog for this version, shown on
//! the first start after an update, three lines then "See all", dismissable. It sits top right,
//! where the side panels open, and steps aside while one is open; it never covers Play.

use freya::prelude::*;

use super::app::{EDGE, border, text, use_lang};
use super::components::{Button, Icon, IconType};
use super::theme::{self, with_alpha as a};
use crate::config::VERSION;
use crate::i18n::{S, fill};
use crate::{settings, whats_new};

/// Lines shown before "See all"
const FIRST_LINES: usize = 3;

#[derive(PartialEq)]
pub struct WhatsNewCard {
    /// A side panel is open in its place
    pub hidden: bool,
}

impl Component for WhatsNewCard {
    fn render(&self) -> impl IntoElement {
        let lang = use_lang();
        // Decided once per launch; marked as seen as soon as it shows, so it shows once
        let entry = use_hook(|| {
            let entry = whats_new::to_show(&whats_new::bundled(), VERSION, settings::last_seen_version().as_deref()).cloned();
            if entry.is_some() {
                settings::save_last_seen_version(VERSION);
            }
            entry
        });
        let mut open = use_state(|| true);
        let mut expanded = use_state(|| false);

        let Some(entry) = entry.filter(|_| open() && !self.hidden) else { return rect() };
        let lines = entry.lines(lang);
        let shown = if expanded() { lines.len() } else { lines.len().min(FIRST_LINES) };
        let more = lines.len() > FIRST_LINES;

        rect()
            .position(Position::new_absolute().top(8.).right(EDGE))
            .width(Size::px(380.))
            .layer(Layer::Relative(5))
            .padding(Gaps::new(12., 10., 16., 18.))
            .spacing(8.)
            .corner_radius(theme::rounded(12.))
            .background(a(theme::page_elevated(), 215))
            .border(border())
            .child(
                rect()
                    .horizontal()
                    .width(Size::fill())
                    .content(Content::Flex)
                    .cross_align(Alignment::Center)
                    .child(text(fill(lang.text(S::WhatsNew), &[&entry.version]), 15., FontWeight::SEMI_BOLD, theme::fg_primary()).width(Size::flex(1.)))
                    .child(
                        Button::new()
                            .ghost()
                            .icon()
                            .alt(lang.text(S::Close))
                            .on_press(move |_| open.set(false))
                            .child(Icon::new(IconType::X).size(16.)),
                    ),
            )
            .children(lines[..shown].iter().map(|line| {
                rect()
                    .horizontal()
                    .width(Size::fill())
                    .content(Content::Flex)
                    .spacing(8.)
                    .child(rect().margin(Gaps::new(7., 0., 0., 0.)).width(Size::px(5.)).height(Size::px(5.)).corner_radius(theme::rounded(3.)).background(theme::play()))
                    .child(text(line.clone(), 13., FontWeight::NORMAL, theme::fg_secondary()).width(Size::flex(1.)))
                    .into_element()
            }))
            .maybe_child(more.then(|| {
                rect()
                    .cursor(CursorIcon::Pointer)
                    .a11y_role(AccessibilityRole::Button)
                    .on_press(move |_| expanded.toggle())
                    .child(text(lang.text(if expanded() { S::ShowLess } else { S::SeeAll }), 13., FontWeight::MEDIUM, theme::play()))
            }))
    }
}

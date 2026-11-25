//! Home / Play screen (docs/ui-spec.md "Home / Play screen") and the Play button's work
//! (the Electron main.js `play` handler).

use std::rc::Rc;
use std::time::Duration;

use freya::prelude::*;

use super::app::{self, EDGE, LOG_LINE, PlayState, Shared, border, enter, lockup, push_log, text, use_lang};
use super::cards::SiteCards;
use super::components::{Button, Icon, IconType};
use super::settings_panel::SettingsPanel;
use super::theme::{self, with_alpha as a};
use crate::auth::{self, Session};
use crate::burst::Burst;
use crate::config::config;
use crate::game::{self, EventSender, LauncherEvent};
use crate::i18n::{S, Text, fill, trf};
use crate::mods::{self, ModsError};
use crate::{paths, settings, status, sync};

/// Window y where the site cards start; panels on the right end 16 px above it.
const CARDS_TOP: f32 = app::HEIGHT - EDGE - 148.;
/// Height of the right-side panels (game log, settings): content y 8 → window y CARDS_TOP − 16
pub const PANEL_HEIGHT: f32 = CARDS_TOP - 16. - theme::NAVBAR_HEIGHT_PX - 8.;
/// Play's width, for every label in both languages
const PLAY_WIDTH: f32 = 300.;

#[derive(PartialEq)]
pub struct Home {
    pub shared: Shared,
    /// Entrance clock, ms
    pub t: f32,
}

impl Component for Home {
    fn render(&self) -> impl IntoElement {
        let s = self.shared;
        let t = self.t;
        let lang = use_lang();
        let config = config();
        let play = *s.play.read();
        let log_open = *s.log_open.read();
        let settings_open = *s.settings_open.read();
        let has_log = !s.logs.read().is_empty();

        // Players online from the website, every 30 s; None = unknown (the line shows without it)
        let mut online = use_state(|| None::<u64>);
        use_hook(move || {
            spawn(async move {
                loop {
                    let url = config.status_url.clone();
                    let count = tokio::spawn(async move { status::players_online(&url).await }).await.ok().flatten();
                    online.set_if_modified(count);
                    tokio::time::sleep(Duration::from_secs(30)).await;
                }
            });
        });

        // Blocks after the lockup enter at 350, 410, 470 ms, in order
        let mut starts = [350., 410., 470.].into_iter();
        let mut next = move || starts.next().unwrap_or(470.);

        let tagline = config.tagline.text(lang);
        let launch_line = (!tagline.is_empty()).then(|| {
            enter(
                rect().margin(Gaps::new(14., 0., 0., 0.)).child(
                    text(tagline.to_string(), 20., FontWeight::SEMI_BOLD, theme::on_media())
                        .max_lines(1)
                        .text_overflow(TextOverflow::Ellipsis),
                ),
                t,
                next(),
                520.,
                12.,
            )
        });

        // Status line: dot, "N players online", "· Minecraft 1.21.11 · newgenmc.net"
        let count = *online.read();
        let facts = format!("Minecraft {} · {}", config.minecraft_version, config.server_host);
        let status_line = enter(
            rect()
                .horizontal()
                .cross_align(Alignment::Center)
                .spacing(8.)
                .margin(Gaps::new(if launch_line.is_some() { 8. } else { 16. }, 0., 0., 0.))
                .child(
                    rect()
                        .width(Size::px(8.))
                        .height(Size::px(8.))
                        .corner_radius(theme::rounded(4.))
                        .background(if count.is_some() { theme::success() } else { theme::fg_secondary() }),
                )
                .maybe_child(count.map(|n| text(fill(lang.text(S::PlayersOnline), &[&lang.number(n)]), 15., FontWeight::SEMI_BOLD, theme::on_media())))
                .child(text(
                    if count.is_some() { format!("· {facts}") } else { facts },
                    15.,
                    FontWeight::MEDIUM,
                    a(theme::on_media(), 190),
                )),
            t,
            next(),
            520.,
            12.,
        );

        // Play: while a step reports progress the button itself fills, "Updating NN%"
        let filling = (play == PlayState::Launching).then(|| *s.progress.read()).flatten();
        let play_label = match (play, filling) {
            (_, Some(p)) => fill(lang.text(S::Updating), &[&((p.clamp(0., 1.) * 100.).round() as u32)]),
            (PlayState::Idle, _) => lang.text(S::Play).to_string(),
            (PlayState::Launching, _) => lang.text(S::Launching).to_string(),
            (PlayState::Playing, _) => lang.text(S::Playing).to_string(),
        };
        let mut centre = s.play_centre;
        let play_button = rect()
            .on_sized(move |e: Event<SizedEventData>| {
                let area = e.area;
                centre.set_if_modified((area.min_x() + area.width() / 2., area.min_y() + area.height() / 2.));
            })
            .child(
                Button::new()
                    .play()
                    .lift(2.)
                    .padding(Gaps::new_all(0.))
                    // Fixed: the longest label ("Đang cập nhật 100%") fits, so nothing beside it moves
                    .width(Size::px(PLAY_WIDTH))
                    .height(Size::px(52.))
                    .radius(12.)
                    .font_size(18.)
                    .font_weight(FontWeight::SEMI_BOLD)
                    .enabled(play == PlayState::Idle)
                    .progress(filling)
                    .on_press(move |_| start_play(s))
                    .child(
                        rect()
                            .horizontal()
                            .height(Size::fill())
                            .padding(Gaps::new_symmetric(0., 36.))
                            .main_align(Alignment::Center)
                            .cross_align(Alignment::Center)
                            .spacing(10.)
                            .child(Icon::new(IconType::Play).size(18.).color(theme::on_play()))
                            .child(label().text(play_label)),
                    ),
            );
        let settings_color = if settings_open { theme::brand() } else { theme::fg_primary() };
        let settings_button = Button::new()
            .secondary()
            .padding(Gaps::new_all(0.))
            .width(Size::px(52.))
            .height(Size::px(52.))
            .radius(12.)
            .alt(lang.text(S::Settings))
            .on_press(move |_| toggle_settings(s))
            .child(Icon::new(IconType::Settings).size(20.).color(settings_color));
        let log_color = if log_open { theme::brand() } else { theme::fg_primary() };
        let log_button = has_log.then(|| {
            Button::new()
                .secondary()
                .padding(Gaps::new_symmetric(0., 20.))
                .height(Size::px(52.))
                .radius(12.)
                .font_size(15.)
                .font_weight(FontWeight::MEDIUM)
                .foreground(log_color)
                .on_press(move |_| toggle_log(s))
                .child(
                    rect()
                        .horizontal()
                        .cross_align(Alignment::Center)
                        .spacing(8.)
                        .child(Icon::new(IconType::Terminal).size(16.).color(log_color))
                        .child(label().text(lang.text(S::GameLog))),
                )
        });
        let actions = enter(
            rect()
                .horizontal()
                .spacing(12.)
                .cross_align(Alignment::Center)
                .margin(Gaps::new(28., 0., 0., 0.))
                .child(play_button)
                .child(settings_button)
                .maybe_child(log_button),
            t,
            next(),
            520.,
            12.,
        );

        // Step text; while the step reports progress, the Play button says it instead
        let status = s.status.read().clone().filter(|_| filling.is_none()).map(|line| {
            let line = line.get(lang).to_string();
            rect()
                .margin(Gaps::new(20., 0., 0., 0.))
                .width(Size::px(420.))
                .child(text(line, 13., FontWeight::MEDIUM, theme::on_media()).max_lines(1).text_overflow(TextOverflow::Ellipsis))
        });
        let error = s.error.read().clone().map(|message| {
            let message = message.get(lang).to_string();
            rect()
                .margin(Gaps::new(16., 0., 0., 0.))
                .width(Size::px(560.))
                .horizontal()
                .content(Content::Flex)
                .spacing(10.)
                .padding(Gaps::new_symmetric(12., 14.))
                .corner_radius(theme::rounded(12.))
                .background(a(theme::danger(), 36))
                .border(Border::new().fill(a(theme::danger(), 110)).width(1.).alignment(BorderAlignment::Inner))
                .child(Icon::new(IconType::AlertCircle).size(16.).color(theme::danger()))
                .child(
                    text(message, 13., FontWeight::NORMAL, theme::fg_primary())
                        .width(Size::flex(1.))
                        .max_lines(4)
                        .text_overflow(TextOverflow::Ellipsis),
                )
        });

        let hero = rect()
            .position(Position::new_absolute().left(EDGE).top(56.))
            .width(Size::px(620.))
            .cross_align(Alignment::Start)
            .child(enter(rect().child(lockup(88.)), t, 0., 700., 16.))
            .maybe_child(launch_line)
            .child(status_line)
            .child(actions)
            .maybe_child(status)
            .maybe_child(error);

        rect()
            .position(Position::new_absolute().top(theme::NAVBAR_HEIGHT_PX).left(0.))
            .width(Size::px(app::WIDTH))
            .height(Size::px(app::HEIGHT - theme::NAVBAR_HEIGHT_PX))
            .child(hero)
            .child(SiteCards {})
            .maybe_child(log_open.then(|| log_panel(s, lang.text(S::GameLog), lang.text(S::CloseGameLog))))
            .maybe_child(settings_open.then(|| SettingsPanel { shared: s }))
            .maybe_child(s.crash.read().clone().map(|lines| crash_panel(s, lines, lang)))
    }
}

fn toggle_log(s: Shared) {
    let (mut open, mut scroll, mut other) = (s.log_open, s.log_scroll, s.settings_open);
    let opening = !open();
    if opening {
        // Opens on the newest lines; the view measures itself again on mount
        scroll.bounds.set(None);
        scroll.scroll_to(ScrollPosition::End, Direction::Vertical);
        other.set_if_modified(false);
    }
    open.set(opening);
}

pub fn toggle_settings(s: Shared) {
    let (mut open, mut other) = (s.settings_open, s.log_open);
    let opening = !open();
    if opening {
        other.set_if_modified(false);
    }
    open.set(opening);
}

/// The frame both right-side panels share (spec "Game log panel"): toolbar with a title and a
/// close button, a divider, then `body`.
pub fn side_panel(width: f32, title: &'static str, close_alt: &'static str, on_close: impl Fn() + 'static, body: impl IntoElement) -> Rect {
    rect()
        .position(Position::new_absolute().top(8.).right(EDGE))
        .width(Size::px(width))
        .height(Size::px(PANEL_HEIGHT))
        .layer(Layer::Relative(10))
        .background(a(theme::page_elevated(), 240))
        .corner_radius(theme::rounded(12.))
        .border(border())
        .overflow(Overflow::Clip)
        .child(
            rect()
                .horizontal()
                .width(Size::fill())
                .content(Content::Flex)
                .cross_align(Alignment::Center)
                .padding(Gaps::new(10., 14., 10., 20.))
                .child(text(title, 16., FontWeight::SEMI_BOLD, theme::fg_primary()).width(Size::flex(1.)))
                .child(
                    Button::new()
                        .ghost()
                        .icon()
                        .alt(close_alt)
                        .on_press(move |_| on_close())
                        .child(Icon::new(IconType::X).size(16.)),
                ),
        )
        .child(rect().width(Size::fill()).height(Size::px(1.)).background(theme::component_border()))
        .child(body)
}

/// After a crash: what happened, the last log lines, Copy log / Open crash reports folder / Close.
/// Centred over the screen on a dim backdrop that takes the clicks.
fn crash_panel(s: Shared, lines: Vec<String>, lang: crate::i18n::Lang) -> Rect {
    let mut crash = s.crash;
    let log_text = lines.join("\n");
    let reports = paths::game_dir().join("crash-reports");
    let button = |name: &'static str| {
        Button::new().secondary().padding(Gaps::new_symmetric(0., 16.)).height(Size::px(40.)).radius(8.).font_size(14.).font_weight(FontWeight::MEDIUM).text(name)
    };
    rect()
        .position(Position::new_absolute().top(-theme::NAVBAR_HEIGHT_PX).left(0.))
        .width(Size::px(app::WIDTH))
        .height(Size::px(app::HEIGHT))
        .layer(Layer::Relative(20))
        .background(a(theme::page(), 170))
        .main_align(Alignment::Center)
        .cross_align(Alignment::Center)
        .child(
            rect()
                .width(Size::px(680.))
                .padding(20.)
                .spacing(14.)
                .corner_radius(theme::rounded(12.))
                .background(theme::page_elevated())
                .border(border())
                .child(
                    rect()
                        .horizontal()
                        .cross_align(Alignment::Center)
                        .spacing(10.)
                        .child(Icon::new(IconType::AlertCircle).size(18.).color(theme::danger()))
                        .child(text(lang.text(S::CrashTitle), 16., FontWeight::SEMI_BOLD, theme::fg_primary())),
                )
                .child(text(lang.text(S::CrashHint), 13., FontWeight::NORMAL, theme::fg_secondary()))
                .child(
                    rect()
                        .width(Size::fill())
                        .height(Size::px(LOG_LINE * 20. + 16.))
                        .padding(Gaps::new_symmetric(8., 10.))
                        .corner_radius(theme::rounded(8.))
                        .background(theme::page())
                        .border(border())
                        .overflow(Overflow::Clip)
                        .children(lines.into_iter().map(|line| {
                            label().text(line).font_family(theme::MONO_FONT).font_size(12.).max_lines(1).height(Size::px(LOG_LINE)).color(theme::fg_secondary()).into_element()
                        })),
                )
                .child(
                    rect()
                        .horizontal()
                        .spacing(10.)
                        .child(button(lang.text(S::CopyLog)).on_press(move |_| {
                            if let Err(err) = freya::clipboard::Clipboard::set(log_text.clone()) {
                                tracing::warn!("could not copy the log: {err:?}");
                            }
                        }))
                        .child(button(lang.text(S::OpenCrashReports)).on_press(move |_| {
                            let _ = std::fs::create_dir_all(&reports);
                            if let Err(err) = open::that(&reports) {
                                tracing::warn!("could not open {}: {err}", reports.display());
                            }
                        }))
                        .child(button(lang.text(S::Close)).on_press(move |_| crash.set(None))),
                ),
        )
}

fn log_panel(s: Shared, title: &'static str, close_alt: &'static str) -> Rect {
    let logs = s.logs;
    let count = s.logs.read().len();
    let rows = VirtualScrollView::new_with_data_controlled(
        *s.log_count.read(),
        move |item: VirtualItem, _: &u64| {
            let (line, error) = logs.read().get(item.index).map(|row| (row.text.clone(), row.error)).unwrap_or_default();
            let color = if error { theme::danger() } else { theme::fg_secondary() };
            rect()
                .key(item.index)
                .width(Size::fill())
                .height(Size::px(item.size))
                .padding(Gaps::new_symmetric(0., 10.))
                .child(
                    label()
                        .text(line)
                        .font_family(theme::MONO_FONT)
                        .font_size(12.)
                        .max_lines(1)
                        .color(color),
                )
                .into()
        },
        s.log_scroll,
    )
    .length(count)
    .item_size(LOG_LINE);

    side_panel(
        520.,
        title,
        close_alt,
        move || toggle_log(s),
        rect().width(Size::fill()).height(Size::fill()).padding(Gaps::new(10., 0., 20., 0.)).child(rows),
    )
}

fn start_play(s: Shared) {
    let Shared { session, mut play, mut error, mut status, mut progress, mut burst, play_centre, .. } = s;
    let Some(user) = session.peek().clone() else { return };
    error.set(None);
    if !game::claim() {
        error.set(Some(Text::key(S::AlreadyRunning, &[])));
        play.set(PlayState::Idle);
        return;
    }
    game::accept_progress(true);
    play.set(PlayState::Launching); // same frame as the burst
    if let Some(set) = app::weapons() {
        burst.set(Some(Rc::new(Burst::new(set, *play_centre.peek()))));
    }
    let ui = app::sender();
    spawn_in_window(async move {
        let result = tokio::spawn(launch(user, ui)).await.unwrap_or_else(|err| Err(err.to_string()));
        match result {
            // The game may already have closed again (GameClosed reset the button)
            Ok(()) if game::is_running() => {
                play.set(PlayState::Playing);
                // "Keep launcher open" off: out of the way while playing
                if !settings::keep_open() {
                    Platform::get().with_window(Platform::window_id(), |window| window.set_minimized(true));
                }
            }
            Ok(()) => {}
            Err(message) => {
                game::accept_progress(false);
                game::release();
                push_log(s, message.clone());
                error.set(Some(Text::raw(message)));
                status.set(None);
                progress.set(None);
                play.set(PlayState::Idle);
            }
        }
    });
}

async fn launch(session: Session, ui: EventSender) -> Result<(), String> {
    let config = config();
    let status = |line: S| drop(ui.send(LauncherEvent::Status(Text::key(line, &[]))));

    status(S::CheckingFiles);
    let progress = ui.clone();
    sync::sync_files(config, paths::game_dir(), move |p| {
        let _ = progress.send(LauncherEvent::Progress { task: p.task, current: p.current, total: p.total });
    })
    .await?;

    // After the sync, so nothing gets past it: install the bundled mods, refuse anything else
    let installed = tokio::task::spawn_blocking(|| {
        mods::install_mods(&paths::bundled_mods_dir(), &paths::game_dir().join("mods"), &paths::installed_mods_record())
    })
    .await
    .map_err(|err| err.to_string())?;
    match installed {
        Ok(()) => {}
        Err(ModsError::Other(message)) => return Err(message),
        Err(ModsError::Added(names)) => {
            let message = trf(S::ModsNotAllowed, &[&names.join(", ")]);
            rfd::AsyncMessageDialog::new()
                .set_level(rfd::MessageLevel::Warning)
                .set_title(&config.server_name)
                .set_description(&message)
                .set_buttons(rfd::MessageButtons::Ok)
                .show()
                .await;
            return Err(message);
        }
    }

    status(S::GettingTicket);
    let join_token = auth::join_token(config, &session).await?;

    status(S::StartingMinecraft);
    game::launch(config, &session, join_token, ui.clone()).await?;
    status(S::GameRunning);
    Ok(())
}

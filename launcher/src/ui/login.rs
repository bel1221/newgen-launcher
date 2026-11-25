//! Login screen (docs/ui-spec.md "Login screen").

use freya::prelude::*;

use super::app::{EDGE, border, enter, lockup, text, use_lang};
use super::components::{Icon, IconType, text_input, Button};
use super::theme::{self, with_alpha as a};
use crate::auth::{self, Session};
use crate::config::config;
use crate::i18n::S;
use crate::settings;

/// Height kept for the error row: three 13 px lines
const ERROR_ROW: f32 = 58.;

#[derive(PartialEq)]
pub struct Login {
    pub session: State<Option<Session>>,
    /// Entrance clock, ms
    pub t: f32,
}

fn field(name: &'static str, input: Input) -> Rect {
    rect()
        .width(Size::fill())
        .spacing(6.)
        .child(text(name, 13., FontWeight::MEDIUM, theme::fg_secondary()))
        // Input has no font size of its own: it inherits this one
        .child(rect().width(Size::fill()).font_size(14.).child(input))
}

impl Component for Login {
    fn render(&self) -> impl IntoElement {
        let account = use_state(settings::saved_username);
        let password = use_state(String::new);
        let pending = use_state(|| false);
        let error = use_state(|| None::<String>);
        let session = self.session;
        let lang = use_lang();
        let t = self.t;

        let submit = move || {
            let (mut pending, mut error, mut password, mut session) = (pending, error, password, session);
            if pending() {
                return;
            }
            pending.set(true);
            error.set(None);
            let (name, secret) = (account.read().clone(), password.read().clone());
            spawn(async move {
                let started = std::time::Instant::now();
                let result = auth::login(config(), &name, &secret).await;
                // A real login shows "Logging in…" for at least 300 ms, so it never just flickers
                // (offline dev mode is instant and skips it)
                if config().auth_enabled() {
                    let min = std::time::Duration::from_millis(300);
                    tokio::time::sleep(min.saturating_sub(started.elapsed())).await;
                }
                pending.set(false);
                match result {
                    Ok(user) => {
                        settings::save_username(&user.username);
                        password.set(String::new());
                        session.set(Some(user)); // last: this screen unmounts
                    }
                    Err(message) => error.set(Some(message)),
                }
            });
        };

        let offline = !config().auth_enabled();
        let account_input = text_input(account)
            .on_validate(|v: InputValidator| v.set_valid(v.text().chars().count() <= 16))
            .on_submit(move |_| submit());
        let password_input = text_input(password)
            .mode(InputMode::new_password())
            .on_submit(move |_| submit());
        let password_input = if offline { password_input.enabled(false).placeholder(lang.text(S::NotNeeded)) } else { password_input };

        let busy = pending();
        let card = rect()
            .width(Size::px(400.))
            .padding(32.)
            .spacing(20.)
            .corner_radius(theme::rounded(16.))
            .background(a(theme::page_elevated(), 215))
            .border(border())
            .child(
                rect()
                    .spacing(8.)
                    .child(enter(rect().child(lockup(56.)), t, 0., 700., 16.))
                    .child(enter(
                        rect().child(text(lang.text(S::LoginSubtitle), 14., FontWeight::NORMAL, theme::fg_secondary())),
                        t,
                        350.,
                        520.,
                        12.,
                    )),
            )
            .child(enter(field(lang.text(S::AccountName), account_input), t, 410., 520., 12.))
            .child(enter(field(lang.text(S::Password), password_input), t, 470., 520., 12.))
            .child(enter(
                rect().width(Size::fill()).child(
                    Button::new()
                        .play()
                        .width(Size::fill())
                        .height(Size::px(46.))
                        .radius(12.)
                        .font_size(16.)
                        .font_weight(FontWeight::SEMI_BOLD)
                        .enabled(!busy)
                        .on_press(move |_| submit())
                        .text(lang.text(if busy { S::LoggingIn } else { S::LogIn })),
                ),
                t,
                530.,
                520.,
                12.,
            ))
            // The error row keeps its space (3 lines) whether or not it shows, so the card never
            // changes height
            .child(rect().width(Size::fill()).height(Size::px(ERROR_ROW)).maybe_child(error.read().clone().map(|message| {
                rect()
                    .horizontal()
                    .content(Content::Flex)
                    .width(Size::fill())
                    .spacing(8.)
                    .cross_align(Alignment::Start)
                    .child(Icon::new(IconType::AlertTriangle).size(14.).color(theme::danger()))
                    .child(
                        text(message, 13., FontWeight::NORMAL, theme::danger())
                            .width(Size::flex(1.))
                            .max_lines(3)
                            .text_overflow(TextOverflow::Ellipsis),
                    )
            })));

        rect()
            .position(Position::new_absolute().top(theme::NAVBAR_HEIGHT_PX).left(0.))
            .width(Size::px(super::app::WIDTH))
            .height(Size::px(super::app::HEIGHT - theme::NAVBAR_HEIGHT_PX))
            .main_align(Alignment::Center)
            .padding(Gaps::new(0., 0., 0., EDGE))
            .child(card)
    }
}

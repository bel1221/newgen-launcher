#![cfg_attr(all(not(debug_assertions), windows), windows_subsystem = "windows")]

mod auth;
mod burst;
mod config;
mod game;
mod i18n;
mod whats_new;
mod mods;
mod paths;
mod settings;
mod status;
mod sync;
mod ui;
mod video;

use freya::prelude::*;

use ui::app::{HEIGHT, Launcher, WIDTH};
use ui::assets::Assets;
use ui::theme;

fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::try_from_default_env().unwrap_or_else(|_| "warn,newgen_launcher=info".into()))
        .init();

    let mut builder = tokio::runtime::Builder::new_multi_thread();
    builder.enable_all();
    // Debug builds of OneLauncher's async code can overflow the default 2 MB stack (as OneLauncher does)
    #[cfg(debug_assertions)]
    builder.thread_stack_size(3 * 1024 * 1024);
    let runtime = builder.build().expect("could not start the async runtime");
    let _runtime = runtime.enter();

    i18n::set_lang(i18n::initial(settings::saved_lang().as_deref()));
    let (sender, events) = tokio::sync::mpsc::unbounded_channel();
    game::init(sender.clone());
    let picture = video::start(video::list_videos(&paths::videos_dir()));

    let zoom = window_zoom();
    ui::app::set_window_zoom(zoom);
    let icon = Assets::get_bytes("newgen/icon.png").expect("icon is embedded");
    let window = WindowConfig::new_app(Launcher::new(events, sender, picture))
        .with_title(config::config().server_name.as_str())
        .with_size(f64::from(WIDTH) * zoom, f64::from(HEIGHT) * zoom)
        // The layout is always 1280×720; a smaller screen gets the same layout zoomed out
        .with_custom_scale_factor(zoom)
        .with_decorations(false)
        .with_resizable(false)
        .with_background(theme::page())
        .with_icon(LaunchConfig::window_icon(&icon));

    let mut launch_config = LaunchConfig::new().with_window(window).with_default_font(theme::DEFAULT_FONT);
    for (family, bytes) in theme::load_fonts() {
        launch_config = launch_config.with_font(family, bytes);
    }
    launch(launch_config);
}

/// 1 when 1280×720 fits the primary screen's work area with a 16 px margin, else the
/// proportional shrink that makes it fit.
fn window_zoom() -> f64 {
    let Some((width, height)) = work_area() else { return 1. };
    let margin = 16.;
    ((width - margin) / f64::from(WIDTH)).min((height - margin) / f64::from(HEIGHT)).clamp(0.5, 1.)
}

/// The primary screen minus the taskbar, in logical px. Called before winit makes the process
/// DPI-aware, so Windows reports it already scaled to 96 DPI, the unit `with_size` takes.
#[cfg(windows)]
fn work_area() -> Option<(f64, f64)> {
    #[repr(C)]
    struct WinRect {
        left: i32,
        top: i32,
        right: i32,
        bottom: i32,
    }
    #[link(name = "user32")]
    unsafe extern "system" {
        fn SystemParametersInfoW(action: u32, param: u32, value: *mut std::ffi::c_void, ini: u32) -> i32;
    }
    const SPI_GETWORKAREA: u32 = 0x0030;
    let mut area = WinRect { left: 0, top: 0, right: 0, bottom: 0 };
    // SAFETY: SPI_GETWORKAREA writes one RECT to the pointer given
    let ok = unsafe { SystemParametersInfoW(SPI_GETWORKAREA, 0, (&raw mut area).cast(), 0) };
    (ok != 0).then(|| (f64::from(area.right - area.left), f64::from(area.bottom - area.top)))
}

#[cfg(not(windows))]
fn work_area() -> Option<(f64, f64)> {
    None
}

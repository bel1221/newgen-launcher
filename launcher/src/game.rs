//! Starting Minecraft, on OneLauncher's launcher core.
//!
//! OneLauncher's crates do the heavy lifting: `oneclient_mc` reads Mojang's and Fabric's metadata,
//! downloads the client, libraries, natives and assets and builds the launch arguments;
//! `oneclient_java` finds a Java that fits the game or downloads one. This file only wires them
//! to NewGen's setup: one fixed version (config.json), an offline account with the same UUID an
//! offline-mode server gives the player, the auto-join arguments and the join token hand-off.

use std::collections::HashMap;
use std::path::Path;
use std::process::Stdio;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, OnceLock};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

use interfrost::api::minecraft::ArgumentType;
use oneclient_common::Resolution;
use oneclient_common::domain::GameLoader;
use oneclient_common::paths as one_paths;
use oneclient_events::{
    Event, EventBus, EventReceiver, GroupedProgressEvent, GroupedProgressSession, ProgressEvent,
    TaskCategory,
};
use oneclient_java::{HostTarget, JavaService, MemoryJavaStore};
use oneclient_mc::{McCtx, MetadataStore, NativesMode};
use oneclient_net::{NetConfig, RequestClient};
use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::sync::{Mutex, mpsc};

use crate::auth::Session;
use crate::config::{Config, memory_mb};
use crate::paths;

/// What the launcher tells the window while it works (the Electron version's IPC events).
#[derive(Debug, Clone)]
pub enum LauncherEvent {
    /// A plain status line; hides the progress bar.
    Status(crate::i18n::Text),
    Progress { task: String, current: u64, total: u64 },
    Log(String),
    /// Exit code, and whether it looks like a crash (see `is_crash`)
    GameClosed(Option<i32>, bool),
}

pub type EventSender = mpsc::UnboundedSender<LauncherEvent>;

struct Core {
    ctx: McCtx,
    events: EventBus,
    metadata: Mutex<MetadataStore>,
    java: JavaService,
    running: AtomicBool,
}

static CORE: OnceLock<Arc<Core>> = OnceLock::new();

/// Sets up OneLauncher's core. Must run on the tokio runtime, before anything else uses
/// OneLauncher's paths.
pub fn init(ui: EventSender) {
    one_paths::set_launcher_dir(paths::launcher_dir().to_path_buf());
    let net = RequestClient::new(NetConfig::default()).expect("could not create the HTTP client");
    let (events, receiver) = EventBus::channel();
    let core = Core {
        ctx: McCtx::new(net.clone(), events.clone()),
        java: JavaService::new(MemoryJavaStore::new(), net, events.clone()),
        events,
        metadata: Mutex::new(MetadataStore::new()),
        running: AtomicBool::new(false),
    };
    let _ = CORE.set(Arc::new(core));
    tokio::spawn(pump_progress(receiver, ui));
}

fn core() -> &'static Arc<Core> {
    CORE.get().expect("game::init was not called")
}

pub fn is_running() -> bool {
    CORE.get().is_some_and(|core| core.running.load(Ordering::SeqCst))
}

/// Marks the game as starting; false if it already is.
pub fn claim() -> bool {
    !core().running.swap(true, Ordering::SeqCst)
}

pub fn release() {
    core().running.store(false, Ordering::SeqCst);
}

/// Whether the window still shows Progress events. Closed once downloads are over or the launch
/// failed, so a late event (they come from another task) cannot bring back a finished bar.
static ACCEPT_PROGRESS: AtomicBool = AtomicBool::new(false);

pub fn accept_progress(on: bool) {
    ACCEPT_PROGRESS.store(on, Ordering::SeqCst);
}

pub fn accepting_progress() -> bool {
    ACCEPT_PROGRESS.load(Ordering::SeqCst)
}

/// One line of game output: invalid UTF-8 (Java writes the console code page) is replaced,
/// the line ending dropped.
fn decode_line(bytes: &[u8]) -> String {
    let line = bytes.strip_suffix(b"\n").unwrap_or(bytes);
    let line = line.strip_suffix(b"\r").unwrap_or(line);
    String::from_utf8_lossy(line).into_owned()
}

/// OneLauncher reports downloads as grouped progress (one session, many files in categories).
/// The window shows one line and one bar, so the files are summed per category.
async fn pump_progress(mut receiver: EventReceiver, ui: EventSender) {
    #[derive(Default)]
    struct Category {
        expected: u64,
        children: HashMap<uuid::Uuid, (u64, u64)>,
    }
    let mut categories: HashMap<TaskCategory, Category> = HashMap::new();
    let mut child_category: HashMap<uuid::Uuid, TaskCategory> = HashMap::new();
    let mut current_label = String::new();
    let mut last_sent = Instant::now() - Duration::from_secs(1);

    while let Some(event) = receiver.recv().await {
        let Event::Progress(ProgressEvent::Grouped(event)) = event else {
            // Notifications and prompts have no place in this window: Java is installed without
            // asking (auto-install) and errors come back as the launch's own result.
            continue;
        };
        let mut force = false;
        match event {
            GroupedProgressEvent::Start { .. } | GroupedProgressEvent::End { .. } | GroupedProgressEvent::Abort { .. } => {
                categories.clear();
                child_category.clear();
                continue;
            }
            GroupedProgressEvent::Expect { category, total, .. } => {
                categories.entry(category).or_default().expected += total;
            }
            GroupedProgressEvent::AddChild { child_id, total, category, .. } => {
                categories.entry(category).or_default().children.insert(child_id, (0, total));
                child_category.insert(child_id, category);
                let label = match category {
                    TaskCategory::Java => crate::i18n::tr(crate::i18n::S::DownloadingJava).to_string(),
                    other => crate::i18n::trf(crate::i18n::S::Downloading, &[&other.label()]),
                };
                force = label != current_label;
                current_label = label;
            }
            GroupedProgressEvent::UpdateChild { child_id, current, total, .. } => {
                if let Some(category) = child_category.get(&child_id)
                    && let Some(entry) = categories.get_mut(category).and_then(|c| c.children.get_mut(&child_id))
                {
                    *entry = (current, total);
                }
            }
            GroupedProgressEvent::FinishChild { child_id, .. } => {
                if let Some(category) = child_category.get(&child_id)
                    && let Some(entry) = categories.get_mut(category).and_then(|c| c.children.get_mut(&child_id))
                {
                    entry.0 = entry.1;
                }
            }
            GroupedProgressEvent::SetChildPhase { .. } => continue,
        }

        // At most about 15 updates a second reach the window
        if !force && last_sent.elapsed() < Duration::from_millis(66) {
            continue;
        }
        last_sent = Instant::now();
        let (mut current, mut total) = (0u64, 0u64);
        for category in categories.values() {
            let (done, sum) = category
                .children
                .values()
                .fold((0, 0), |(d, s), &(c, t)| (d + c.min(t), s + t));
            current += done;
            total += sum.max(category.expected);
        }
        let _ = ui.send(LauncherEvent::Progress {
            task: if current_label.is_empty() { crate::i18n::tr(crate::i18n::S::DownloadingGameFiles).into() } else { current_label.clone() },
            current,
            total: total.max(1),
        });
    }
}

fn status(ui: &EventSender, text: crate::i18n::S) {
    let _ = ui.send(LauncherEvent::Status(crate::i18n::Text::key(text, &[])));
}

fn versions_at_least(version: &str, target: &str) -> bool {
    let parse = |v: &str| v.split('.').map(|p| p.parse::<u64>().unwrap_or(0)).collect::<Vec<_>>();
    let (a, b) = (parse(version), parse(target));
    for i in 0..a.len().max(b.len()) {
        let (x, y) = (a.get(i).copied().unwrap_or(0), b.get(i).copied().unwrap_or(0));
        if x != y {
            return x > y;
        }
    }
    true
}

/// Join the server straight away: Quick Play from 1.20 on, the old switches before that.
pub fn join_args(config: &Config) -> Vec<String> {
    let (host, port) = (&config.server_host, config.server_port);
    if versions_at_least(&config.minecraft_version, "1.20") {
        vec!["--quickPlayMultiplayer".into(), format!("{host}:{port}")]
    } else {
        vec!["--server".into(), host.clone(), "--port".into(), port.to_string()]
    }
}

/// Writes the token where the client mod reads it (the mod deletes it after reading).
fn write_token_file(config: &Config, root: &Path, username: &str, join_token: Option<&str>) -> Result<(), String> {
    let file = root.join("config").join(&config.token_file_name);
    std::fs::create_dir_all(file.parent().unwrap_or(root)).map_err(|e| e.to_string())?;
    match join_token {
        Some(token) => {
            let created_at = SystemTime::now().duration_since(UNIX_EPOCH).map_or(0, |d| d.as_millis() as u64);
            let body = serde_json::json!({ "username": username, "token": token, "createdAt": created_at });
            std::fs::write(&file, body.to_string()).map_err(|e| e.to_string())
        }
        None => {
            if file.exists() {
                std::fs::remove_file(&file).map_err(|e| e.to_string())?;
            }
            Ok(())
        }
    }
}

/// "21.0.5" -> 21, "1.8.0_402" -> 8
fn java_major(version: &str) -> Option<u32> {
    let mut parts = version.split(['.', '_', '-', '+']);
    let first: u32 = parts.next()?.parse().ok()?;
    if first == 1 { parts.next()?.parse().ok() } else { Some(first) }
}

struct Java {
    path: String,
    os_arch: String,
}

async fn pick_java(core: &Core, config: &Config, required: u32, native_ok: bool, progress: &GroupedProgressSession) -> Result<Java, String> {
    if !config.java_path.trim().is_empty() {
        let path = config.java_path.trim().to_string();
        let info = oneclient_java::check_java_runtime(path.clone())
            .await
            .map_err(|err| crate::i18n::trf(crate::i18n::S::JavaBroken, &[&path, &err]))?;
        if java_major(&info.version).is_some_and(|major| major < required) {
            return Err(crate::i18n::trf(crate::i18n::S::JavaTooOld, &[&info.version, &config.minecraft_version, &required]));
        }
        return Ok(Java { path, os_arch: info.os_arch });
    }
    let runtime = core
        .java
        .prepare(required, native_ok, true, true, Some(progress))
        .await
        .map_err(|err| crate::i18n::trf(crate::i18n::S::JavaFailed, &[&required, &err]))?;
    Ok(Java { path: runtime.absolute_path, os_arch: runtime.os_arch })
}

/// Downloads whatever is missing and starts the game. Returns once the game process is running;
/// its output arrives as [`LauncherEvent::Log`] and its end as [`LauncherEvent::GameClosed`].
pub async fn launch(config: &Config, session: &Session, join_token: Option<String>, ui: EventSender) -> Result<(), String> {
    let core = core().clone();
    let root = paths::game_dir().to_path_buf();
    std::fs::create_dir_all(&root).map_err(|e| crate::i18n::trf(crate::i18n::S::GameFolderFailed, &[&e]))?;
    let game_lang = if crate::i18n::lang() == crate::i18n::Lang::Vi { "vi_vn" } else { "en_us" };
    if let Err(err) = ensure_game_language(&root.join("options.txt"), game_lang) {
        tracing::warn!("could not set the game language: {err}");
    }
    let reports_before = crash_reports(&root);
    let mc_version = config.minecraft_version.clone();

    let progress = GroupedProgressSession::start(&core.events, "Starting Minecraft");

    let (version, updated, loader, version_info) = {
        let mut metadata = core.metadata.lock().await;
        let (version, _, updated) = oneclient_mc::resolve_minecraft_version(&mut metadata, &core.ctx, &mc_version)
            .await
            .map_err(|err| crate::i18n::trf(crate::i18n::S::MinecraftNotFound, &[&mc_version, &err]))?;
        let loader = oneclient_mc::get_loader_version(
            &mut metadata,
            &core.ctx,
            &mc_version,
            GameLoader::Fabric,
            Some(config.fabric_loader_version.as_str()),
        )
        .await
        .map_err(|err| crate::i18n::trf(crate::i18n::S::FabricFailed, &[&config.fabric_loader_version, &err]))?;
        let info = oneclient_mc::download_version_info(&core.ctx, Some(&progress), &version, GameLoader::Fabric, loader.as_ref(), false)
            .await
            .map_err(|err| crate::i18n::trf(crate::i18n::S::VersionFileFailed, &[&err]))?;
        (version, updated, loader, info)
    };

    let version_name = loader.as_ref().map_or_else(|| version.id.clone(), |lv| format!("{}-{}", version.id, lv.id));
    let native_ok = oneclient_mc::natives_cover(
        &version_info.libraries,
        HostTarget::native().arch.os_arch(),
        HostTarget::CURRENT.arch.os_arch(),
    );
    let required = version_info.java_version.as_ref().map_or(21, |v| v.major_version);
    let java = pick_java(&core, config, required, native_ok, &progress).await?;

    let missing = oneclient_mc::game_files_missing(&version_info, &java.os_arch, updated).unwrap_or(true);
    if missing {
        oneclient_mc::download_minecraft(&core.ctx, &progress, &version_info, &java.os_arch, updated, false, NativesMode::Extract)
            .await
            .map_err(|err| crate::i18n::trf(crate::i18n::S::MinecraftDownloadFailed, &[&err]))?;
    }
    // (On an error above the session ends when dropped.)
    progress.finish();
    accept_progress(false);
    status(&ui, crate::i18n::S::StartingMinecraft);

    let err = |e: oneclient_mc::McError| e.to_string();
    let path_err = |e: oneclient_common::PathsError| e.to_string();
    let client_jar = one_paths::versions_dir().map_err(path_err)?.join(&version_name).join(format!("{version_name}.jar"));
    let natives = one_paths::natives_dir().map_err(path_err)?.join(&version_name);
    std::fs::create_dir_all(&natives).map_err(|e| e.to_string())?;
    let libraries = one_paths::libraries_dir().map_err(path_err)?;
    let assets = one_paths::assets_dir().map_err(path_err)?;
    let arg_map = version_info.arguments.clone().unwrap_or_default();

    let classpaths = oneclient_mc::classpaths(&libraries, &version_info.libraries, &client_jar, &java.os_arch, updated).map_err(err)?;
    // -Xmx from the Settings panel (config.json's memory.max until the player changes it)
    let mem_max = crate::settings::memory_gb(memory_mb(&config.memory.max).unwrap_or(4096)) * 1024;
    let extra_jvm = memory_mb(&config.memory.min).map(|min| format!("-Xms{}M", min.min(mem_max))).unwrap_or_default();
    let mut jvm_args = oneclient_mc::java_arguments(
        arg_map.get(&ArgumentType::Jvm).map(Vec::as_slice),
        &natives,
        &libraries,
        &classpaths,
        &version_name,
        mem_max,
        extra_jvm.clone(),
        &java.os_arch,
        required,
    )
    .map_err(err)?;
    // OneLauncher adds its own -Xms (512M at most); keep only ours, from config memory.min
    if !extra_jvm.is_empty() {
        jvm_args.retain(|arg| !arg.starts_with("-Xms"));
        jvm_args.insert(0, extra_jvm);
    }

    let account = oneclient_auth::offline_account(session.username.clone());
    let mut mc_args = oneclient_mc::minecraft_arguments(
        arg_map.get(&ArgumentType::Game).map(Vec::as_slice),
        version_info.minecraft_arguments.as_deref(),
        // Offline: a placeholder token, like every offline launcher
        "0",
        &account.username,
        account.id,
        &version.id,
        &version_info.asset_index.id,
        &root,
        &assets,
        version.type_,
        Resolution::default(),
        &java.os_arch,
    )
    .map_err(err)?;
    mc_args.extend(join_args(config));

    write_token_file(config, &root, &session.username, join_token.as_deref())?;

    tracing::info!(java = %java.path, version = %version_name, "starting minecraft");
    let mut command = tokio::process::Command::new(&java.path);
    command
        .args(&jvm_args)
        .arg(&version_info.main_class)
        .args(&mc_args)
        .current_dir(&root)
        .stdin(Stdio::null())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped());
    #[cfg(windows)]
    {
        // No console window, and cut loose from the launcher's process group
        const DETACHED_PROCESS: u32 = 0x0000_0008;
        const CREATE_NEW_PROCESS_GROUP: u32 = 0x0000_0200;
        command.as_std_mut().creation_flags(DETACHED_PROCESS | CREATE_NEW_PROCESS_GROUP);
    }
    #[cfg(windows)]
    use std::os::windows::process::CommandExt as _;

    let mut child = command.spawn().map_err(|e| {
        let _ = write_token_file(config, &root, &session.username, None); // no game will read it
        crate::i18n::trf(crate::i18n::S::MinecraftStartFailed, &[&e])
    })?;

    for stream in [child.stdout.take().map(|s| Box::new(s) as Box<dyn tokio::io::AsyncRead + Unpin + Send>),
                   child.stderr.take().map(|s| Box::new(s) as Box<dyn tokio::io::AsyncRead + Unpin + Send>)]
        .into_iter()
        .flatten()
    {
        let ui = ui.clone();
        tokio::spawn(async move {
            let mut reader = BufReader::new(stream);
            let mut buf = Vec::new();
            loop {
                buf.clear();
                match reader.read_until(b'\n', &mut buf).await {
                    Ok(0) | Err(_) => break,
                    Ok(_) => drop(ui.send(LauncherEvent::Log(decode_line(&buf)))),
                }
            }
        });
    }

    let ui_close = ui.clone();
    let started = Instant::now();
    tokio::spawn(async move {
        let code = child.wait().await.ok().and_then(|status| status.code());
        release();
        let new_report = crash_reports(&root) > reports_before;
        let _ = ui_close.send(LauncherEvent::GameClosed(code, is_crash(code, started.elapsed(), new_report)));
    });
    Ok(())
}

/// Gives the game the launcher's language on its first run: adds `lang:<code>` to options.txt
/// (creating it) only when there is no `lang:` line yet, so a language picked in game stays.
pub fn ensure_game_language(file: &std::path::Path, code: &str) -> std::io::Result<()> {
    let mut text = match std::fs::read_to_string(file) {
        Ok(text) => text,
        Err(err) if err.kind() == std::io::ErrorKind::NotFound => String::new(),
        Err(err) => return Err(err),
    };
    if text.lines().any(|line| line.starts_with("lang:")) {
        return Ok(());
    }
    if !text.is_empty() && !text.ends_with('\n') {
        text.push('\n');
    }
    text.push_str(&format!("lang:{code}\n"));
    std::fs::write(file, text)
}

/// Files in the game's crash-reports folder.
fn crash_reports(root: &std::path::Path) -> usize {
    std::fs::read_dir(root.join("crash-reports")).map_or(0, |dir| dir.flatten().count())
}

/// A crash: Minecraft wrote a crash report, or it exited with an error within its first minute.
pub fn is_crash(code: Option<i32>, ran: Duration, new_report: bool) -> bool {
    new_report || (code != Some(0) && ran < Duration::from_secs(60))
}

#[cfg(test)]
mod tests {
    #[test]
    fn game_language_is_added_once_and_keeps_other_options() {
        let dir = std::env::temp_dir().join(format!("newgen-options-test-{}", std::process::id()));
        let _ = std::fs::remove_dir_all(&dir);
        std::fs::create_dir_all(&dir).unwrap();
        let file = dir.join("options.txt");
        super::ensure_game_language(&file, "vi_vn").unwrap();
        assert_eq!(std::fs::read_to_string(&file).unwrap(), "lang:vi_vn\n", "created with just that line");
        std::fs::write(&file, "fov:0.5\ngamma:1.0").unwrap();
        super::ensure_game_language(&file, "vi_vn").unwrap();
        assert_eq!(std::fs::read_to_string(&file).unwrap(), "fov:0.5\ngamma:1.0\nlang:vi_vn\n");
        super::ensure_game_language(&file, "en_us").unwrap();
        assert_eq!(std::fs::read_to_string(&file).unwrap(), "fov:0.5\ngamma:1.0\nlang:vi_vn\n", "a set language stays");
        let _ = std::fs::remove_dir_all(&dir);
    }

    #[test]
    fn crash_rules() {
        use std::time::Duration;
        assert!(super::is_crash(Some(1), Duration::from_secs(10), false));
        assert!(super::is_crash(None, Duration::from_secs(10), false));
        assert!(!super::is_crash(Some(0), Duration::from_secs(10), false), "a clean quit");
        assert!(!super::is_crash(Some(1), Duration::from_secs(600), false), "an error after a long session");
        assert!(super::is_crash(Some(0), Duration::from_secs(600), true), "a crash report always counts");
    }
    use super::*;

    #[test]
    fn version_compare() {
        assert!(versions_at_least("1.21.11", "1.20"));
        assert!(versions_at_least("1.20", "1.20"));
        assert!(!versions_at_least("1.19.4", "1.20"));
    }

    #[test]
    fn auto_join_switches() {
        let mut config = crate::config::config().clone();
        config.server_host = "play.example.net".into();
        config.server_port = 25565;
        config.minecraft_version = "1.21.11".into();
        assert_eq!(join_args(&config), ["--quickPlayMultiplayer", "play.example.net:25565"]);
        config.minecraft_version = "1.19.2".into();
        assert_eq!(join_args(&config), ["--server", "play.example.net", "--port", "25565"]);
    }

    #[test]
    fn game_output_lines() {
        assert_eq!(decode_line(b"hello\r\n"), "hello");
        assert_eq!(decode_line(b"last"), "last");
        assert_eq!(decode_line(b"caf\xe9\n"), "caf\u{fffd}"); // cp1252 byte, not UTF-8
    }

    #[test]
    fn java_majors() {
        assert_eq!(java_major("21.0.5"), Some(21));
        assert_eq!(java_major("17"), Some(17));
        assert_eq!(java_major("1.8.0_402"), Some(8));
        assert_eq!(java_major("x"), None);
    }

    #[test]
    fn offline_uuid_matches_offline_servers() {
        // Same value the Electron launcher computed (UUID v3 of "OfflinePlayer:<name>")
        assert_eq!(
            oneclient_auth::offline_uuid("Notch").to_string(),
            "b50ad385-829d-3141-a216-7e7d7539ba7f"
        );
    }
}

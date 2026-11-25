//! Where things live.
//!
//! - Launcher folder `%APPDATA%\Newgen Launcher`: settings, the record of installed mods, and
//!   OneLauncher's shared game files (`metadata\`: Minecraft versions, libraries, assets, Java).
//! - Game folder `%APPDATA%\.<folderName>`: the Minecraft game directory (mods, config, saves),
//!   the same folder the Electron launcher used.
//! - Videos and bundled mods sit next to the launcher's .exe (`videos\`, `mods\`); a debug build
//!   run from the repository uses the repository's own folders instead.

use std::path::{Path, PathBuf};
use std::sync::OnceLock;

use crate::config::config;

pub const LAUNCHER_FOLDER: &str = "Newgen Launcher";

fn roaming() -> PathBuf {
    directories::BaseDirs::new()
        .map(|dirs| dirs.data_dir().to_path_buf())
        .unwrap_or_else(|| PathBuf::from("."))
}

pub fn launcher_dir() -> &'static Path {
    static DIR: OnceLock<PathBuf> = OnceLock::new();
    DIR.get_or_init(|| roaming().join(LAUNCHER_FOLDER))
}

pub fn game_dir() -> &'static Path {
    static DIR: OnceLock<PathBuf> = OnceLock::new();
    DIR.get_or_init(|| roaming().join(format!(".{}", config().folder_name)))
}

pub fn settings_file() -> PathBuf {
    launcher_dir().join("settings.json")
}

pub fn installed_mods_record() -> PathBuf {
    launcher_dir().join("installed-mods.json")
}

fn exe_dir() -> Option<PathBuf> {
    std::env::current_exe().ok()?.parent().map(Path::to_path_buf)
}

/// The first of `candidates` that is a folder, else the first candidate.
fn first_dir(candidates: Vec<PathBuf>) -> PathBuf {
    candidates
        .iter()
        .find(|dir| dir.is_dir())
        .cloned()
        .unwrap_or_else(|| candidates[0].clone())
}

fn repo_root() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR")).join("..")
}

/// Background videos (README "Background video").
pub fn videos_dir() -> PathBuf {
    let mut candidates: Vec<PathBuf> = exe_dir().map(|dir| dir.join("videos")).into_iter().collect();
    if cfg!(debug_assertions) || candidates.iter().all(|dir| !dir.is_dir()) {
        candidates.push(Path::new(env!("CARGO_MANIFEST_DIR")).join("videos"));
    }
    first_dir(candidates)
}

/// The mods shipped with the launcher (README "Mods").
pub fn bundled_mods_dir() -> PathBuf {
    let mut candidates: Vec<PathBuf> = exe_dir().map(|dir| dir.join("mods")).into_iter().collect();
    if cfg!(debug_assertions) || candidates.iter().all(|dir| !dir.is_dir()) {
        candidates.push(repo_root().join("mods"));
    }
    first_dir(candidates)
}

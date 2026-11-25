//! The launcher's own settings file (`%APPDATA%\Newgen Launcher\settings.json`): the last name
//! that logged in, the game's maximum memory and whether the launcher stays open while playing.

use std::path::Path;

use serde_json::{Map, Value};

use crate::paths;

/// Bounds of the RAM slider, GB.
pub const MEMORY_GB: (u32, u32) = (2, 12);

fn load_from(file: &Path) -> Map<String, Value> {
    std::fs::read(file)
        .ok()
        .and_then(|bytes| serde_json::from_slice(&bytes).ok())
        .unwrap_or_default()
}

fn save_to(file: &Path, key: &str, value: Value) {
    let mut settings = load_from(file);
    settings.insert(key.into(), value);
    let result = file
        .parent()
        .map_or(Ok(()), std::fs::create_dir_all)
        .and_then(|()| std::fs::write(file, serde_json::to_vec_pretty(&settings).unwrap_or_default()));
    if let Err(err) = result {
        tracing::warn!("could not save settings to {}: {err}", file.display());
    }
}

fn load() -> Map<String, Value> {
    load_from(&paths::settings_file())
}

fn save(key: &str, value: Value) {
    save_to(&paths::settings_file(), key, value);
}

pub fn saved_username() -> String {
    load()
        .get("username")
        .and_then(Value::as_str)
        .unwrap_or_default()
        .to_string()
}

pub fn save_username(username: &str) {
    save("username", Value::String(username.to_string()));
}

fn memory_gb_in(settings: &Map<String, Value>, default_mb: u32) -> u32 {
    let gb = settings.get("memoryMaxGb").and_then(Value::as_u64).map_or(default_mb.div_ceil(1024), |gb| gb as u32);
    gb.clamp(MEMORY_GB.0, MEMORY_GB.1)
}

/// The game's maximum memory, GB; `default_mb` (config.json `memory.max`) until changed.
pub fn memory_gb(default_mb: u32) -> u32 {
    memory_gb_in(&load(), default_mb)
}

pub fn save_memory_gb(gb: u32) {
    save("memoryMaxGb", Value::from(gb.clamp(MEMORY_GB.0, MEMORY_GB.1)));
}

/// Whether the launcher stays open while the game runs (default yes; no = minimized).
pub fn keep_open() -> bool {
    load().get("keepLauncherOpen").and_then(Value::as_bool).unwrap_or(true)
}

pub fn save_keep_open(on: bool) {
    save("keepLauncherOpen", Value::Bool(on));
}

/// The language picked in the launcher ("en" / "vi"), if any.
pub fn saved_lang() -> Option<String> {
    load().get("language").and_then(Value::as_str).map(str::to_string)
}

pub fn save_lang(code: &str) {
    save("language", Value::String(code.to_string()));
}

/// The launcher version whose "What's new" was last shown.
pub fn last_seen_version() -> Option<String> {
    load().get("lastSeenVersion").and_then(Value::as_str).map(str::to_string)
}

pub fn save_last_seen_version(version: &str) {
    save("lastSeenVersion", Value::String(version.to_string()));
}

/// Whether the launcher was left fullscreen (default no).
pub fn fullscreen() -> bool {
    load().get("fullscreen").and_then(Value::as_bool).unwrap_or(false)
}

pub fn save_fullscreen(on: bool) {
    save("fullscreen", Value::Bool(on));
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn settings_round_trip_and_keep_other_keys() {
        let dir = std::env::temp_dir().join(format!("newgen-settings-test-{}", std::process::id()));
        let file = dir.join("settings.json");
        let _ = std::fs::remove_dir_all(&dir);

        assert_eq!(memory_gb_in(&load_from(&file), 4096), 4, "config default before anything is saved");
        save_to(&file, "username", Value::from("Steve"));
        save_to(&file, "memoryMaxGb", Value::from(6));
        save_to(&file, "keepLauncherOpen", Value::from(false));
        let settings = load_from(&file);
        assert_eq!(settings.get("username").and_then(Value::as_str), Some("Steve"));
        assert_eq!(memory_gb_in(&settings, 4096), 6);
        assert_eq!(settings.get("keepLauncherOpen").and_then(Value::as_bool), Some(false));

        save_to(&file, "memoryMaxGb", Value::from(64));
        assert_eq!(memory_gb_in(&load_from(&file), 4096), MEMORY_GB.1, "clamped to the slider");
        let _ = std::fs::remove_dir_all(&dir);
    }
}

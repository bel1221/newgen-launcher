//! `config.json` at the root of the repository: server address, game versions, API and
//! manifest addresses, RAM and links. It is built into the launcher, as before (README "Files").

use std::collections::BTreeMap;
use std::sync::LazyLock;

use serde::Deserialize;

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Config {
    pub server_name: String,
    pub folder_name: String,
    pub server_host: String,
    pub server_port: u16,
    pub minecraft_version: String,
    pub fabric_loader_version: String,
    #[serde(default)]
    pub api_base_url: String,
    #[serde(default)]
    pub manifest_url: String,
    pub token_file_name: String,
    #[serde(default)]
    pub java_path: String,
    pub memory: Memory,
    #[serde(default)]
    pub links: BTreeMap<String, String>,
    /// One sentence under the logo on the Play screen, per language (a plain string serves
    /// both); the line is hidden when empty.
    #[serde(default = "default_tagline")]
    pub tagline: Tagline,
    /// The website's live statistics (JSON `{ "online": N, "totalJoined": N, … }`), shown as
    /// "N players online" on the Play screen; empty = the line shows without a count.
    #[serde(default = "default_status_url")]
    pub status_url: String,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(untagged)]
pub enum Tagline {
    Plain(String),
    PerLanguage { vi: String, en: String },
}

fn default_tagline() -> Tagline {
    Tagline::PerLanguage { vi: "Server Battle Royale thế hệ mới".into(), en: "The next-generation Battle Royale server".into() }
}

impl Tagline {
    pub fn text(&self, lang: crate::i18n::Lang) -> &str {
        match self {
            Self::Plain(text) => text,
            Self::PerLanguage { vi, .. } if lang == crate::i18n::Lang::Vi => vi,
            Self::PerLanguage { en, .. } => en,
        }
        .trim()
    }
}

fn default_status_url() -> String {
    "https://www.newgenmc.net/api/stats".into()
}

#[derive(Debug, Clone, Deserialize)]
pub struct Memory {
    pub min: String,
    pub max: String,
}

static CONFIG: LazyLock<Config> = LazyLock::new(|| {
    serde_json::from_str(include_str!("../../config.json")).expect("config.json is not valid")
});

pub fn config() -> &'static Config {
    &CONFIG
}

/// The launcher's own version (Cargo.toml), shown in the corner of the window.
pub const VERSION: &str = env!("CARGO_PKG_VERSION");

impl Config {
    /// Offline dev mode: no account API, any valid name logs in and no password is asked.
    pub fn auth_enabled(&self) -> bool {
        !self.api_base_url.trim().is_empty()
    }

    /// A link button exists only for a name written in config.json whose address is a
    /// valid `https://` address (README "Links").
    pub fn link_url(&self, name: &str) -> Option<String> {
        let raw = self.links.get(name)?;
        let url = url::Url::parse(raw).ok()?;
        (url.scheme() == "https").then(|| url.to_string())
    }
}

/// "4G" / "2048M" / "2048" (MB) as megabytes, the unit OneLauncher's argument builder takes.
pub fn memory_mb(value: &str) -> Option<u32> {
    let value = value.trim();
    let (digits, unit) = match value.char_indices().find(|(_, c)| !c.is_ascii_digit()) {
        Some((at, _)) => (&value[..at], value[at..].trim()),
        None => (value, ""),
    };
    let amount: u32 = digits.parse().ok()?;
    match unit.to_ascii_uppercase().as_str() {
        "" | "M" | "MB" => Some(amount),
        "G" | "GB" => amount.checked_mul(1024),
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn memory_units() {
        assert_eq!(memory_mb("4G"), Some(4096));
        assert_eq!(memory_mb("2048M"), Some(2048));
        assert_eq!(memory_mb("1536"), Some(1536));
        assert_eq!(memory_mb("2 GB"), Some(2048));
        assert_eq!(memory_mb("lots"), None);
    }

    #[test]
    fn links_must_be_https() {
        let mut config = config().clone();
        config.links.insert("a".into(), "https://example.com/x".into());
        config.links.insert("b".into(), "http://example.com".into());
        config.links.insert("c".into(), "".into());
        config.links.insert("d".into(), "javascript:alert(1)".into());
        assert_eq!(config.link_url("a").as_deref(), Some("https://example.com/x"));
        assert_eq!(config.link_url("b"), None);
        assert_eq!(config.link_url("c"), None);
        assert_eq!(config.link_url("d"), None);
        assert_eq!(config.link_url("missing"), None);
    }

    #[test]
    fn shipped_config_parses() {
        let config = config();
        assert!(!config.server_host.is_empty());
        assert!(memory_mb(&config.memory.max).is_some());
        assert!(memory_mb(&config.memory.min).is_some());
    }
}

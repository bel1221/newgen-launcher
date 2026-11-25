//! "What's new": the changelog shipped with each build (`assets/newgen/whats-new.json`,
//! `[{ "version": "0.2.0", "en": [...], "vi": [...] }]`), shown once on Home after the launcher's
//! version changes. The last version shown is kept in settings.json.

use serde::Deserialize;

use crate::i18n::Lang;

#[derive(Debug, Clone, Deserialize, PartialEq)]
pub struct Entry {
    pub version: String,
    #[serde(default)]
    pub en: Vec<String>,
    #[serde(default)]
    pub vi: Vec<String>,
}

impl Entry {
    /// The lines in `lang`, falling back to the other language when that list is empty.
    pub fn lines(&self, lang: Lang) -> &[String] {
        let (first, other) = if lang == Lang::Vi { (&self.vi, &self.en) } else { (&self.en, &self.vi) };
        if first.is_empty() { other } else { first }
    }
}

pub fn parse(json: &[u8]) -> Vec<Entry> {
    serde_json::from_slice(json).unwrap_or_default()
}

/// "0.10.1" → [0, 10, 1], compared number by number.
fn version_key(version: &str) -> Vec<u64> {
    version.trim().split('.').map(|part| part.trim().parse().unwrap_or(0)).collect()
}

/// The entry to show: the one for `current` when it has lines and `current` is newer than the
/// version last shown (so going back to an older build never shows it again).
pub fn to_show<'a>(entries: &'a [Entry], current: &str, last_seen: Option<&str>) -> Option<&'a Entry> {
    if last_seen.is_some_and(|seen| version_key(current) <= version_key(seen)) {
        return None;
    }
    entries.iter().find(|e| e.version == current && !(e.en.is_empty() && e.vi.is_empty()))
}

/// The bundled changelog.
pub fn bundled() -> Vec<Entry> {
    crate::ui::assets::Assets::get_bytes("newgen/whats-new.json").map(|bytes| parse(&bytes)).unwrap_or_default()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn entries() -> Vec<Entry> {
        parse(br#"[{"version":"0.2.0","en":["a","b"],"vi":["x"]},{"version":"0.3.0","en":["c"]},{"version":"0.4.0"}]"#)
    }

    #[test]
    fn shown_once_per_new_version() {
        let all = entries();
        assert_eq!(to_show(&all, "0.2.0", None).map(|e| e.version.as_str()), Some("0.2.0"), "first start");
        assert!(to_show(&all, "0.2.0", Some("0.2.0")).is_none(), "already shown");
        assert_eq!(to_show(&all, "0.3.0", Some("0.2.0")).map(|e| e.version.as_str()), Some("0.3.0"), "after an update");
        assert!(to_show(&all, "0.4.0", Some("0.3.0")).is_none(), "an entry without lines");
        assert!(to_show(&all, "0.5.0", Some("0.3.0")).is_none(), "no entry for this version");
        assert!(to_show(&all, "0.2.0", Some("0.3.0")).is_none(), "a downgrade does not show it again");
        assert!(to_show(&all, "0.3.0", Some("0.10.0")).is_none(), "compared as numbers, not text");
    }

    #[test]
    fn lines_follow_the_language_with_a_fallback() {
        let all = entries();
        assert_eq!(all[0].lines(Lang::Vi), ["x"]);
        assert_eq!(all[1].lines(Lang::Vi), ["c"], "no Vietnamese: English");
    }

    #[test]
    fn bundled_changelog_has_this_version() {
        let all = bundled();
        let entry = all.iter().find(|e| e.version == crate::config::VERSION).expect("an entry for this version");
        assert!(!entry.en.is_empty() && !entry.vi.is_empty());
    }
}

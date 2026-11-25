//! Keeps the player's mods / resource packs / configs identical to the server's list
//! (README "Manifest format"). Skipped when `manifestUrl` is empty.

use std::collections::HashSet;
use std::path::{Component, Path, PathBuf};

use serde::Deserialize;
use sha1::{Digest, Sha1};

use crate::config::Config;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct Manifest {
    #[serde(default)]
    managed_folders: Option<Vec<String>>,
    #[serde(default)]
    files: Option<Vec<ManifestFile>>,
}

#[derive(Deserialize)]
struct ManifestFile {
    path: String,
    url: String,
    /// String or number in the wild (sync.js compared its string form)
    sha1: serde_json::Value,
}

fn sha1_text(value: &serde_json::Value) -> String {
    match value {
        serde_json::Value::String(s) => s.to_ascii_lowercase(),
        other => other.to_string().to_ascii_lowercase(),
    }
}

pub struct Progress {
    pub task: String,
    pub current: u64,
    pub total: u64,
}

fn sha1_hex(data: &[u8]) -> String {
    Sha1::digest(data).iter().map(|b| format!("{b:02x}")).collect()
}

/// A manifest path inside the game folder. Absolute paths and `..` are refused outright,
/// so nothing can point outside it.
pub fn safe_resolve(root: &Path, rel: &str) -> Result<PathBuf, String> {
    let rel_path = Path::new(rel);
    let mut out = root.to_path_buf();
    let mut any = false;
    for part in rel_path.components() {
        match part {
            Component::Normal(name) => {
                out.push(name);
                any = true;
            }
            Component::CurDir => {}
            _ => return Err(crate::i18n::trf(crate::i18n::S::UnsafePath, &[&rel])),
        }
    }
    if !any {
        return Err(crate::i18n::trf(crate::i18n::S::UnsafePath, &[&rel]));
    }
    Ok(out)
}

pub async fn sync_files(
    config: &Config,
    root: &Path,
    mut on_progress: impl FnMut(Progress),
) -> Result<(), String> {
    if config.manifest_url.trim().is_empty() {
        return Ok(());
    }
    // No total timeout: big files may take long. A stalled server still fails (idle read timeout).
    let client = reqwest::Client::builder()
        .connect_timeout(std::time::Duration::from_secs(15))
        .read_timeout(std::time::Duration::from_secs(30))
        .build()
        .map_err(|e| e.to_string())?;
    let fail = || crate::i18n::tr(crate::i18n::S::FileListFailed).to_string();
    let response = client.get(&config.manifest_url).send().await.map_err(|_| fail())?;
    if !response.status().is_success() {
        return Err(fail());
    }
    let manifest: Manifest = response.json().await.map_err(|_| fail())?;
    let files = manifest.files.unwrap_or_default();
    let total = files.len() as u64;
    let mut wanted = HashSet::new();

    for (i, file) in files.iter().enumerate() {
        let dest = safe_resolve(root, &file.path)?;
        let expected = sha1_text(&file.sha1);
        wanted.insert(dest.clone());
        on_progress(Progress { task: crate::i18n::trf(crate::i18n::S::CheckingFile, &[&file.path]), current: i as u64, total });

        if let Ok(existing) = tokio::fs::read(&dest).await
            && sha1_hex(&existing) == expected
        {
            continue;
        }

        let failed = || crate::i18n::trf(crate::i18n::S::DownloadFailed, &[&file.path]);
        let response = client.get(&file.url).send().await.map_err(|_| failed())?;
        if !response.status().is_success() {
            return Err(failed());
        }
        let data = response.bytes().await.map_err(|_| failed())?;
        if sha1_hex(&data) != expected {
            return Err(crate::i18n::trf(crate::i18n::S::DownloadCorrupted, &[&file.path]));
        }
        if let Some(parent) = dest.parent() {
            tokio::fs::create_dir_all(parent).await.map_err(|e| e.to_string())?;
        }
        tokio::fs::write(&dest, &data).await.map_err(|e| e.to_string())?;
    }

    // Delete anything in managed folders that isn't on the list
    // (removes old versions and mods players added themselves).
    let folders = manifest.managed_folders.unwrap_or_else(|| vec!["mods".into()]);
    for folder in folders {
        let dir = safe_resolve(root, &folder)?;
        let Ok(entries) = std::fs::read_dir(&dir) else { continue };
        for entry in entries.flatten() {
            let path = entry.path();
            // metadata follows symlinks, like sync.js statSync().isFile()
            if std::fs::metadata(&path).is_ok_and(|m| m.is_file()) && !wanted.contains(&path) {
                let _ = std::fs::remove_file(&path);
            }
        }
    }

    on_progress(Progress { task: crate::i18n::tr(crate::i18n::S::FilesUpToDate).into(), current: total, total });
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn manifest_paths_stay_inside() {
        let root = Path::new("C:/game");
        assert_eq!(safe_resolve(root, "mods/a.jar").unwrap(), root.join("mods").join("a.jar"));
        assert_eq!(safe_resolve(root, "./mods/a.jar").unwrap(), root.join("mods").join("a.jar"));
        assert!(safe_resolve(root, "../evil.jar").is_err());
        assert!(safe_resolve(root, "mods/../../evil.jar").is_err());
        assert!(safe_resolve(root, "C:/Windows/evil.dll").is_err());
        assert!(safe_resolve(root, "/etc/passwd").is_err());
        assert!(safe_resolve(root, "").is_err());
        assert!(safe_resolve(root, ".").is_err());
    }

    #[test]
    fn sha1_matches_known_value() {
        assert_eq!(sha1_hex(b"abc"), "a9993e364706816aba3e25717850c26c9cd0d89d");
    }

    #[test]
    fn lenient_manifest() {
        let m: Manifest = serde_json::from_str(r#"{"files":null}"#).unwrap();
        assert!(m.files.unwrap_or_default().is_empty());
        let m: Manifest = serde_json::from_str(
            r#"{"files":[{"path":"a","url":"u","sha1":"ABC"},{"path":"b","url":"u","sha1":123}]}"#,
        )
        .unwrap();
        let files = m.files.unwrap();
        assert_eq!(sha1_text(&files[0].sha1), "abc");
        assert_eq!(sha1_text(&files[1].sha1), "123");
    }
}

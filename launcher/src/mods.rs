//! Bundled mods (README "Mods"): the game's mods folder must hold exactly the jars shipped with
//! the launcher. Anything else is an "added mod" and blocks PLAY.
//! It is checked once, before the game starts, on the player's own PC: it stops casual attempts
//! only. Real enforcement needs a server-side check.

use std::collections::BTreeMap;
use std::path::Path;

use sha2::{Digest, Sha256};

pub enum ModsError {
    /// Names in the mods folder that are not bundled jars with the same content.
    Added(Vec<String>),
    Other(String),
}

fn sha256(file: &Path) -> std::io::Result<String> {
    let data = std::fs::read(file)?;
    Ok(Sha256::digest(&data).iter().map(|b| format!("{b:02x}")).collect())
}

/// `{ "<name>.jar": "<sha256>" }` for every jar directly in the bundled folder.
fn bundled_hashes(bundled_dir: &Path) -> BTreeMap<String, String> {
    let mut hashes = BTreeMap::new();
    let Ok(entries) = std::fs::read_dir(bundled_dir) else { return hashes };
    for entry in entries.flatten() {
        let name = entry.file_name().to_string_lossy().into_owned();
        if name.to_ascii_lowercase().ends_with(".jar")
            && let Ok(hash) = sha256(&entry.path())
        {
            hashes.insert(name, hash);
        }
    }
    hashes
}

/// Names of everything in the game's mods folder (files and subfolders) that is not a bundled jar
/// with the same content. A bundled jar swapped for another file under the same name is listed.
pub fn added_mods(bundled_dir: &Path, mods_dir: &Path) -> Vec<String> {
    let Ok(entries) = std::fs::read_dir(mods_dir) else { return Vec::new() };
    let bundled = bundled_hashes(bundled_dir);
    let mut added: Vec<String> = entries
        .flatten()
        .filter(|entry| {
            let name = entry.file_name().to_string_lossy().into_owned();
            let same = entry.file_type().is_ok_and(|t| t.is_file())
                && bundled
                    .get(&name)
                    .is_some_and(|hash| sha256(&entry.path()).is_ok_and(|h| &h == hash));
            !same
        })
        .map(|entry| entry.file_name().to_string_lossy().into_owned())
        .collect();
    added.sort();
    added
}

/// Fails with [`ModsError::Added`] when the player added something; otherwise copies in the
/// bundled jars that are missing. `record_file` remembers what the launcher itself installed, so
/// a launcher update that changes the bundled mods replaces its own old jars instead of blaming
/// the player.
pub fn install_mods(bundled_dir: &Path, mods_dir: &Path, record_file: &Path) -> Result<(), ModsError> {
    let other = |err: std::io::Error| ModsError::Other(err.to_string());
    let bundled = bundled_hashes(bundled_dir);
    if bundled.is_empty() {
        return Err(ModsError::Other(crate::i18n::tr(crate::i18n::S::ModsMissing).into()));
    }
    std::fs::create_dir_all(mods_dir).map_err(other)?;

    let record: BTreeMap<String, String> = std::fs::read(record_file)
        .ok()
        .and_then(|bytes| serde_json::from_slice(&bytes).ok())
        .unwrap_or_default(); // first run
    for (name, hash) in &record {
        // file_name: the record can never point outside mods
        let Some(base) = Path::new(name).file_name() else { continue };
        let file = mods_dir.join(base);
        if bundled.get(name) == Some(hash) || !file.is_file() {
            continue;
        }
        if sha256(&file).is_ok_and(|h| &h == hash) {
            let _ = std::fs::remove_file(&file); // still the launcher's own old jar, untouched
        }
    }

    for name in bundled.keys() {
        let _ = std::fs::remove_file(mods_dir.join(format!("{name}.part"))); // left by a crashed copy
    }
    let added = added_mods(bundled_dir, mods_dir);
    if !added.is_empty() {
        return Err(ModsError::Added(added));
    }

    for name in bundled.keys() {
        let dest = mods_dir.join(name);
        if !dest.exists() {
            // Via a temporary name, so a half-copied jar never sits under the real one
            let part = mods_dir.join(format!("{name}.part"));
            std::fs::copy(bundled_dir.join(name), &part).map_err(other)?;
            std::fs::rename(&part, &dest).map_err(other)?;
        }
    }
    if let Some(parent) = record_file.parent() {
        std::fs::create_dir_all(parent).map_err(other)?;
    }
    std::fs::write(record_file, serde_json::to_vec_pretty(&bundled).unwrap_or_default()).map_err(other)?;
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;

    fn sorted(dir: &Path) -> Vec<String> {
        let mut names: Vec<_> = fs::read_dir(dir)
            .unwrap()
            .map(|e| e.unwrap().file_name().to_string_lossy().into_owned())
            .collect();
        names.sort();
        names
    }

    // The self-check of the Electron launcher's mods.js, step by step.
    #[test]
    fn bundled_mods_self_check() {
        let tmp = std::env::temp_dir().join(format!("newgen-mods-check-{}", uuid::Uuid::new_v4()));
        let bundled_dir = tmp.join("bundled");
        let mods_dir = tmp.join("game").join("mods");
        let record = tmp.join("record.json");
        let blocked = || match install_mods(&bundled_dir, &mods_dir, &record) {
            Ok(()) => Vec::new(),
            Err(ModsError::Added(names)) => names,
            Err(ModsError::Other(err)) => panic!("{err}"),
        };

        fs::create_dir_all(&bundled_dir).unwrap();
        fs::write(bundled_dir.join("a.jar"), "A").unwrap();
        fs::write(bundled_dir.join("b.jar"), "B").unwrap();

        assert!(added_mods(&bundled_dir, &mods_dir).is_empty()); // no mods folder yet
        assert!(blocked().is_empty()); // clean: installs
        assert_eq!(sorted(&mods_dir), ["a.jar", "b.jar"]);
        assert!(added_mods(&bundled_dir, &mods_dir).is_empty());

        fs::write(mods_dir.join("cheat.jar"), "X").unwrap(); // one extra jar
        assert_eq!(added_mods(&bundled_dir, &mods_dir), ["cheat.jar"]);
        assert_eq!(blocked(), ["cheat.jar"]);
        assert!(mods_dir.join("cheat.jar").exists()); // never deleted
        fs::remove_file(mods_dir.join("cheat.jar")).unwrap();

        fs::write(mods_dir.join("a.jar"), "not A").unwrap(); // swapped jar
        assert_eq!(blocked(), ["a.jar"]);
        assert_eq!(fs::read_to_string(mods_dir.join("a.jar")).unwrap(), "not A"); // not overwritten
        fs::remove_file(mods_dir.join("a.jar")).unwrap();

        fs::create_dir(mods_dir.join("extra")).unwrap(); // extra subfolder
        assert_eq!(blocked(), ["extra"]);
        fs::remove_dir(mods_dir.join("extra")).unwrap();

        assert!(blocked().is_empty()); // removed jar is put back
        assert_eq!(fs::read_to_string(mods_dir.join("a.jar")).unwrap(), "A");

        fs::write(mods_dir.join("a.jar.part"), "A half").unwrap(); // left by a crashed copy
        assert!(blocked().is_empty() && !mods_dir.join("a.jar.part").exists());

        fs::rename(bundled_dir.join("b.jar"), bundled_dir.join("b-2.jar")).unwrap(); // launcher update
        assert!(blocked().is_empty());
        assert_eq!(sorted(&mods_dir), ["a.jar", "b-2.jar"]);

        fs::remove_dir_all(&tmp).unwrap();
    }

    #[test]
    fn no_bundled_mods_is_an_error() {
        let tmp = std::env::temp_dir().join(format!("newgen-mods-empty-{}", uuid::Uuid::new_v4()));
        let result = install_mods(&tmp.join("none"), &tmp.join("mods"), &tmp.join("r.json"));
        assert!(matches!(result, Err(ModsError::Other(_))));
        let _ = fs::remove_dir_all(&tmp);
    }
}

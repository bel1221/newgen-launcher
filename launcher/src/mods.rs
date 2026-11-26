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

/// Names of everything in the game's mods folder (files and subfolders) that is not a bundled
/// jar. A file under a bundled jar's name is never listed: [`install_mods`] overwrites it with the
/// bundled copy, so an old version or a tampered copy is undone instead of blamed on the player.
pub fn added_mods(bundled_dir: &Path, mods_dir: &Path) -> Vec<String> {
    let Ok(entries) = std::fs::read_dir(mods_dir) else { return Vec::new() };
    let bundled = bundled_hashes(bundled_dir);
    let mut added: Vec<String> = entries
        .flatten()
        .filter(|entry| {
            let name = entry.file_name().to_string_lossy().into_owned();
            !(entry.file_type().is_ok_and(|t| t.is_file()) && bundled.contains_key(&name))
        })
        .map(|entry| entry.file_name().to_string_lossy().into_owned())
        .collect();
    added.sort();
    added
}

/// Fails with [`ModsError::Added`] when the player added something; otherwise puts the bundled
/// jars in place, replacing any file under a bundled name whose content differs. `record_file`
/// remembers what the launcher itself installed, so a launcher update that drops or renames a
/// bundled jar removes its own old jar instead of blaming the player. A file that can't be
/// replaced or removed (usually: the game is still running and holds it open) fails with
/// [`ModsError::Other`], never with `Added`.
pub fn install_mods(bundled_dir: &Path, mods_dir: &Path, record_file: &Path) -> Result<(), ModsError> {
    let other = |err: std::io::Error| ModsError::Other(err.to_string());
    let locked = |err: std::io::Error| {
        ModsError::Other(format!("{} ({err})", crate::i18n::tr(crate::i18n::S::ModsLocked)))
    };
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
        // A bundled name is replaced below; only dropped/renamed jars need removing here
        if bundled.contains_key(name) || !file.is_file() {
            continue;
        }
        // Unreadable: held open by the game; don't let it fall through to "added"
        if &sha256(&file).map_err(locked)? == hash {
            std::fs::remove_file(&file).map_err(locked)?; // still the launcher's own old jar
        }
    }

    for name in bundled.keys() {
        let _ = std::fs::remove_file(mods_dir.join(format!("{name}.part"))); // left by a crashed copy
    }
    let added = added_mods(bundled_dir, mods_dir);
    if !added.is_empty() {
        return Err(ModsError::Added(added));
    }

    for (name, hash) in &bundled {
        let dest = mods_dir.join(name);
        if sha256(&dest).is_ok_and(|h| &h == hash) {
            continue;
        }
        // Via a temporary name, so a half-copied jar never sits under the real one
        let part = mods_dir.join(format!("{name}.part"));
        std::fs::copy(bundled_dir.join(name), &part).map_err(other)?;
        if let Err(err) = std::fs::rename(&part, &dest) {
            let _ = std::fs::remove_file(&part);
            return Err(locked(err));
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

        fs::write(mods_dir.join("a.jar"), "not A").unwrap(); // tampered copy under our name
        assert!(added_mods(&bundled_dir, &mods_dir).is_empty());
        assert!(blocked().is_empty());
        assert_eq!(fs::read_to_string(mods_dir.join("a.jar")).unwrap(), "A"); // replaced
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

    // The reported bug: an old version of our jar, under the bundled name, that the record no
    // longer matches (or that was never recorded), must be updated, not flagged as added.
    #[test]
    fn old_bundled_jar_is_replaced_not_flagged() {
        let tmp = std::env::temp_dir().join(format!("newgen-mods-old-{}", uuid::Uuid::new_v4()));
        let (bundled_dir, mods_dir) = (tmp.join("bundled"), tmp.join("mods"));
        fs::create_dir_all(&bundled_dir).unwrap();
        fs::create_dir_all(&mods_dir).unwrap();
        fs::write(bundled_dir.join("newgen-client-1.0.0.jar"), "new").unwrap();
        fs::write(mods_dir.join("newgen-client-1.0.0.jar"), "old").unwrap();
        fs::write(mods_dir.join("cheat.jar"), "X").unwrap();
        // A really added jar still blocks, and nothing is touched yet
        assert!(matches!(install_mods(&bundled_dir, &mods_dir, &tmp.join("r.json")),
            Err(ModsError::Added(names)) if names == ["cheat.jar"]));
        fs::remove_file(mods_dir.join("cheat.jar")).unwrap();
        assert!(install_mods(&bundled_dir, &mods_dir, &tmp.join("r.json")).is_ok());
        assert_eq!(fs::read_to_string(mods_dir.join("newgen-client-1.0.0.jar")).unwrap(), "new");
        assert_eq!(sorted(&mods_dir), ["newgen-client-1.0.0.jar"]);
        fs::remove_dir_all(&tmp).unwrap();
    }

    // A jar held open by a running game: Other (the "close Minecraft" message), never Added.
    #[cfg(windows)]
    #[test]
    fn locked_jar_is_other_not_added() {
        use std::os::windows::fs::OpenOptionsExt;
        let tmp = std::env::temp_dir().join(format!("newgen-mods-locked-{}", uuid::Uuid::new_v4()));
        let (bundled_dir, mods_dir, record) = (tmp.join("bundled"), tmp.join("mods"), tmp.join("r.json"));
        fs::create_dir_all(&bundled_dir).unwrap();
        fs::create_dir_all(&mods_dir).unwrap();
        fs::write(bundled_dir.join("a.jar"), "new").unwrap();
        // Like Java holding a jar: others may read, but not delete or replace (FILE_SHARE_READ)
        let open_exclusive = |path: &Path| fs::OpenOptions::new().read(true).share_mode(1).open(path).unwrap();

        // Replacing an outdated bundled-name jar fails
        fs::write(mods_dir.join("a.jar"), "old").unwrap();
        let hold = open_exclusive(&mods_dir.join("a.jar"));
        assert!(matches!(install_mods(&bundled_dir, &mods_dir, &record), Err(ModsError::Other(_))));
        assert!(!mods_dir.join("a.jar.part").exists());
        drop(hold);
        assert!(install_mods(&bundled_dir, &mods_dir, &record).is_ok());

        // Removing a recorded jar that the update dropped fails
        fs::write(&record, r#"{"a.jar":"x","gone.jar":"4b68ab3847feda7d6c62c1fbcbeebfa35eab7351ed5e78f4ddadea5df64b8015"}"#).unwrap(); // sha256("X")
        fs::write(mods_dir.join("gone.jar"), "X").unwrap();
        let hold = open_exclusive(&mods_dir.join("gone.jar"));
        assert!(matches!(install_mods(&bundled_dir, &mods_dir, &record), Err(ModsError::Other(_))));
        drop(hold);
        assert!(install_mods(&bundled_dir, &mods_dir, &record).is_ok());
        assert_eq!(sorted(&mods_dir), ["a.jar"]);
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

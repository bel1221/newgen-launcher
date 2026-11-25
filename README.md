# Newgen Launcher

The launcher for the NewGen Minecraft server (newgenmc.net). It is written in Rust: the window and screens use [Freya](https://github.com/marc2332/freya), and installing and starting Minecraft uses [OneLauncher](https://github.com/Polyfrost/OneLauncher)'s launcher core. It downloads Java, Minecraft 1.21.11 and Fabric, installs the server's bundled mods, refuses any mods the player adds, and starts the game already joining the server.

## Build and run

You need Rust (stable, 1.97 or newer). On Windows you also need the Visual Studio C++ Build Tools.

```
cd launcher
cargo run              # development build
cargo test             # unit tests
cargo build --release  # release build: launcher/target/release/newgen_launcher.exe
```

When you ship the release `.exe`, put the `videos\` and `mods\` folders next to it. A development build reads them from `launcher\videos\` and `mods\` in this repository instead.

## What is where

| Path | What it is |
|---|---|
| `config.json` | Server address, Minecraft and Fabric versions, account API and file-list addresses, RAM, links, the player-count address (`statusUrl`) and the tagline (`tagline`, in English and Vietnamese). It is built into the launcher. |
| `mods/` | The client mods shipped with the launcher (not in git). Anything else in the player's mods folder blocks Play. |
| `manifest.example.json` | An example of the file list the launcher syncs (`manifestUrl`). |
| `launcher/src/` | The launcher code: `game.rs` (launch, on OneLauncher's core), `sync.rs`, `mods.rs`, `auth.rs`, `status.rs`, `video.rs`, `burst.rs`, `i18n.rs` (English and Vietnamese), `ui/` (screens) |
| `launcher/assets/` | Fonts (Be Vietnam Pro, JetBrains Mono), icons, theme, NewGen logo, weapon models, website section cards |
| `launcher/videos/` | Background clips, played in file-name order (`bg-2` before `bg-10`), dissolving into each other. H.264 `.mp4` only. |
| `launcher/docs/` | UI spec, QA checklist, launcher comparison, in-game menu audit, cosmetics and emotes guides |
| `docs/reference/` | Reference screenshots (in-game menus, another launcher) |

**Folders on the player's PC:**
- `%APPDATA%\Newgen Launcher` holds the settings and the Java and Minecraft files.
- `%APPDATA%\.myserver-launcher` is the game folder.

## How a launch works

```
Log in ──► POST {apiBaseUrl}/launcher/login                 (offline dev mode if apiBaseUrl is empty)
Play   ──► sync files from manifestUrl                       (skipped if empty)
       ──► install bundled mods, refuse added mods
       ──► POST {apiBaseUrl}/launcher/join-token             (one-time token)
       ──► write <game dir>/config/launcher-auth.json         (the client mod reads and deletes it)
       ──► download Java / Minecraft / Fabric if needed, start the game with --quickPlayMultiplayer
```

The account backend, the client mod's token hand-off and the server plugin are described in `launcher/docs/` and in git history.

## Licence

The launcher uses OneLauncher's crates and adapts parts of its UI code, which are licensed GPL-3.0-only. So this launcher is GPL-3.0 too: anyone who receives the launcher must be offered its source. These are NOT covered and are not in this repository: Polyfrost's logos and art. The fonts are under the SIL Open Font License (see `launcher/assets/fonts/*/OFL.txt`).

The old Electron version of the launcher was removed on 2026-11-26. It is still in git history.

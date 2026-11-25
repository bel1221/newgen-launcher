# Attribution

Newgen Launcher is licensed under the GNU General Public License, version 3 only (see [LICENSE](LICENSE)), because it uses and adapts code from OneLauncher.

## OneLauncher
The launcher core (`oneclient_*` crates, used as dependencies) and parts of the UI code (theme, buttons, inputs, navbar and vignette patterns, adapted in `launcher/src/ui/`) come from [OneLauncher](https://github.com/Polyfrost/OneLauncher):

```
OneLauncher - Open source Rust-based Minecraft Launcher
Copyright (C) 2023~CURRENT  Polyfrost and Contributors
Licensed under the GNU General Public License, version 3.
```

Polyfrost's branding (logos, background art) is **not** used or included.

## Fonts
- Be Vietnam Pro, JetBrains Mono: SIL Open Font License 1.1 (`launcher/assets/fonts/*/OFL.txt`).

## Libraries (in the launcher)
- [Freya](https://github.com/marc2332/freya) (MIT) for the UI, [OpenH264](https://github.com/cisco/openh264) (BSD-2) for background video decoding, and the other crates listed in `launcher/Cargo.lock` under their own licences.

## Client mod content
The `newgen-client` mod bundles Player Animation Library (MIT) and JCodec (BSD), and emote files by their credited authors (see `CREDITS.md` in the mod's resources).

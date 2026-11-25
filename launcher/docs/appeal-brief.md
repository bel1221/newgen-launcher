# Appeal brief: making NewGen feel premium (D1)

Creative Advisor, 2026-11-26, overnight autopilot.

**Inputs:**
- QA round 4's opinion and captures (`scratchpad\qa4\`: title, wheel, hub, options, launcher).
- The Design Manager's audit (`ingame-menu-audit.md`, v1–v3.2).
- The launcher comparison.
- The Lunar references (`images\2–4.png`): these were studied for *structure* only. Nothing is copied.

**Score:** S = 0.30·I + 0.25·F + 0.20·R − 0.15·B − 0.10·C (`company\autopilot-plan.md`).

**Owners:** DM = Design Manager (in-game, `newgen-client`); MD = Main Developer (launcher, Rust).

**Hard limits respected:**
- No new network services.
- Nothing that needs the user's content or money.
- Visuals stay local.
- No other client's art.

## Design rules every idea follows

These are taste-skill principles, translated to an in-game and launcher UI, and they match the Design Manager's v3.1 direction:

1. **One accent, used as a small mark.** White glass by default, #2B4BFF only for switches, focus and the "on" dot. Never a filled block over the footage.
2. **Every control answers the pointer.**
   - Hover: lift by 1 unit or brighten the glass.
   - Press: step down for 80 ms.
   - Nothing tilts or wobbles.
3. **One entrance per screen,** played once (fade plus 6–8 px rise, ≤ 300 ms). **No** fade-in on every row and no looping motion without purpose. "Reduce motion" (vanilla accessibility option) turns all of it off.
4. **Show the real thing:** the player's own skin, the real world footage, real emote icons. No labels explaining pictures.
5. **One system:** one glass panel, one button pair, one radius scale, one type scale. Any vanilla screen that breaks it is wrapped, not left raw.

## Evidence from the captures

- `qa4\base-title.png` (in a world):
  - The vanilla **"Move with W, A, S and D" tutorial toast** sits over everything.
  - The default HUD shows placeholders **"No combo" / "No hit yet"** and a speed readout before anything has happened.
  - Keystrokes, compass, coordinates and speed are all on at once.
  - This is the "unfinished" feeling QA describes.
- `qa4\emote-wheel.png`: the wheel works, but the icons are mixed:
  - three different scales and crops
  - one with a red ring
  - one generic person glyph
  - one square photo
  - empty slots look like broken icons
- QA: the vanilla Options screen, the server's chest-style language picker, and English mod names in VI.

## The ideas, scored

| # | Idea | Owner | I | F | R | B | C | **S** |
|---|---|---|---|---|---|---|---|---|
| 1 | **Clean first run**: suppress vanilla tutorial toasts (with a setting). HUD placeholders hidden until there's data ("No combo" appears only after a hit). A tidy default HUD preset: FPS, ping and coordinates on; keystrokes, speed and compass off by default | DM | 5 | 5 | 5 | 0 | 1 | **3.65** |
| 2 | **Launcher → game continuity**: the launcher writes the player's language into `options.txt` (`lang:vi_vn` / `en_us`, only if unset or launcher-owned) and its chosen palette into the client's config, so the game opens in the language and look the player picked | MD (+DM reads the key) | 4 | 5 | 5 | 1 | 1 | **3.20** |
| 3 | **"What's new" card** on the launcher home: a changelog bundled with each launcher build (no service), shown on the first start after an update, dismissable, 3 lines max plus "See all" | MD | 3 | 5 | 5 | 0 | 1 | **3.05** |
| 4 | **UI sound set**: one soft press click, one quieter hover tick (wheel slots only, not every button), one "open" whoosh for the hub and wheel. Self-made or CC0 OGGs, played through a NewGen "Interface sounds" volume (default 60 %), replacing the vanilla click on our screens only | DM | 4 | 4 | 5 | 0 | 2 | **3.00** |
| 5 | **Vietnamese mod names and descriptions** (74 mods) via the existing `vi_vn.json`. A native speaker checks the terms | DM (+ user reviews) | 4 | 4 | 5 | 0 | 3 | **2.90** |
| 6 | **Pause menu (Esc) in glass**: the most-seen vanilla screen, rebuilt with the title screen's panel and buttons. All vanilla actions kept | DM | 4 | 4 | 5 | 1 | 2 | **2.85** |
| 7 | **Handoff moment**: the launcher shows "Starting Minecraft…" with the weapon burst, then minimises when the game window appears. The game's first loading overlay uses the NewGen mark on dark with the progress bar, and keeps the Minecraft and Mojang credit line | MD + DM | 4 | 4 | 5 | 1 | 2 | **2.85** |
| 8 | **Emote wheel feel** (QA idea): uniform icon frames (same circle mask, same scale, fixed dark backdrop); empty slots as a dashed ring with "+". The hovered slot scales 1.0 → 1.12 in 120 ms. The centre shows the name, the length, and a speaker mark if it has sound. **Sound preview** only after a 400 ms hover, at 50 % volume, so sweeping the wheel isn't noisy | DM | 4 | 3 | 5 | 0 | 3 | **2.65** |
| 9 | **Live preview in the wheel centre**: your own player model doing the hovered emote (PAL on a GUI-rendered player) | DM | 4 | 2 | 5 | 0 | 4 | **2.30** |
| 10 | **Hub de-crowding**: one row of icon buttons with a label on hover or focus (the Lunar *structure*, our own icons in the glass style) instead of many text labels | DM | 3 | 4 | 5 | 0 | 2 | **2.70** |
| 11 | **Launcher polish pass**: a 150 ms window fade-in on open, the same hover and press steps on every button, focus rings for keyboard use, no layout jump while the player count loads | MD | 3 | 4 | 5 | 0 | 2 | **2.70** |
| 12 | **Joining transition**: replace "Connecting… / Loading terrain" with a glass card over the blurred title footage showing the server name and three-dot progress. Errors appear in the same card, with "Try again" and "Back" | DM | 3 | 4 | 5 | 1 | 2 | **2.55** |
| 13 | **Players-online line** on the title screen ("128 playing on NewGen"). It uses the normal server-list ping to newgenmc.net, not a new service. Hidden if the ping fails; never shows "0" or an error | DM | 3 | 4 | 5 | 1 | 2 | **2.55** |
| 14 | **Vanilla Options / Language / Controls in glass**: a wrapper that draws our blurred backdrop and panel behind the vanilla widgets and restyles their buttons. Not a rewrite, so every vanilla option still works | DM | 4 | 3 | 5 | 1 | 3 | **2.50** |
| 15 | **Skip the server's chest language picker** when the client already sent its language. Paper exposes the player's locale to plugins | Server owner (**not autopilot**: touches the server) | 4 | 3 | 5 | 2 | 2 | **2.45** |

Seasonal title clips (QA idea) are left out. They need new footage from the user, which a hard limit rules out. They're worth doing later with the user's own recordings.

## Top 5 recommended (for D3)

1. **Clean first run (3.65, DM).** The cheapest, biggest fix. Removing vanilla tutorial toasts and placeholder text is what turns "unfinished" into "finished".
2. **Launcher → game continuity (3.20, MD).** Players choose VI once in the launcher and never meet English again. Only write `lang` when the player hasn't changed it in-game.
3. **"What's new" card (3.05, MD).** It gives every update a moment, and the content ships with the build.
4. **UI sound set (3.00, DM).** Sound is half of "premium". Keep it quiet and few: three sounds, our screens only.
5. **Vietnamese mod names (2.90, DM).** This fixes the most visible language break QA found.

**Next if time allows:** #6 the glass pause menu and #7 the handoff moment (both 2.85), then #8 the wheel feel (2.65).

The Design Manager should treat #8 and #4 as one pass, because the wheel's hover tick and sound preview share the sound plumbing.

## Checks for QA after D3

- GUI scale 2, 3 and 4; EN and VI.
- With Reduce motion on, nothing animates.
- With interface sounds at 0, there's silence.
- A fresh `.minecraft`-style game folder opens in the launcher's language.
- No tutorial toast on a fresh profile.
- The wheel still works with 1–8 emotes.

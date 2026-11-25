# Newgen Client in-game menu audit (taste review)

Scope: the `newgen-client` mod's own GUIs: the title screen, the Right Shift hub (HUD layout), the Newgen Mods menu and the shared `Ui.java`. Server chest menus are out of scope.
Source: branch `origin/client-mod`. The changes are on the local branch `client-mod-revamp` (worktree `c:\Users\Administrator\server-launcher\newgen-client-revamp`, commit `1bd8a6d`).
**Not seen running:** the game files were cleaned up and a launch would download about 900 MB, so this audit is from the code. The look still needs to be checked at GUI scale 2 and 3. Units below are GUI units: 1 unit = 2 px at GUI scale 2.

## Findings, ranked by impact

| # | Screen | Problem (taste rule) | Change | Status |
|---|---|---|---|---|
| 1 | All | Labels in spaced, tracked capitals ("E N A B L E D", "O P T I O N S") at Inter SemiBold **8**. Hard to read, ugly with Vietnamese diacritics, and below the type floor. Rule broken: sentence case, never tracked capitals. | `Ui.spaced()` removed. The label face is now SemiBold **10**, in sentence case. | Done |
| 2 | Mods menu | Every card ends in a red "DISABLED" or green "ENABLED" bar, so half the grid shouts red for an ordinary state. Three click targets per card. Rule broken: colour means something, and off is not an error. | Card is 38 high: name (SemiBold 10) + switch on line 1, description (Regular 10, `DIM`) on line 2. The switch is 20×10: blue track and knob on the right when on, grey `0x40FFFFFF` when off. Clicking the card opens its options. Hover fades the outline to `0x50FFFFFF`. 3 columns at a 480-wide panel (was 4). | Done |
| 3 | All | Accent `#4A8EF0` while the launcher uses `#2B4BFF`. The client did not feel like the same product. Rule broken: one system. | `Ui.BLUE = 0xFF2B4BFF`, used for the main action, active tab, switches, slider and focus. | Done |
| 4 | All | Five different radii (3, 4, 5, 6, 12). Rule broken: one radius scale. | `R_SM 4` for controls, `R_MD 6` for cards, `R_LG 8` for panels. That is 8/12/16 px at scale 2, the same as the launcher. | Done |
| 5 | Title screen | "Play NewGen" was the same height as Singleplayer and Multiplayer, three stacked equal buttons. The bottom row was 16 high with tiny capitals. Rule broken: one main action that stands out. | Play NewGen is 220×**24**, SemiBold 11, blue. Singleplayer and Multiplayer share one row of 20-high glass buttons. Options, Mods and Quit are a 20-high row at the foot. Quit now uses the vanilla translated `menu.quit`. | Done |
| 6 | Title screen | No entrance and no motion at all. Rule broken: one hero entrance, quiet hover. | Once per game session, the logo rises 8 units and fades in over 700 ms. The buttons rise 12 units over 520 ms starting at 300 ms. All with quartic ease-out. Button hover now fades in over 150 ms and out over 120 ms (`Ui.Btn`). | Done |
| 7 | All | All text was hard-coded English, but the launcher ships in English and Vietnamese. | Every menu string is in `lang/en_us.json` and a new `lang/vi_vn.json`, read through `Ui.tr()`. Inter has full Vietnamese glyph coverage. | Done (menus) |
| 8 | Mods menu | Mod options: an ON/OFF pill with 8 px "ON"/"OFF" text, red when off. | The same switch as on the cards. | Done |
| 9 | Mods menu | Copy: "Search...", "No mods found", "Key or mouse". Rule broken: plain words, say what to do. | "Search mods", "No mods match your search", "Press a key or mouse button", "This mod has no options". | Done |
| 10 | Title screen | The vanilla panorama. The hero should be the server's own world, not a generic one. | Ship a NewGen panorama: six 1024×1024 cube faces taken at spawn, at `assets/minecraft/textures/gui/title/background/panorama_0..5.png` inside the mod. | Needs art |
| 11 | Title screen | No live fact on the first screen, such as players online or server status. | Add a status line under the logo: an 8-unit dot and "214 players online", using vanilla `ServerStatusPinger`, like the launcher. | Next round |
| 12 | Mods menu | 74 mod names and descriptions are still English only. | Move them to `newgen_client.mod.<id>.name` and `.desc` keys with Vietnamese text. | Next round |
| 13 | All | The font is Inter while the launcher is switching to Be Vietnam Pro. | Swap the TTF files in `font/*_N.json` (SemiBold + Regular) once the launcher's font is final. The same sizes work. | Next round |
| 14 | Hub | Fine overall: blurred game, logo, one button, outlined HUD frames. Its name tags used radius 3 and the capitals button. | Name tags use `R_SM`; the button reads "Mods"; the hints are translated. | Done |
| 15 | HUD mods | The look of the HUD elements in game (`HudMod` and its 30+ mods) was not reviewed visually. | Review it in game with a screenshot pass. | Not seen |

## Clean-up (junk removed)
- `Ui.spaced()`: spaced-capitals helper, no longer used.
- `Ui.roundBottom()` and the `top`/`bottom` flags in `Ui.slice()`: these only drew the old red/green card strip.
- `Ui.GREEN`, `Ui.RED`: no callers left.
- `ModsScreen.NAME_H`, `STRIP`, `KNOB`, `PILL_H`: constants of the removed card strips and the ON/OFF pill.
- The fabric.mod.json description said "HUD mods (CPS, FPS, Coordinates)", which was stale with 74 mods; it is now updated.
- No one-off scripts, notes or unused files were found in the mod. Every resource is referenced, `inter-license.txt` is required by the font licence, and no Java class is unreferenced.

## Build
JDK 21.0.13 (portable, in the scratchpad) + `gradlew build`: **BUILD SUCCESSFUL**. The jar is `c:\Users\Administrator\server-launcher\newgen-client-revamp\build\libs\newgen-client-1.0.0.jar`. Nothing was pushed, and `client-mod` and `main` are untouched.

---

## v2: Lunar-style (commit `7d39a29`, same branch and worktree)

User feedback: v1 was "still kinda bad". v2 copies the **structure** of Lunar Client 1.21.11's menus: a centred logo, stacked translucent buttons, a minimal hub, one panel with tabs, a left column, filter chips with search, and a card grid.
Lunar's logo, art, text, tracked capitals and red/green bars are **not** copied.
These rules from v1 still hold:
- accent `#2B4BFF`
- sentence case
- on = blue, off = neutral
- one radius scale (4/6/8)
- EN/VI strings

All sizes are GUI units (×2 px at GUI scale 2, ×4 at scale 4).

**Icons:** `textures/icons.png`, 13 white line icons drawn for this mod, at 64 px each: play, user, users, gear, grid, close, back, search, layout, globe, eye, mouse, sliders. They are tinted per use, and icon-only buttons have narration text.

**v1 bug QA found:** the close "×" rendered as ".." because the label was clipped in a 14-wide button. It is now an icon.

### Title screen
```
[user Bunn9]                                     [globe][ x ]   <- 20x20 corners, 6 from edges
                          (NewGen lockup, 160 wide)
                    +-----------[> Play NewGen]-----------+     <- 200x20, blue
                    +-----------[user Singleplayer]-------+     <- glass 0x99000000
                    +-----------[users Multiplayer]-------+
                    +--[gear Options]--+ +--[grid Mods]---+     <- 98x20 each, gap 4
Minecraft 1.21.11 (Modded)                         Copyright Mojang AB...
```
- Compared with Lunar, our stack has Play NewGen on top in blue in place of Lunar's ads. We have no icon dock and no quest or ad cards.
- Quit is the top-right X; language is the globe next to it.
- The logo and stack enter once per session (0.8 s, ease-out).
- Buttons fade their hover in 150 ms and out in 120 ms.

### Hub (Right Shift)
```
          (live game, unblurred, 0x30000000 shade; HUD elements framed and draggable)
                          (NewGen lockup, 150 wide)
                     [gear] [        Mods        ] [globe]          <- 20 + 110 + 20, gap 4
   Drag a HUD element to move it, scroll to resize, right click to reset   <- foot, SOFT
```
- Compared with Lunar: emote, MODS, cosmetics becomes options, Mods, language. The game is no longer blurred.

### Mods menu
```
+--------------------------------- panel 500x310, glass, radius 8 ---------------------------------+
| [icon] NewGen  [Mods] [gear Options]                                                        [x] |  header 28
|-------------------------------------------------------------------------------------------------|
| +- column 92 ---+  [All][On][Off]  [search  Search mods.....................................]   |  bar 14
| |grid  All   74 |  +-----------+ +-----------+ +-----------+ +-----------+                        |
| |layout HUD  30 |  |   icon    | |   icon    | |   icon    | |   icon    |   card 66 high:          |
| |eye Visual  20 |  |   Name    | |   Name    | |   Name    | |   Name    |   38 icon + name         |
| |mouse Ctrl   6 |  | Options * | | Options * | | Options * | | Options * |   14 Options strip       |
| |sliders Util 18|  |  Enabled  | | Disabled  | |  Enabled  | | Disabled  |   14 state strip         |
| |               |  +-----------+ +-----------+ +-----------+ +-----------+   (blue / 0x1AFFFFFF)    |
| | [layout Edit HUD] (blue, only in a world)                                         scrollbar 3 |
+-------------------------------------------------------------------------------------------------+
```
- The cards are at least 96 wide, which gives 3 columns at GUI scale 3 (1280 px) and 4 at a 500-wide panel.
- The category's icon stands in for Lunar's per-mod art.
- **Options view** (the card's top or its Options strip): the column stays. The content area shows [back] Name ... [switch], the description in DIM, the option rows (General / Colour sections, 2 columns when wide), and "Reset to defaults" at the foot.
- The **Options** header tab opens the game's own options.

### What QA should check (GUI scale 2, 3, 4; EN and VI)
1. **Title screen:**
   - Nothing overlaps at 1280×720 or 1920×1080.
   - Icons are sharp and centred beside their labels.
   - The globe opens Language and the X quits.
   - The name chip shows the account name.
2. **Hub:**
   - The game is visible, not blurred, and HUD elements can be dragged.
   - The hint is readable above the hotbar.
   - The three buttons work (Options, Mods, Language).
3. **Mods menu:**
   - Category counts are right, and the state chips filter.
   - Search works with Vietnamese input.
   - On a card, the top or Options strip opens options, and the last strip toggles it blue or neutral.
   - Edit HUD shows only in a world. The close icon shows (no "..").
   - Long Vietnamese labels fit ("Khôi phục mặc định", "Điều khiển").
4. **Options view:** back, the switch, sliders, key capture, colour typing and Reset all still work. These were carried over unchanged.

### v2.1: QA fixes (commit `b2a6013`, branch `client-mod-revamp`)
- **Mods panel spec, reconciled:**
  - 20 units of margin at every scale.
  - The panel top sits at least 36 units down, below the 32-high toast column, so the close button is never under a tutorial toast.
  - Columns: **3** at GUI scale 2 in 1280×720 (panel 500 wide) and **2** at scale 3 (Minecraft's maximum for 1280×720).
- **Header tabs:** sized to their label, plus 15 for the gear icon on Options. "Options" uses the game's own `menu.options` everywhere, so the title screen and the menu spell it the same.
- **Hub:**
  - The logo and buttons draw on a new GUI stratum above the HUD previews.
  - The cluster ends 20 units above the middle, clear of the crosshair and Attack Indicator.
  - The hint is on a glass strip 66 units above the bottom, above the hotbar, hearts and armour.
- **HUD default stack (left column), as fractions of free height:** FPS 0, CPS 0.09, Ping 0.18, Memory 0.27, Server Address 0.36, Coordinates 0.45. Saved positions are kept; Reset applies the new ones.
- **Keyboard:** Tab and Shift+Tab move a blue focus ring over the cards, Enter opens the card's options, Shift+Enter toggles it.
- **Remaining:** translated mod names and descriptions (74 mods) and the other HUD defaults.

### v3: video background (commit `c8445f2`)
- The title screen (and the hub and Mods menu outside a world) plays 4 NewGen clips: `assets/newgen_client/backgrounds/clip1..4.mp4`.
  - Encoding: 960×540, 24 fps, H.264 baseline, 14 s each, 4.2 MB total.
  - Playback: one after another, with a 1 s crossfade and a 0x38000000 shade over them.
- **Decoding:** JCodec 0.2.5 (BSD-2), nested in the jar. Measured 6.6 ms per frame on one low-priority daemon thread.
- **Stopping:** decoding stops 500 ms after no screen draws the background, so it never runs in a world.
- **Fallback:** an unreadable or undecodable clip is skipped; if none can play, the vanilla panorama is drawn.

### v3.1: colour (clear glass over the footage)
- **What the footage is like** (4 clips, mean RGB of a frame each: 78/54/33 warm sign, 26/29/30, 22/23/18, 43/43/38): dark night scenes with low saturation. Their only bright notes are small: teal glow, lanterns, greens.
- **Why the old theme failed:** saturated #2B4BFF blocks (the Play button, card strips, chips, Edit HUD) fought those scenes.
- **The new palette** (`Ui.java`, in one place):

| Token | Value | Used for |
|---|---|---|
| `GLASS` | black 32 % | panels |
| `DARK` | black 22 % | buttons and fields |
| `CARD` | white 5 % | cards |
| `LINE` | white 18 % | hairline borders |
| `HOVER` | white +8 % | the glass brightens on hover |
| `SELECTED` | white 14 % | tab, chip, row, "Enabled" strip |
| `PRIMARY` | white 85 %, `INK` #15171C type | the one main action (Play NewGen) |
| Type | `TEXT` #FFF, `SOFT` #E4E6EA, `DIM` #B0B5BD | with a 0.5-unit black 55 % shadow on light type |

- **Accent:** the launcher's #2B4BFF stays as the one accent (brand link to the launcher). It is used **only as a small mark**: switch on, slider, focus ring, the 2-unit active-row bar, the 4-unit "on" dot. It is never a filled block.
- **Mock-ups** over real frames, old (left) vs new (right): `scratchpad\theme\cmp-title-1.png`, `cmp-title-4.png`, `cmp-mods-1.png`, `cmp-mods-4.png`. Per frame: `title|mods-old|new-1..4.png`.

### v3.2: palette switch (Mods > Visual > Theme > Palette)
- **Default** is Glass: white is the accent, so there is no blue anywhere.
- **Five presets** live in `Ui.PALETTES`, one table. Each preset is accent / main-action pill / pill type / panel glass / button glass. The hues are picked from the clips.

| Preset | Accent | Main-action pill | Pill type | Panel glass | Button glass |
|---|---|---|---|---|---|
| Glass (default) | #FFFFFF | white 85 % | #15171C | black 32 % | black 22 % |
| Moss (the leaves) | #9CCB8F | 88 % | #0F1A0E | #0A120B at 32 % | #0A120B at 22 % |
| Lagoon (the teal glow) | #7CC4BF | 88 % | #0A1A19 | #081213 at 32 % | #081213 at 22 % |
| Lantern (lanterns, the sign) | #E6B872 | 88 % | #1E1407 | #130D07 at 32 % | #130D07 at 22 % |
| Sakura (the blossoms) | #E7A3B8 | 88 % | #22101A | #130A0E at 32 % | #130A0E at 22 % |

- **Applying it:** the palette is applied every client tick, so all menus change at once.
- **Names:** the preset names have EN/VI labels (`newgen_client.choice.*`).
- **Mock-ups:** `scratchpad\theme\preset-<name>.png` and `presets-all.png`.

### D2: taste audit of the QA4 captures (overnight)
**Fixed:**
- The hub labelled every HUD element at once, so the labels touched the globe button and the hint crossed Keystrokes and chat. Now only the hovered element shows its name, and the hint sits under the buttons.
- The emote wheel had no slot numbers, and its hint floated over bright scenery. Each slot now shows its number (1-8), and the hint sits on a glass strip.
- The tutorial toast covered our panels. It is now cleared while our screens are open.

**Noted, not built:**
- Every card in a category shows the same category glyph. The icon carries no information; per-mod glyphs or no glyph would be better.
- The HUD elements themselves (Keystrokes, Coordinates, combo/reach) are vanilla-style black boxes. A glass variant to match the menus would help.
- **Vanilla Options / Language screens:** keep the vanilla layout, but:
  - draw the dark menu band as our glass (black 32 % plus a hairline)
  - use Inter for the screen titles
  - give the list rows our 4-unit radius and hover brightening

  This needs a mixin on `AbstractSelectionList` / `Screen.renderMenuBackground`.

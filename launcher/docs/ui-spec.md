# Newgen Launcher UI spec v3 (Freya 0.5.4, OneLauncher look)

## Global
- Window 1280×720 logical, fixed; all sizes px. Font `DEFAULT_FONT` (Be Vietnam Pro, full Vietnamese coverage; Poppins lacked ơ ư ạ ế …, which fell back to another font); log uses `MONO_FONT` (JetBrains Mono).
- Colors from `ui::theme`; `a(c, n)` = `with_alpha(c, n)`. Accent `brand()` only on the one filled button, focus, the active log toggle and progress.
- **Edges:** one content line at x 48 and x 1232 for everything: navbar, hero, login card, news, log panel, version text.
- **Radius scale (3 steps):** 8 = inputs, Medium/Icon buttons, name pill; 12 = cards, panels, error box, log panel, Play and Game log buttons; 16 = login card. Nothing else.
- **Type floor:** nothing under 13px except the log body (mono 12). Labels sentence case, 13px Medium, never capitals.
- Layer order (back → front): background video → dim scrim → vignette → text guard → screen content → side panel (log or settings) → navbar → burst canvas. Layers 1–4 and 8 `interactive(false)`.
- **Language:** English / Vietnamese. Every launcher string lives in `src/i18n.rs` (one table, both texts side by side). Default: Vietnamese when the Windows display language is Vietnamese, else English. The choice is saved in `settings.json` and applies at once. API errors and website text are shown as they come. Numbers are formatted per language (21,179 / 21.179).

## Motion (house rules)
- Curve: `Function::Quart` + `Ease::Out` for every UI step and entrance; no bounce/elastic/back. Only offset, scale, opacity and colours animate; never width/height/padding.
- **Buttons (all variants), new code in `components.rs`:** hover = existing colour step + `offset_y` −1 (−2 on Play), 150 ms in / 120 ms out; press = `offset_y` +1 and scale .985, 90 ms. Disabled: none.
- **Linked news card:** hover lifts −2, border → `component_border_hover()`, image scales 1.03 inside its clip (600 ms in / 400 ms out). Unlinked cards do nothing.
- **Name pill** fades in 150 ms after 150 ms of hover, out 120 ms.
- **Screen entrance (once per screen mount; ~1.0 s):** one `AnimNum` 0→1100 ms linear; each part's progress = ease(clamp((t − start)/dur)). The panel a screen's words sit on (login card) and the navbar stand from the first paint.
  - Background frame: scale 1.05→1, start 0, 1100 ms (never hidden, never fades).
  - Lockup: y +16→0 + opacity 0→1, start 0, 700 ms.
  - Each following block (launch line, fact line, action row / form groups, button): y +12→0 + opacity, 520 ms, starts 350, 410, 470, 530.
  - Status, errors, news, log panel: no entrance.
- **The moment:** the weapon burst out of Play. Nothing else moves by itself except the video and the slow spin of the weapon on the Legendary Weapons card (user request).

## Shared layers (both screens)
1. **Background**: full-window rect `background(page())`; video frame via canvas, cover-fit (scale = max(1280/iw, 720/ih), centred, cropped). Before the first frame and whenever video fails: `assets/newgen/poster.jpg` (a still from the clips) drawn the same way — never a bare `page()` ground.
2. **Dim scrim**: absolute fill, `a(page(), 40)` (was 72: let the world through; the text guard protects the text).
3. **Vignette**: copy `VIGNETTE_SPOTLIGHT_SHADER` + `gradient_overlay_radial()` verbatim from `layout/app_shell.rs:532-590` (centre 0.80/0.45, maxDarkness 0.65; left stays darkest).
4. **Text guard**: absolute left 0, 720×720, LinearGradient left → right `a(page(), 170)` → transparent, `Layer::Relative(-15)`; behind the hero and the login card only.
5. **Version text**: absolute bottom 16, right 48; "v0.2.0", 13px Regular, `a(fg_secondary(), 160)`. Login only (on Home it would sit under the site cards).
6. **Burst overlay**: full-window canvas (`burst.rs`), absolute top0/left0, `Layer::OverlayLevel(20)`, `interactive(false)`, mounted only during the 1.9 s run; origin = Play button centre from its `on_sized` window-space area. Play switches to "Launching…" on the same frame the burst starts.

## Navbar (both screens) — `components/navbar/app_navbar.rs:47-79`
- Container: absolute top0/left0, 1280×80, `Layer::OverlayLevel(2)`. No intro animation.
  - Child 1 (first, so controls sit above it): drag strip `window_drag()`, absolute 1280×80.
  - Child 2: row, horizontal, `Content::Flex`, cross-centre, padding 0×48.
- **Left:** `icon.png` 32×32 only (width `flex(1)` group). The lockup names the server on both screens; no second name.
- **Right group** (horizontal, spacing 8, cross-centre), left→right:
  1. *Home only* — player name 14px Medium `on_media()`, margin-right 4.
  2. *Home only* — ghost Icon 36×36 [`LogOut` 18], name pill "Log out".
  3. *Home only* — divider 1×20 `a(fg_primary(), 40)`, margin 0×8.
  3b. Language toggle "EN | VI" (both screens, before Discord): segmented, padding 2, radius 8, bg `a(page_elevated(), 160)`, 1px `component_border()`; segments padding 4×8, radius 6, 13px Medium; active `component_bg_pressed()` + `fg_strong()`, other `fg_secondary()`.
  4. Discord, Website (each only if configured): ghost Icon 36×36, `Discord` / `Globe` 20, `on_media()`. Name pill: absolute top 42, centred, `Layer::OverlayLevel(3)`, padding 4×8, radius 8, bg `page_elevated()`, 1px `component_border()`, text 13px Medium `fg_primary()` ("Discord", "Website").
  5. Minimize: ghost 36×36, `Minus` 18. 6. Close: ghost 36×36, `X` 18. No maximize.
  - All icons `on_media()`; stock ghost hover/pressed.

## Login screen
- Content root: absolute top 80, left 0, 1280×640; vertical, main-align centre, padding left 48.
- **Card**: width 400, vertical, padding 32, spacing 20, radius 16, bg `a(page_elevated(), 215)`, 1px `component_border()`.
- Children, top→bottom:
  1. Header (vertical, spacing 8): `lockup.png` height 56 (≈227 wide); "Log in with your NewGen account" 14px Regular `fg_secondary()`.
  2. Account group (vertical, spacing 6): label "Account name" 13px Medium `fg_secondary()`; `text_input(account)` width fill, max 16 chars, prefilled with last name, `.on_submit` → submit.
  3. Password group, same layout, label "Password": `InputMode::new_password()`, `.on_submit` → submit. Offline dev mode: `.enabled(false)` + placeholder "Not needed".
  4. Button: primary, width fill, height 46, radius 12, 16px SemiBold, label "Log in"; pending: "Logging in…" + `.enabled(false)`. Enter in either field = same action (ignored while pending).
  5. Error row (only on error): horizontal, spacing 8, cross-start; `AlertTriangle` 14 `danger()` + API text verbatim 13px Regular `danger()`, width fill, `max_lines(3)` ellipsis.

## Home / Play screen
- Content root: absolute top 80, 1280×640; children absolutely positioned so nothing reflows.

### Hero (absolute left 48, top 56 → window y 136; width 620; vertical; cross-start)
1. Lockup: `lockup.png` height 88 (≈357 wide). It is the title.
2. Launch line: `config.tagline` per language (`{ "vi": …, "en": … }`; a plain string serves both). Built-in default: vi "Server Battle Royale thế hệ mới", en "The next-generation Battle Royale server". 20px SemiBold `on_media()`, `max_lines(1)`, margin-top 14.
3. **Status line** (row, spacing 8, cross-centre, margin-top 8, or 16 when there is no launch line): an 8px dot (`success()` with a count, `fg_secondary()` without); "N players online" 15px SemiBold `on_media()`; "· Minecraft 1.21.11 · newgenmc.net" 15px Medium `a(on_media(), 190)`. The count is `online` from `config.statusUrl` (default `https://www.newgenmc.net/api/stats`, the same JSON the website's stats bar reads), refreshed every 30 s. On failure the line shows without the count, never an error. The dot does not pulse.
4. Action row (horizontal, spacing 12, cross-centre, margin-top 28). All buttons height 52, radius 12:
   - **Play**: primary, content padding 0×36, min width 200, 18px SemiBold, [filled `Play` 18 `on_brand()`, 10 gap, label]. Labels: "Play" / "Launching…" / "Playing" (`enabled(false)`). While a step reports progress, the button itself fills left → right (`brand_pressed()` over `brand_disabled()`, with a hard edge) and reads "Updating NN%". Click → burst.
   - **Settings**: secondary 52×52, `Settings` icon 20; while the panel is open, the icon is `brand()`.
   - **Game log** (only once the game wrote output): secondary, padding 0×20, 15px Medium, [`Terminal` 16, 8 gap, "Game log"]; while open, foreground `brand()`.
5. Status text (only for steps without progress): margin-top 20, width 420, 13px Medium `on_media()`, `max_lines(1)` ellipsis.
6. Launch error box (only on error): margin-top 16, width 560, horizontal, spacing 10, padding 12×14, radius 12, bg `a(danger(), 36)`, 1px `a(danger(), 110)`; `AlertCircle` 16 `danger()` + text 13px Regular `fg_primary()`, `max_lines(4)` ellipsis (full text also in the game log).
- Worst-case hero bottom ≈ y 510, clear of the site cards (top y 524).

### Site cards (`ui/cards.rs`, replaces the news row)
- Absolute left 48, right 48, bottom 48, height 148; horizontal, spacing 16, each card `flex(1)` (4 cards ≈ 284 px).
- One card per section of newgenmc.net. They are bundled in `assets/newgen/site/sections.json` plus images, because the site renders its text from a JS dictionary and there is nothing stable to fetch. The cards are Legendary Weapons (→ /battle-royale#recipes), Battle Royale (→ /battle-royale), Duels (→ /duels) and Spiritbeast Realm, which is coming soon (→ /#cluster-spirit). Titles and text use the site's own vi/en wording. Clicking opens the section (https only).
- Card: radius 12, bg `a(page_elevated(), 190)`, 1px `component_border()` (`component_border_hover()` on hover), `overflow(Clip)`, lifts 2 px on hover. The picture is cover-fit and scales to 1.03 on hover (600 ms in / 400 ms out). The text strip sits on a fade to `a(page(), 225)` at `Layer::Relative(1)` (above the picture), padding 0 14 12: title 15px SemiBold `on_media()` + `ArrowRight` 16, then one line 13px Regular `a(on_media(), 200)`, ellipsis.
- **Legendary Weapons card**: no picture. Instead, a live 3D weapon from `weapons.bbmodel` (drawn by `burst.rs`), 96 px, centred above the strip.
  - Idle: yaw 0.6 rad/s, pitch 0.35, tilt −0.55, bob ±3 px.
  - Hover: scale 1.08, spin 1.8 rad/s (~200 ms ease).
  - Drag rotates yaw and pitch freely; on release it eases back into the spin.
  - Click without dragging shows the next weapon (all 49, cycling); the strip's second line names it. Clicking the strip opens the section.
  - Redraws at ~30 fps (this card only) and pauses while minimized. Hover is worked out from the pointer position, because the card changes every frame.

### Game log panel (only while toggled open) — `components/log_viewer.rs`
- Absolute right 48, top 8 (window y 88), width 520; bottom edge at y 508 (16 px above the site cards). Only one of it and the settings panel is open at a time. `Layer::Relative(10)`; bg `a(page_elevated(), 240)`, radius 12, 1px `component_border()`, `overflow(Clip)`.
- Toolbar: horizontal, padding 10 14 10 20; "Game log" 16px SemiBold `fg_primary()` `flex(1)`; ghost Icon close [`X` 16].
- 1px divider `component_border()`.
- Body: ScrollArea, padding 10 top / 20 bottom, rows padded 10 horizontal; `MONO_FONT` 12px, line height 16, `fg_secondary()`; lines with "ERROR" or "Exception" → `danger()`. Sticks to bottom (pauses when scrolled up, resumes at bottom). Cap 2,000 lines.

### Settings panel (`ui/settings_panel.rs`)
- Same frame as the game log panel, width 480, title "Settings". Body padding 20, spacing 24:
  - Memory (RAM): the value "N GB" 14px SemiBold at right, and a Freya `Slider` for 2–12 GB, step 1, with a `brand()` thumb on `component_bg_pressed()`. Defaults to `config.memory.max`. Saved as `memoryMaxGb` and used for -Xmx; the minimum stays from config.
  - Keep launcher open: a Freya `Switch` (`brand()` when on) with a 13px `fg_secondary()` hint. Off = the window minimizes when the game starts. Default on.
  - Language: the same EN | VI toggle.
  - Open game folder / Open logs: secondary buttons, height 40, radius 8, `Folder` / `Terminal` icon 16.

## States summary
- Empty parts are not rendered (not zero-height). Errors: `danger()` only in the two places above; mods warning = native OS message box.
- Login → home: content root re-mounts, the home entrance plays once; navbar stays, gaining the player block.

## Taste review changes (v2)
1. Removed the five "Coming soon" news cards; news row only with ≥ 3 real items, as image + title + date cards — placeholders are banned, and empty cards read as a template.
2. Bottom fade and version text follow the news row — no layer without a job.
3. Scrim 110 → 72 and a poster still for no-video — the hero is the work (the server's world), not a dark mood; every visual renders.
4. Navbar padding 40 → 48, version right 18 → 48 — equal left/right edges.
5. Navbar: icon only, server name text removed — the lockup already names it; one name per screen.
6. Removed navbar slide-in and login card slide; one staggered entrance per screen (~1 s, ease-out quart, background settles 1.05→1) — one hero entrance, panels stand from the first paint.
7. Buttons step 1px on hover and press 1px/.985 on click (new code) — house rule 5, quiet micro-interactions.
8. Unlinked cards no longer react to hover — motion answers only things you can click.
9. Added launch line (`config.tagline`) under a larger lockup (72 → 88) — gaming hero: name as title plus a launch line.
10. "ACCOUNT"/"PASSWORD" → "Account name"/"Password" 13px Medium; "Sign in to play" → "Log in with your NewGen account" — sentence case, no tracked capitals, one wording for one action.
11. All text ≥ 13px (error, version, pill; "NEWS" label gone) — type floor.
12. Radius scale fixed at 8/12/16 (pill 6 → 8, error box 10 → 12, Play/Log in 12) — one radius scale.
13. Play and Game log share height 46 and radius; Log out became an icon button with a name pill like Discord/Website — one button pair, one chrome style.
14. Play shows "Launching…" the frame the burst starts — the burst is the one dealt moment, never a wait.

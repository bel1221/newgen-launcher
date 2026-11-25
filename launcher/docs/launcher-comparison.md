# Launcher comparison: OneClient, Lunar Client, Newgen

Captured 2026-11-26 with `PrintWindow(…, 2)`, with no input sent to any app. All three windows were restored and rendered (none black). **Observed** = in the capture. **Known** = from OneLauncher's source or product knowledge.

## 1. OneClient (1200×800)
**Observed (Home):** The OneClient wordmark sits top left. The nav is a row of centred word pills (Home, Versions, Browse, Wardrobe). Friends, notifications, avatar and the window controls sit on the right. Full-bleed key art fills the window. The title "26.3 Fabric" is in Poppins Bold at about 48px, with a sub-line "OneClient · Wilderness Bound", a blue Launch button and a settings icon. Below that is a row of version cards, each with its own art and a title over it. The active card has a 1px blue border, and a final "more" tile shows a dot grid.

**Capture caveat:** The hero is shifted about 70px left (the title and button are clipped), and the Browse page's sidebar shows underneath. This looks like a page transition caught mid-way. I am not judging that.

**Good:**
- The art is the hero, kept bright with only a left-side darkening, so the world reads.
- One filled action, with settings right beside it.
- The cards are pictures, not text boxes.
- Nav words are calm.
- One accent colour.

**Weak:**
- The title is a version number. That names a build, not the experience.
- Card titles sit straight on busy art with no calm area under them.
- The wordmark uses tracked capitals.
- The "more" tile is a generic grid glyph.

**Known (source):** A settings screen with a memory field and a Java install manager, a crash dialog, a launcher updater, toast notifications, an account switcher, a skin avatar and 3D player model, a screenshot viewer, optional-mods prompts and an onboarding flow.

## 2. Lunar Client (1416×808)
**Observed (Home / Launchpad):**
- A left icon rail with 8 icons and no labels.
- A top bar with coins (198), downloads, music, search, help, notifications (32) and a profile.
- A greeting, "Up Late Astronaut, Bunn9", and a live "59,400 Online" count.
- A hero box on a dark space ground with a large gradient green "LAUNCH GAME" button. Its second line names what launches ("Hoplite for Lunar Client"), with a dropdown and a settings gear.
- "Latest News (12 unread)" as 2-column art tiles with green unread dots.
- A featured-servers icon row.
- A Friends panel (0 online) above a large empty area with a watermark.

**Good:**
- The live online count is a hard fact on the first screen.
- The launch button says what it will launch.
- The news has unread state and real art.
- Settings is one click from launch.

**Weak:**
- Shouting all-caps on a gradient button.
- Four or more accent colours (green, gold, red, purple).
- News tiles are ads with text baked into the image.
- About 20 controls on one screen.
- Unlabelled rail icons.
- Mood copy in the greeting.
- A dead empty panel with a watermark.
- The game's world is missing: the hero is a dark space ground, not Minecraft.

## 3. Newgen Launcher (1280×720)
**Observed (Home):**
- `icon.png` top left.
- Top right: "Bunn9", a log-out icon, a divider, Discord, Globe, minimise and close.
- The lockup at about 88px, the fact line "Minecraft 1.21.11 · Fabric · newgenmc.net" and a blue Play button (233×46, thin outline play glyph).
- Server gameplay video behind everything, and "v0.2.0" at bottom right.
- Nothing else. The launch line, news row and log toggle are all hidden because there is no data.

**Observed (Login, from `screenshot-login.png`):** A glass card at x 48 holding the lockup, "Log in with your NewGen account", the Account name and Password fields, and a Log in button.

**Good:**
- The server's own world is the key art.
- The brand is unmistakable.
- One action, with equal 48px edges holding everywhere.
- A calm, restrained nav with one accent colour.
- The cleanest of the three.

**Weak:**
- The screen below y 340 is about 55% empty, with no news, status or anything alive to show.
- The video is too dark: the scrim plus vignette turn the world to mud, while OneClient's art stays bright.
- The fact line is small grey text on dark video.
- The Play glyph is a thin outline that reads weaker than its label.
- There is no settings entry (RAM, folder), unlike both rivals.
- Log out is a bare one-click icon next to the name, with no avatar.

## 4. Feature map for Newgen
| Feature (source) | Tag | Reason |
|---|---|---|
| Live players online + server up/down (Lunar, observed) | **Must** | It is the one hard fact that makes the launcher feel alive, and one Server List Ping covers it. |
| News feed with art, date and unread dot (Lunar observed, Polyfrost site known) | **Must** | It fills the empty half with the server's own updates. It needs a content source. |
| Settings: RAM, game folder, close/keep launcher on launch, open logs (OneClient known, Lunar gear observed) | **Must** | Players on 8 GB machines need the RAM control, and both rivals have it one click from launch. |
| Launcher self-update (OneClient `updater.rs` known) | **Must** | We ship one fixed pack, so an outdated launcher means a broken join. |
| Crash dialog: exit code, last 30 log lines, Copy log, Open folder (OneClient known) | **Should** | It turns "it closed" into a report the staff can act on. |
| Skin head beside the name (both, observed) | **Should** | It is identity at a glance and makes Log out less bare. |
| Store link in the navbar (Lunar cart, observed) | **Should** | Only if the server sells ranks (VIP is seen in-game); a link costs nothing. |
| Unread news count | **Should** | Cheap once the feed exists. |
| Friends panel, featured servers, account switcher, greeting | **Skip** | One server, one account, with social life happening in-game. The greeting is mood copy. |
| Versions/instances, mod browser, wardrobe, coins, music player | **Skip** | The modpack and version are fixed, and cosmetics live in-game. |

## 5. Make it appealing (ranked by impact)
1. **Let the world through.** Lower the scrim from `a(page(),72)` to `a(page(),40)` and the vignette maxDarkness from 0.8 to 0.65. Add a left text guard: a LinearGradient from x 0→720, `a(page(),170)`→transparent, behind the hero only. *Params only.*
2. **Live status line**, replacing the grey fact line. It is a row with spacing 8:
   - an 8px dot, `success()` when online and `danger()` when offline
   - "214 players online" in 15px SemiBold `on_media()`
   - "· Minecraft 1.21.11 · newgenmc.net" in 15px Medium `a(on_media(),190)`
   - The ping runs every 30 s. When offline the text reads "Server offline".
   - *New code: SLP ping.*
3. **News row as built in spec v2**, with three art cards at the bottom (x 48→1232, 148 tall), an 8px `brand()` dot on unread items, and the title "News" in 16px SemiBold above the row. *New code plus content.*
4. **Play becomes the hero object.**
   - Size: height 52, padding 0×36, radius 12.
   - Icon: a **filled** play glyph at 18px (make it a filled path in `play.svg`).
   - Progress: while updating, the button background becomes the progress track (`brand_pressed()` fill growing over `brand_disabled()`), with the label "Updating 42%". This replaces the separate status block.
   - *New code.*
5. **Settings button** beside Play: secondary, 52×52, radius 12, `settings-01.svg` (copy it from the OneLauncher icons). It opens a 480px right panel styled like the log panel, holding:
   - RAM slider (2–12 GB, step 1)
   - Keep launcher open toggle
   - Open game folder
   - Open logs
   - *New code.*
6. **Player chip:** a 24×24 skin head (radius 6) + name in 14px Medium, as one ghost button. Click opens a small menu ("Log out"). This replaces the bare log-out icon. *New code plus a skin source.*
7. **Launch line**: 20px SemiBold `on_media()` under the lockup, from `config.tagline`. *Needs your copy.*
8. **Login:** add a text link "Create an account" (13px Medium `brand()`) under Log in, opening the site's sign-up page. Apply the same scrim change as item 1. *Needs a URL.*

Motion is unchanged: one entrance per screen, the button step and press, and the weapon burst as the single moment. The status dot does not pulse.

## 6. Captures still wanted
- OneClient's Settings screen and crash dialog.
- Lunar's Settings screen.
- Newgen's game log panel open while the game runs.

Please open these yourself; I won't click in the other apps.

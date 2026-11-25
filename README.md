# Newgen Launcher (skeleton)

An Electron launcher for our offline-mode server. It already launches Fabric Minecraft, auto-joins the server and syncs mods. The account system isn't connected yet: the login, the client mod and the server plugin still need to be built (see **What's left**).

## Run it

Requires Node.js 18+ and Java (17+ for MC 1.20.x) on PATH.

```
npm install
npm start
```

If `npm start` complains that Electron failed to install: newer npm versions skip Electron's download step (it warns about `allowScripts`). Run `node node_modules/electron/install.js`. On Node 26 that step can quit without unpacking; then unzip `%LOCALAPPDATA%\electron\Cache\...\electron-v33.*-win32-x64.zip` into `node_modules\electron\dist` and create `node_modules\electron\path.txt` containing `electron.exe`.

With `apiBaseUrl` empty in `config.json`, the launcher runs in **offline dev mode**: any username works and no password is asked (the password box is switched off and says NOT NEEDED).

Build a Windows installer: `npm run dist` (output in `dist/`).

## How it works

```
Login screen ──► POST {apiBaseUrl}/launcher/login         (username + password → sessionToken)
PLAY ──► sync files from manifestUrl                       (mods, resource packs, configs)
     ──► install bundled mods, refuse added mods            (see Mods)
     ──► POST {apiBaseUrl}/launcher/join-token             (sessionToken → one-time joinToken)
     ──► write <game dir>/config/launcher-auth.json        ({ username, token, createdAt })
     ──► start Fabric MC offline, auto-join serverHost:serverPort
In game: client mod reads + deletes the token file, sends the token to the server
         on a plugin channel → server plugin verifies it with the API → AuthMe forceLogin
```

Game folder: `%APPDATA%\.<folderName>`.

## Files

| File | Purpose |
|---|---|
| `config.json` | Server address, MC/Fabric versions, API and manifest URLs, RAM, Discord and website links (see **Links**) |
| `src/main.js` | Electron main process; wires login → sync → launch |
| `src/auth.js` | Calls the backend API (offline mode if `apiBaseUrl` is empty) |
| `src/sync.js` | Downloads/verifies files from the manifest; deletes unlisted files in managed folders |
| `mods/` | The client's mods, shipped with the launcher (see **Mods**) |
| `src/mods.js` | Installs the bundled mods into the game folder and refuses added mods |
| `src/launch.js` | Installs Fabric, offline auth (same UUID as an offline server), auto-join, token hand-off |
| `src/preload.js` | Restricted bridge between the UI and main process |
| `src/renderer/` | Login and Play UI, logo, window buttons, link buttons and footer (see **Branding**, **Window** and **Links**) |
| `src/renderer/burst.js` | Weapon launch animation (three.js) |
| `src/renderer/assets/` | Logo, icon, fonts and weapon models (see **Branding**) |
| `src/renderer/assets/videos/` | Background videos, played in file-name order (see **Background video**) |
| `docs/gui-variants/` | Other versions of the look, as CSS patches, and the earlier login screen (see **Other versions of the look**) |
| `manifest.example.json` | Example file-sync manifest |

## What's left (TODO for the main dev)

### 1. Backend API (any language)
It needs to read the AuthMe accounts, so AuthMe should use MySQL (`DataSource.backend: MYSQL`).

- `POST /launcher/login`: body `{ "username", "password" }`. Check the password against the AuthMe table using AuthMe's configured hash. Returns `{ "username", "sessionToken" }`, or HTTP 401 with `{ "error": "Wrong username or password" }`. Rate-limit this endpoint.
- `POST /launcher/join-token`: body `{ "sessionToken" }`. Returns `{ "joinToken" }`: random, single-use, tied to the username, expires in about 60 seconds.
- `POST /launcher/verify` (called by the server plugin only, protected by a shared secret): body `{ "username", "joinToken" }`. Returns OK and deletes the token if it is valid.
- Serve over HTTPS. The `error` field is shown to the player as-is.

### 2. Client Fabric mod
- On join, read `config/launcher-auth.json`, **delete it**, and send `token` on a custom plugin channel (e.g. `ourserver:auth`).
- Add the mod to the manifest so the launcher installs it.

### 3. Server plugin (Paper/Spigot)
- Listen on the same channel, call `/launcher/verify`, and on success run `AuthMeApi.getInstance().forceLogin(player)`.
- Ignore tokens for players who are already logged in. Normal `/login` stays as the fallback.

### 4. Launcher improvements
- **Java runtime:** bundle or download Java (currently uses `java` on PATH or `config.javaPath`).
- **Auto-update:** use `electron-updater` for the launcher itself.
- **Remember login:** store `sessionToken` securely (Electron `safeStorage`) so players don't log in every time. Never store the password.
- **Settings screen:** RAM and similar options.

## Branding

Name: **Newgen Launcher** (window title comes from `serverName` in `config.json`; installer name from `build.productName` in `package.json`).

Files in `src/renderer/assets/` (copied from the original artwork, which is not in this repo):

| File | Use |
|---|---|
| `lockup.png` | Logo in the top left corner of the window, on both screens (`newgen_lockup_than_ngan.png`) |
| `icon.png` | Window and installer icon: `newgen_icon_than_ngan_master.png` padded to 512x512 |
| `fonts/` | Silkscreen (uppercase pixel font: labels, buttons, titles) and Pixelify Sans (mixed-case pixel font: typed names, body text, log). Both SIL Open Font License, see `Silkscreen-OFL.txt` and `PixelifySans-OFL.txt`; the Play screen uses these two. `Minecraft.ttf` (font name "Minecraft" by CraftronGaming) is used for all text on the login bar, at 16px (its 8px grid). **Its licence is unknown** (no licence file came with it): check it before distributing the launcher publicly |
| `weapons.bbmodel` | Legendary weapon models for the launch animation (`newgenmc_vu_khi_huyen_thoai.bbmodel`) |

The look is the server's in-game GUIs drawn as **see-through frames over the background video**: the video shows through everything except the things you can click. It is built in CSS from flat colours and hard pixel edges (no images cut from screenshots, no rounded corners, no blur; the only rounded thing is the window itself, see **Window**). Every palette colour was sampled from the in-game screenshots and is a CSS variable at the top of `src/renderer/style.css`.

One shared rule draws the pixel construction for every piece (`style.css`, "Shared pixel construction"): a one-pixel outline with the corner pixel missing (stepped corner) and, just inside it, a top/left and a bottom/right line. Each piece only sets the colours:

- A **button** is the only solid piece: a fill, a light top/left edge and a shaded bottom/right edge.
- A **frame** is a button without the fill: the dark outline and one light line in the screen's colour. The dark line shows on a bright picture, the light line on a dark one.
- A **box inside a frame** (typing field, game log, progress bar, error message) has one line only.

The two screens:

- **Login screen = one bar near the bottom of the window**, in the colours of the in-game LOG IN book, so the video has the middle of the window to itself. The bar is one frame (outline in the blue of the cover `#173263`, line in the orange of the page frame `#f6b041`). In it, from left to right and parted by lines in the same orange: the ACCOUNT box, the PASSWORD box and the solid blue LOG IN button (`#376194`, edges `#487dbd` / `#1f4575`, outline `#0f2a4c`). Each box is a typing field drawn the way Minecraft draws its own (dark see-through box, dusty-pink line `#c2a4a9`, white text) under its caption, so a box still says what it is after something is typed in it. A name of 16 characters fits without scrolling.
- **The password box follows `apiBaseUrl`.** Both boxes are always shown. With an account API configured the password box is an ordinary password field, and a login without a password is refused as before ("Enter your password"). In offline mode a password does nothing, so the box is switched off: it cannot be clicked or typed in, the Tab key skips it, and it has no dark inside, a faint line and the words NOT NEEDED. `init()` in `renderer.js` switches it off; `input:disabled` in `style.css` is its look.
- **The logo** sits in the top left corner of the window on both screens (in `#titlebar`, 36px high). Neither screen has a logo of its own, so it stays where it is when one screen follows the other.
- **Play screen = the tan panel family (cosmetics, battle pass, VIP rank).** One frame (outline `#5c342e`, line in the panel tan `#f0b978`) with the LAUNCHER title in a small red-lined frame (`#ce453e`) on its top edge. Inside: the player name next to LOG OUT, and the big PLAY button. The buttons are the in-game tan ones (`#f0b978`, edges `#fcd58b` / `#9d5c35`, lettering `#5c342e`; disabled `#c9835f`).
- **On the Play screen only while there is something to show:** the status line; the battle-pass progress bar (fill `#56dbee` / `#2ab6e5` on `#041819`), only while a task reports progress; the error message after a failed launch; and the GAME LOG button, once the game has written something (the log opens under it).
- **Error messages** (both screens): white lettering on see-through red (`#9c182f` at 60%) with a red line (`#ce453e`). A login error stands on the top edge of the bar, as wide as the bar, the two sharing one outline; a long message takes more lines and grows upwards, so the bar never moves. A launch error sits inside the Play frame.
- **Lettering on the video** is white (`#fcfcfc`) with a hard one-pixel dark rim (`--rim`).

One GUI pixel is `--px` (2px everywhere); label sizes are multiples of it so the pixel font stays crisp. The Play frame hangs from a fixed line (`padding-top` on `body`), so it only grows downwards: PLAY does not move when the status line, an error or the log appears. The login bar is placed from the bottom instead (`margin-bottom` on `.bar`): its outline ends 18px above the link buttons.

The login screen until 2026-11-25, the two-page LOG IN book in the middle of the window, is kept in `docs/gui-variants/book-frame-login.md` (see **Other versions of the look**).

### Readability

White lettering straight on a video is only safe if the picture behind it is dark enough. The rule used here: **the picture behind any lettering is darkened by 56% or more.** That keeps white lettering at 4.5:1 or better even on a pure white picture, so a brighter clip added later cannot break it.

- Inside the frames the 56% is the video dim (40%, `#bg::after`) plus `--glass` (27%): `1 - 0.60 x 0.73 = 0.56`. If you change one, change the other so that the result stays at 56% or more.
- The typing fields and the game log are darker still (`--well`), and error messages have their own red backing.
- The login bar sits near the bottom edge, where the vignette adds to the dim: inside the bar the picture is darkened by 56% (top middle, behind the PASSWORD caption) up to 67% (bottom corners). The words NOT NEEDED in the switched-off password box are lettering on the frame, like the captions: white with the rim, not greyed out.
- Two pieces of lettering stand outside a frame. The version line sits in the corner, where the vignette alone is above 56%. The login error stands on the bar, where the dim is 40% or a little more; its red backing does the rest.
- The rim puts a dark edge round every letter, whatever is behind it (15:1 between letter and rim).

Measured in a hidden window, the same way as the earlier experiment (`docs/gui-variants/FINDINGS.md`, "Method"): the lettering is hidden for the capture and each text's colour is compared with the pixels really behind it (WCAG contrast ratio; the figure is the 5th-percentile pixel of the worst frame). Measured again on 2026-11-25, when the login became the bar: all seven clips from start to end, one frame every 0.25 seconds (390 frames) for each of five screen states, plus a pure white and a pure black picture in place of the video:

| Lettering | Needs | Worst frame of the clips | White picture | Black picture |
|---|---|---|---|---|
| In the login bar: ACCOUNT, PASSWORD, NOT NEEDED | 4.5 | 6.04 | 4.83 | 20.5 |
| In the Play frame: LAUNCHER, player name, status | 4.5 | 5.13 | 4.83 | 20.5 |
| Login error (on the bar, outside the frame) | 4.5 | 5.96 | 5.78 | 13.2 |
| Launch error (in the Play frame) | 4.5 | 7.54 | 7.36 | 13.2 |
| Version line | 4.5 | 7.74 | 6.52 | 20.5 |
| Typed text, game log | 4.5 | 11.1 | 10.6 | 20.5 |
| On the solid buttons: LOG IN / tan buttons / window buttons | 3 | 6.20 / 6.00 / 6.28 | same | same |

The lowest single pixel behind any lettering, in all of it, is 4.83. Over the darkest clip (`bg-02`) and the brightest (`bg-06`) alone, the lettering of the bar stands at: captions 9.09 and 6.37, NOT NEEDED 8.72 and 6.72, login error 7.88 and 5.96, typed text 14.3 and 11.5.

The brightest moments of today's clips come close to the white-picture figure (5.13 behind the player name, against 4.83), so there is no room to dim less: with `--glass` taken out (only the 40% dim) the lettering in the frames drops to 2.78 on a white picture and to about 3.0 on the brightest moments of the clips.

Frames, measured against the video next to them (2026-11-24, the Play frame and the earlier login book): on the darkest clip the light line stands out at 6.7:1 or more; on the brightest moments the weaker spots are about 2.7:1 (lowest single spot 2.45:1) for the better of the two lines, typically 5:1 to 11:1. The two lines against each other are 6.0:1 (Play) and 6.7:1 (login). The login bar has the book's two line colours; measured on 2026-11-25 against the video 5px outside it, the better of its two lines stands at 2.7:1 in the worst frame of the clips (lowest single spot 2.59:1), 4.8:1 in a typical frame, 2.76:1 on a pure white picture and 11.2:1 on a black one.

### Other versions of the look

`docs/gui-variants/` holds other versions as CSS patches. To try one, paste its rules after the last rule of `src/renderer/style.css` (and take them out again to go back):

- `transparent-alt-1.css`, "clear line": nothing darkens the inside of the frames (the whole video is dimmed 56% instead), each frame is one thin light line, and the titles are plain lettering.
- `transparent-alt-2.css`, "in-game bevel": frames with the in-game light and shaded edges, solid title plaques across the top edge, solid dusty-pink typing fields, framed cards for the player name and the status.

`book-frame-login.md` is not a look but the earlier login screen: the two-page LOG IN book that stood in the middle of the window until 2026-11-25, when the login bar replaced it. It holds the book's markup, CSS rules and script lines as they were, and says how to put them back.

`solid-look-backup/` is the earlier, fully solid look (the three files of `src/renderer/` as they were): copy them back over `index.html`, `style.css` and `renderer.js` to return to it. `FINDINGS.md`, `recommended.css` and `runnerup-*.css` are an earlier experiment with a 75% solid version of that look; those three patches fit the backup's `style.css`, not the current one.

### Background video

The GUI is see-through (see **Branding**): only the buttons are solid, and `html` and `body` are transparent. Behind everything sits one full-window layer, `<div id="bg">`, the first element in `src/renderer/index.html`. It plays the videos in **`src/renderer/assets/videos/`**.

- **What it does:** the videos play one after another in the order of their file names, each from its start to its real end at normal speed, with no sound. After the last one the first starts again, and so on forever. The launcher always starts with the first one. Nothing is random.
- **The number in the file name is the play order.** The files are named `bg-01.mp4` to `bg-07.mp4` and play in that order. Numbers are compared as numbers, so `bg-2` would play before `bg-10`. **To change the order, rename the files** and restart the launcher: the clip with the lowest number plays first. The order is sorted in code (`listVideos()` in `src/main.js`); it does not depend on the order Windows lists the folder in.
- **Add or remove a video:** copy an `.mp4` or `.webm` file into `src/renderer/assets/videos/`, or delete one from it, and restart the launcher. No code change is needed. A new file plays at the place its name sorts to (short names without spaces are tidiest, e.g. `bg-08.mp4`).
- **One clip dissolves into the next.** `FADE` seconds before a clip ends, the next clip starts playing behind it and the ending clip fades out over the time it has left. For that moment both pictures are moving, and no clip is cut short. The last clip dissolves into the first the same way. `FADE` is one constant above `startBackground()` in `src/renderer/renderer.js` (now `1.5`; `0` gives a plain cut). The curve of the fade (`ease-in-out`: soft start, soft end) is on `#bg video.out` in `style.css`.
- **A file that cannot be loaded or played** is skipped until the next start and the order goes on with the next one. With no playable video, or no `videos` folder, the plain dark colour stays (`--bg` in `style.css`). A single video dissolves into itself.

How it is wired: `listVideos()` in `src/main.js` lists the folder, sorts the names and sends them to the page with `get-info`; `startBackground()` in `src/renderer/renderer.js` plays them in two stacked `<video>` elements that take turns. The next clip is loaded early and waits, paused, behind the clip on screen.

- The start of a dissolve is read from the ending clip's own clock (the time it has left), not from a timer. A clip that stalls or is paused delays the dissolve by the same amount.
- The dissolve starts only once the next clip is really playing. If that is late, the fade is shorter: it always ends when the ending clip ends.
- Fallback: if the timed start is missed (for example the clip's length is unknown, or the next clip is not ready), the end of the clip (`ended`) starts the next one with a plain cut.
- While the window is not being drawn (measured in a hidden test window) the page gets no animation frames. The clock is then read only about four times a second (`timeupdate`), and a dissolve can start up to a quarter of a second late.
- The dissolve is deliberately **not** switched off by the Windows setting "Show animations in Windows" (`prefers-reduced-motion`). With that setting off, a rule like that would remove the transition completely; a dissolve is the gentler change. To honour the setting, use `FADE = 0` when `matchMedia('(prefers-reduced-motion: reduce)').matches`.
- `bg-02` and `bg-06` begin with a stray still picture from the recording (0.17 s and 0.37 s). The clips are kept untrimmed. The dissolve hides most of it: with the 1.5 s fade the still reaches about 3% of full strength in `bg-02` and about 13% in `bg-06` before the real picture takes over. A shorter fade shows more of it (37% in `bg-06` with a straight 1 s fade).

The video fills the window edge to edge inside the rounded corners (cropped to fit, never stretched). It is dimmed so the white lettering of the see-through GUI stays readable: 40% in the middle, fading to 70% at the edges (a soft vignette), and another 27% inside the GUI frames (`--glass`). The two `rgba(...)` values in the `radial-gradient` on `#bg::after` in `style.css` set the dim. Do not lower them without reading **Readability** first: the 40% and `--glass` belong together. For the in-game blurred look, add `filter: blur(4px);` to `#bg video`.

#### The window edge

The videos must end exactly at the rounded window edge, on the real screen and not only in a screenshot. Three things take care of that:

1. **Two clips.** `body` is clipped to the rounded window shape (see **Window**), and `#bg` is clipped to the same shape by a `clip-path` of its own, so the videos are cut by their own container as well. Keep it a `clip-path`: Chromium draws a `clip-path` box into a surface of its own and masks that surface, so the mask is applied to the video's pixels. `border-radius` with `overflow: hidden` does not have that effect: Chromium can still hand such a video to an overlay plane (point 2).
2. **No overlay planes.** `src/main.js` sets the Chromium switch `disable_direct_composition_video_overlays` before the app is ready. On Windows, Chromium may hand a video to the system as a separate overlay plane under the page instead of drawing it into the page. Such a plane is cut to a plain rectangle; the rounded shape then comes only from the page drawn over it. In a see-through window nothing is drawn outside the rounded shape, so the square corners of the plane would show. With the switch a video is always drawn into the page, where both clips, the dim and the edge ring apply. In Chromium 130 (Electron 33) a video inside a `clip-path` surface is never handed to an overlay plane anyway (only videos drawn straight into the window's main surface are considered), so point 1 and point 2 each do the job alone.
3. **Same box.** `#bg` is `position: fixed; inset: 0` and the videos fill it, so the video box is the page box. Measured at 100%, 125% and 150% display scaling: the real window, the page and the video box are the same size to the pixel (900x600, 1125x750, 1350x900), every pixel on the four edges is covered and every pixel outside the rounded corners is clear.

The switch is a Chromium "GPU workaround" name, spelled with underscores. The spelling with dashes (`disable-direct-composition-video-overlays`) was removed from Chromium and does nothing in Electron 33. To check it after an Electron upgrade: `app.getGPUInfo('complete')` must give `auxAttributes.overlayInfo.supportsOverlays: false` (without the switch it is `true` on a PC whose graphics card supports overlays). All of this was measured in hidden test windows and read from the Chromium source; the visible window has not been compared by eye since.

**How much of the picture is cut:** the clips are 16:9 (1280x720) and the window is 3:2 (900x600). `object-fit: cover` scales a clip to the window's height and cuts what sticks out at the sides: 100 of the 1280 pixels on the left and 100 on the right (7.8% each, 15.6% together). Nothing is cut at the top or bottom, and nothing is stretched.

#### File size

The videos ship inside the installer, so they must stay small. The folder holds seven clips, 49.9 MB in total: 1280x720, 30 fps, H.264, no sound, 6.4 to 24.2 s each, 97.8 s together. They were made from the original screen recordings (1920x1080, 60 fps, 2.25 GB) with this command for each clip:

```
ffmpeg -colorspace bt709 -color_primaries bt709 -color_trc bt709 -color_range tv -i IN.mp4 -an -vf "fps=30,scale=1280:720:flags=lanczos" -c:v libx264 -profile:v high -preset slow -crf 21 -pix_fmt yuv420p -colorspace bt709 -color_primaries bt709 -color_trc bt709 -color_range tv -movflags +faststart OUT.mp4
```

Keep the colour flags on the input side too (before `-i`), or ffmpeg 9.1 converts the colours. Compress a new recording the same way before adding it: a raw recording is more than 40 times bigger.

**In the installed app:** `"asarUnpack": ["src/renderer/assets/videos/**"]` in `package.json` keeps the videos out of the `app.asar` archive. They are installed as normal files in `resources/app.asar.unpacked/src/renderer/assets/videos/`, and the code reaches them through the same path as in development. The list of videos is fixed when the installer is built: to change the videos of an installed launcher, change the folder here and build again. No installer has been built with videos yet, so this part is untested.

### Window

The window has no Windows title bar or border (`frame: false`, `transparent: true` in `src/main.js`). The page draws the window shape itself: `body` is clipped to a rounded rectangle (`--radius` in `style.css`, section "Window shell"), so the background videos and the launch animation all stop at the same edge (the videos have a second clip of their own, see **Background video**, "The window edge"). 1280x720, centred, not resizable, no maximize. On a screen where that does not fit (for example 1920x1080 at 150% scaling) the window shrinks in proportion and the page is zoomed by the same factor, so the layout stays the same: `fitScale()` in `src/fit.js` (self-check: `node src/fit.js`), used in `createWindow()` in `main.js`.

**Play screen (after login):** laid out like a game launcher's home screen, in `#play-view` in `index.html` and "Play: the home screen" in `style.css`. A sidebar down the left edge (icon; the GAME LOG button appears there once the game has written something and opens the log in a frame beside the logo), the big logo with PLAY under it at the upper left (status line, progress bar and error appear under PLAY), the player name with LOG OUT next to the window buttons at the top right, and five news cards along the bottom. **The news cards are placeholders** ("COMING SOON", `#news` in `index.html`): there is no news source yet. Buttons rise by one GUI pixel and brighten under the pointer or keyboard focus (`style.css`, "Hover"); with "Show animations in Windows" switched off only the colours change.

- **Dragging:** the top 64px strip (`#titlebar`) moves the window; the logo sits in it, so the window can be dragged by the logo too. Buttons, inputs and the log are marked `no-drag` in `style.css`; anything clickable you add inside a drag area needs the same, or it will not click.
- **Minimize / close buttons:** top right, in `#titlebar` in `index.html`. Wiring: click handlers in `renderer.js` call `window.api.minimize()` / `window.api.close()` (`preload.js`), which reach the `minimize` and `close` handlers in `main.js`. Closing the window quits the launcher, as before.
- **Version:** the small line in the bottom right corner (`#footer`) shows `version` from `package.json` (sent by `get-info` in `main.js`, written by `renderer.js`). It sits on the same line as the link buttons.
- **Pixel crispness:** everything is placed on whole pixels. The login bar is centred in the window, so it has to start on an even pixel: the side padding of the LOG IN button makes the bar 588px wide for that (`style.css`, the comment above `.bar button`). That keeps the pixel font sharp at 100% and at 150% display scaling; re-check it if you change the wording of the LOG IN button or the width of a typing field. A login error is not centred for the same reason. At 125% scaling one GUI pixel is 2.5 screen pixels, so lines and lettering are slightly soft there whatever is done.

A see-through window gets no Windows drop shadow, so a faint 1px ring is drawn on the edge instead (`box-shadow` on `#bg::after`). Checked on Windows 10 on a normal desktop only; not tested over remote desktop or with GPU acceleration off, where see-through windows sometimes show black corners.

### Links

Two icon buttons in a frame at the right edge of both screens, **DISCORD** and **WEBSITE**, open those pages in the player's default browser. Hovering a button slides out its name and shows its address.

- **Change a link:** edit `"links"` in `config.json` and restart the launcher (for an installed launcher, build the installer again). An address must start with `https://`. A link that is missing, empty (`""`) or not a valid `https://` address has no button.
- **How a link is opened:** the page never sends an address. A button sends only its name (`discord` or `website`) with `window.api.openLink()` (`preload.js`) to the `open-link` handler in `src/main.js`. That handler reads the address from `config.json`, refuses anything that is not a valid `https://` address, and only then opens the browser (`shell.openExternal`). The launcher window itself can never load another page or open another window (`will-frame-navigate` and `setWindowOpenHandler` in `createWindow()`).
- **Look:** they are ordinary buttons (`#links` in `index.html`), so they follow the shared button style; `style.css` ("Link buttons") only places them and sizes the icons. The icons are plain pixel glyphs drawn in the page (a speech bubble and a globe), not logos: to use official artwork, replace the `<svg>` in `index.html`.

## Launch animation

Pressing PLAY runs `window.weaponBurst()` from `src/renderer/burst.js` first, then the normal `play` call. The button is disabled on the first click, so double-clicking can't launch twice. If the animation fails for any reason, the launch still goes ahead.

How it works: on startup `burst.js` reads `assets/weapons.bbmodel` (Blockbench JSON) and builds a real 3D model per weapon with three.js. Every top-level group in the Blockbench outliner is one weapon (49 in the current file), every cube becomes a box with its own per-face UVs and rotation, and the base64 PNG textures inside the file are used as-is (for animated textures only the first frame is shown). On click, 18 random weapons pop out of the button in a transparent full-window canvas, spin, arc outwards and fall, then shrink away (about 1.9 s). Tune `DURATION`, `COUNT` and `SIZE` at the top of `burst.js`.

**Swapping the weapons:** replace `src/renderer/assets/weapons.bbmodel` with another Blockbench project saved as `.bbmodel` (File > Save Project) with textures embedded (the default). Put each weapon in its own top-level group, with its cubes directly inside it (nested sub-groups are ignored). No build step: restart the launcher.

## Mods

The client's mods are the `.jar` files in the project's `mods/` folder (15 today: Fabric API, Sodium, Iris, Lithium, Simple Voice Chat, Newgen-Addons, Newgen Client and others). The jars are not in this repository (`mods/` is in `.gitignore`); put them in `mods/` before `npm start` or `npm run dist`. The Newgen Client source is on the `client-mod` branch. The installer ships that folder next to the app (`extraResources` in `package.json`).

- **On PLAY** (`src/mods.js`, called from the `play` handler in `src/main.js`, after the manifest sync): the launcher copies any missing bundled jar into `%APPDATA%\.<folderName>\mods`, then starts the game.
- **Added mods are refused.** Anything else in that folder (another file, a subfolder, or a bundled jar whose content was changed, compared by SHA-256) stops the launch. The player sees a warning pop-up: `Adding Mods is not Allowed, please remove "cheat.jar" to continue using Newgen Launcher` (several names are listed with commas). Nothing is deleted; the player removes the file and presses PLAY again.
- **Change the mods:** add, remove or replace jars in `mods/`, then restart the launcher (for players: build and ship a new installer). The launcher remembers which jars it installed itself (`installed-mods.json` in its settings folder) and swaps its own old ones without blaming the player.
- **Check the logic:** `node src/mods.js` runs a small self-check.
- **With `manifestUrl` set:** every mod in the manifest must also be in `mods/`, or it is refused as an added mod.
- **Limit:** this is a check on the player's own PC, once, before the game starts. It stops casual attempts only. A determined cheater can add a file after the game starts, change the launcher, or join the offline-mode server with another launcher. Real enforcement needs a server-side check.

## Manifest format

```json
{
  "managedFolders": ["mods"],
  "files": [ { "path": "mods/x.jar", "url": "https://...", "sha1": "<sha1 hex>" } ]
}
```

Unlisted files inside `managedFolders` are deleted on each launch. Paths are checked so they can't escape the game folder. Get a hash in PowerShell with `Get-FileHash file.jar -Algorithm SHA1`.

## Not yet tested

The skeleton has not been run end-to-end yet: it was written on a PC without Node/Java. Check the first `npm start` carefully. The `quickPlayMultiplayer` / `--server` auto-join switch and `customLaunchArgs` depend on the MC version and the `minecraft-launcher-core` version.

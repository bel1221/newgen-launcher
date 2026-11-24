<!-- Final report of the transparent-GUI experiment sub-session (2026-11-24), saved word for word by the coordinator. The lab it refers to is in a temp folder that may be gone; recommended.css, runnerup-1.css and runnerup-2.css next to this file are copies of its deliverables. -->

<!-- Note added later on 2026-11-24 by the GUI refinement session (step 2). After this report the user chose another direction ("transparent GUI, only the clickable buttons solid, frames kept"), so recommended.css was NOT applied and src/renderer/style.css was rebuilt for that look (README "Branding" and "Readability"). recommended.css, runnerup-1.css and runnerup-2.css are patches for the earlier solid style.css, which is kept in solid-look-backup/ next to this file; they cannot be pasted onto the new style.css. transparent-alt-1.css and transparent-alt-2.css are patches for the new style.css. The measuring method described below was reused for the new look. -->

# See-through GUI experiment (step 1 of 3): result

No window was shown at any time. The lab ran only in hidden windows (never shown, never focused), so there was nothing to close. Nothing was written inside `C:\Users\Administrator\server-launcher` or `Desktop\cinemat`.

One deliverable is missing: **`FINDINGS.md` was not written.** The tool refused it (subagents may not write report files). Its content is the "Details for the next session" part at the end of this report, and the apply instructions are also in the header of `recommended.css`.

## What I recommend: "tinted pixel glass"

- The cream book pages and the tan panel become 75% solid, so the video shows through them softly.
- Everything else stays solid: outlines, bevels, plaques, the orange page frame, fields, buttons, the name slot, the cards.
- The dark overlay on the video is halved (40% to 20% in the middle, 75% to 55% at the edges), so the clips are clearly brighter around and behind the GUI.
- Four pieces of lettering use darker colours of the same palette (ACCOUNT, PASSWORD, the welcome line, GAME LOG). Error messages sit on a small solid strip. The footer gets a 1px dark outline.
- No blur, no change to the HTML, no new colours.

What it looks like: still clearly the cream book and the tan panel. Over the bright clips (sky, forest, the top-down map) you see the scene softly through the pages, like through thin paper. Over the dark cave clip the pages look like slightly smoked paper and the video shows only faintly. It is a moderate effect, not a strong one.

## Variants tried

Figures are the worst-case contrast of the body text on the book page (it needs 4.5) unless said otherwise.

- **Opaque (today):** baseline, 9.7. Four texts are already under their bar today: LOG IN plaque lettering 2.2, PASSWORD label 2.8, player name 3.0, footer over a bright clip 1.8.
- **90% solid:** kept as the "safest" setting (7.8). Hardly looks see-through.
- **75% solid:** kept, basis of the recommendation (5.5). The mauve, orange and blue words fail at this level (1.6 to 2.9), so they got darker colours.
- **60% / 45% / 30% solid:** rejected. 3.6 / 2.3 / 1.3; labels disappear; the cream turns grey over the dark clips.
- **Everything see-through (fields, buttons, cards too):** rejected. Every control loses contrast (PLAY 6.0 to 5.3 / 4.4 / 3.2) and buttons look disabled.
- **Frosted glass, blur 8px and 16px, behind a 90% surface:** rejected. The blur cannot be seen; same numbers as 90%.
- **Frosted glass behind a 60% surface:** look kept for runner-up 2. Blur does not improve the worst case (3.6 with or without): a blurred dark picture is as dark as a sharp one. It only calms busy pictures.
- **Frosted glass behind a 45% surface:** rejected (2.3).
- **Dark glass, 60% solid, light lettering:** kept as runner-up 1 (6.0).
- **Dark glass 75%, dark glass plus blur:** not needed. 60% already passes; blur adds nothing it needs.
- **Video dim: today's / lighter / off:** for cream and tan it makes no difference to the worst case (5.45 / 5.49 / 5.51), so the lighter one was chosen for looks. Dark glass needs today's dim (6.0 / 4.5 / 3.4).
- **Darker palette colours for the words:** kept. This is the text fix that works (ACCOUNT 1.9 to 5.4, PASSWORD 1.6 to 4.0, welcome line 2.9 to 5.8).
- **Hard 1px light shadow under the words:** kept only for runner-up 2. Not needed at 75%.
- **2px shadow, light outlines round page text:** rejected. Letters look doubled or fill in.
- **1px dark outline:** kept for the tiny footer only.
- **Dither (pixel checkerboard instead of transparency):** rejected (1.1). Half of each letter lands on raw video.
- **60% without blur, with the orange and blue words kept:** rejected. PASSWORD 2.7 and welcome line 4.4 on the cave clip. With one dark ink for all words it passes (4.7 to 4.9); that is runner-up 2 without its blur line.

## Key numbers

Recommendation, measured over all seven clips from start to end (548 frames, both screens):

| | bar | today | recommended |
|---|---|---|---|
| weakest body text on a see-through surface | 4.5 | 9.7 | **5.4** |
| weakest label on a see-through surface (PASSWORD) | 3 | 2.8 | **4.0** |

- No future clip can push these lower: against a fully black picture the arithmetic gives 5.4 and 3.9.
- Checked again on the final compressed clips of the other session: same figures (5.5 and 4.05 over 20 moments).
- Text on solid parts is unchanged. Two of those are under their bar today and still are: LOG IN plaque lettering 2.2 and player name 3.0.
- The weapon burst still draws above the GUI in all three files.
- Pixel fonts are exactly as crisp as today (identical letter pixels in 36 of 36 checks).

Performance comparison:

| | result |
|---|---|
| solid (today) | not measured: needs a visible window |
| plain opacity (the recommendation, dark glass) | not measured: needs a visible window |
| blur (frosted glass) | not measured: needs a visible window |

That is why the recommendation does not use blur. The measuring script is ready in the lab and was only dry-run.

## Runner-ups

1. **Dark glass** (`runnerup-1.css`). For "more see-through". Pages become deep-blue glass, the panel dark-brown glass, both 60% solid, with light lettering. The video shows through much more clearly and it is the most readable of all (body text 5.8 at worst, about 9 typically). The price: it is no longer the cream book and the tan panel, although shapes, plaques, frame, fields and buttons keep their colours. It must keep today's dim.
2. **Frosted glass** (`runnerup-2.css`). For "looks most like real glass". Cream and tan at 60% with the video blurred behind them; all words in one dark ink (PASSWORD loses its orange, the welcome line its blue). Everything passes (4.7). **Its blur is untested for performance.** Deleting its one blur line gives a measured, passing 60% version without blur.

"Safest and most readable" needs no separate file: set `--glass: 90%` in `recommended.css`.

## Where things are

- Screenshots: `C:\Users\Administrator\AppData\Local\Temp\claude\77f498d6ed4aea62\5bed0d65-a11b-4548-8168-7f590932f097\scratchpad\gui-lab\shots`
  - Top level: today's look and the three files over all seven clips.
  - `shots\compare`: labelled side-by-side sheets. Start with `allclips_recommended_login.png`, `allclips_recommended_play.png` and the `finalists_*` sheets.
  - `shots\experiments`: every rejected variant.
- Recommended CSS: `C:\Users\Administrator\AppData\Local\Temp\claude\77f498d6ed4aea62\5bed0d65-a11b-4548-8168-7f590932f097\scratchpad\gui-lab\recommended.css`
- Runner-ups: `C:\Users\Administrator\AppData\Local\Temp\claude\77f498d6ed4aea62\5bed0d65-a11b-4548-8168-7f590932f097\scratchpad\gui-lab\runnerup-1.css` and `C:\Users\Administrator\AppData\Local\Temp\claude\77f498d6ed4aea62\5bed0d65-a11b-4548-8168-7f590932f097\scratchpad\gui-lab\runnerup-2.css`
- `FINDINGS.md`: not written (see top). It would have been `C:\Users\Administrator\AppData\Local\Temp\claude\77f498d6ed4aea62\5bed0d65-a11b-4548-8168-7f590932f097\scratchpad\gui-lab\FINDINGS.md`.

## Not tested, and risks

- **Performance: not measured for any variant.** Frosted glass is unproven until it is measured. The recommendation's cost is expected to be nothing new (the video is already drawn under a see-through dim layer today), but that is reasoning, not a measurement.
- **Never seen in a visible window.** All pictures come from a hidden window. It is the same drawn page, but the window as Windows shows it (rounded transparent corners, the 1px edge ring) was not looked at.
- Not tried: hover, pressed, focus and disabled states (their rules are unchanged), Windows display scaling other than 100%, and the real playback code (the lab used its own simple playlist).
- 75% may feel too timid. The honest next step is then dark glass, not a lower percentage: below about 70% the body text fails on the cave clip.
- Dark glass depends on the dim staying at 40%.
- The project changed while I worked: another session added the Discord and Website buttons. I re-checked the recommended CSS on that newer `style.css`; the buttons stay solid and readable (6.0).
- The lab sits in a temp folder that a reboot or cleanup can clear. Copy the three CSS files somewhere durable if they should survive.
- One ffmpeg run (6 threads, about a minute) finished before the "no popups, low load" message arrived. None after it.

---

# Details for the next session (the would-be FINDINGS.md)

## Method

- Lab copy of the renderer with the unchanged `style.css`, same window options, stubbed login and launch. Electron 33.4.11 from the project, read-only. Hidden window with a frame subscription, because a hidden window otherwise stops rendering between screenshots.
- Each variant is a stylesheet loaded on top of `style.css`. The three delivered files were tested as they are.
- All seven clips as 1280x720, 30 fps test copies, really playing for every screenshot. Each clip was scanned for its brightest, darkest and busiest picture: 20 moments. The delivered files were also measured over every clip from start to end.
- Contrast: a second screenshot with all lettering made invisible shows the pixels really behind each text. The WCAG ratio is computed between the text colour and each of those pixels. Reported figure: the 5th-percentile pixel of the worst frame. The single worst pixel is in the JSON files.
- Bars: 4.5 for body text (paragraphs, typed text, log, errors, player name, footer). 3 for large labels (plaques, PLAY, the uppercase 16px captions) and the window-button glyphs. Under strict sizing the 16px captions would need 4.5; in the recommendation PASSWORD (4.0) and, on its solid slot, LOGGED IN AS (3.5, unchanged) sit between 3 and 4.5.
- Self-checks: the structural part of the patch at 100% opacity changes 0 pixels on both screens, also on the newest `style.css`. Captured colours are exact.

The footage is mostly dark (brightness 0 = black, 1 = white, before the dim): bg-01 0.08 calm; bg-02 0.19 falling to 0.01, a nearly black cave, the darkest; bg-03 0.07 to 0.26 busy; bg-04 0.08 calm; bg-05 0.06 calm; bg-06 0.03 to 0.32, the brightest and the busiest; bg-07 0.17 calm. A light surface with dark lettering has its worst case on a dark picture. That decides everything.

## Contrast, round one (3 moments: bright, dark, busy)

| variant | body (4.5) | ACCOUNT (3) | PASSWORD (3) | welcome (4.5) | GAME LOG (3) |
|---|---|---|---|---|---|
| opaque (today) | 9.67 | 3.46 | 2.78 | 5.19 | 6.00 |
| 90% | 7.77 | 2.78 | 2.25 | 4.16 | 4.92 |
| 75% | 5.45 | 1.95 | 1.58 | 2.92 | 3.50 |
| 60% | 3.58 | 1.26 | 1.04 | 1.92 | 2.37 |
| 45% | 2.25 | 1.01 | 1.07 | 1.19 | 1.55 |
| 30% | 1.32 | 1.09 | 1.49 | 1.01 | 1.00 |
| blur 16px + 90% | 7.82 | 2.78 | 2.25 | 4.17 | 4.92 |
| blur 16px + 60% | 3.61 | 1.28 | 1.04 | 1.92 | 2.43 |
| blur 16px + 45% | 2.28 | 1.00 | 1.17 | 1.21 | 1.61 |
| dither | 1.07 | 2.52 | 2.78 | 1.32 | 1.18 |
| 75% + darker words | 5.45 | 5.43 | 4.01 | 6.61 | 3.50 |
| dark glass 60% | 6.01 | 9.45 | 6.59 | 4.21 | 7.59 |
| dark glass 60%, dim off | 3.44 | 6.96 | 5.22 | 2.37 | 5.83 |

Everything see-through, the controls: typed text 5.18 to 5.23 / 4.47 / 3.33, LOG IN button 6.20 to 4.49 / 4.25 / 4.40, PLAY 6.00 to 5.34 / 4.39 / 3.23 (at 75 / 60 / 45%).

## Contrast, the delivered files, every clip from start to end

| text | bar | today | recommended (548 frames) | runner-up 1 (591) | runner-up 2 (591) |
|---|---|---|---|---|---|
| ACCOUNT label | 3 | 3.46 | 5.43 | 5.64 | 4.74 |
| PASSWORD label | 3 | 2.78 | 3.96 | 4.09 | 4.74 |
| welcome line | 4.5 | 5.19 | 5.75 | 6.52 | 4.74 |
| body text | 4.5 | 9.67 | 5.44 | 5.83 | 4.74 |
| offline note | 4.5 | 9.67 | 5.45 | 5.85 | 4.74 |
| login error (on its strip) | 4.5 | 6.63 | 6.63 | 6.63 | 6.63 |
| launch error (on its strip) | 4.5 | 4.61 | 4.61 | 4.61 | 4.61 |
| GAME LOG heading | 3 | 6.00 | 5.14 | 5.16 | 3.51 |
| footer against the video | 4.5 | 1.76 | 2.00 | 3.80 | 2.00 |
| footer against its own outline | 4.5 | none | 12.8 | 12.8 | 12.8 |
| body text, typical frame | | 9.67 | 5.85 | 8.98 | 5.67 |

("Today" is from the 20 moments.) Solid parts, the same in every column: typed text 5.18, LOG IN button 6.20, LOG OUT and PLAY 6.00, status 5.10, log 5.10, LAUNCHER plaque 4.50, LOGGED IN AS 3.47, window glyphs 6.28, and under their bar: LOG IN plaque 2.16, player name 2.97.

## What failed, and why

1. Low opacity with cream and tan kept: the surface takes on the darkness behind it. Only the opacity changes that worst case, so about 70% is the floor with today's body colour; 75% leaves a margin.
2. Blur as a legibility fix: no effect on the worst case. It is a look, not a fix.
3. The dim as a legibility fix for cream and tan: no effect, for the same reason. It matters only for light lettering and the footer.
4. Today's label colours on any see-through surface: already near or under their bar on solid cream.
5. Everything see-through: the controls are where the reading happens.
6. Thick or four-sided outlines on page text: the font's strokes are 2px, so the letters fill in.

## How to apply the recommendation

- Markup change: none.
- Quickest, and exactly what was tested: paste the rules of `recommended.css` after the last rule of `src/renderer/style.css`. The header of that file also lists the same change rule by rule, for merging in place.
- Selectors it relies on: `.book`, `.page`, `.panel`, `label`, `label.accent`, `.welcome`, `.error`, `summary`, `#footer`, `#bg::after`. It adds one new rule, `.page::after`, which draws the orange page frame.
- Dim and vignette: 0.2 in the middle, 0.55 at the edges (today 0.4 and 0.75). This is taste, not need. For dark glass keep today's values.
- `--glass` is the only number to tune: 75% is the lowest that passed, 90% the safe end.
- Afterwards update the comment on `--spine`, the comment above `#bg::after`, and in `README.md` the label colours under "Branding" and the dim percentages under "Background video".
- A hint for the minimal redesign: on the Play screen only the GAME LOG heading and the error line sit on the panel itself. If the login page's text also sat on a solid card, the surfaces could go well below 75%.

## Weak spots that exist today, not caused by transparency

- LOG IN plaque lettering 2.16 (needs 3) and player name 2.97 (needs 4.5): left alone, they sit on solid parts.
- PASSWORD label 2.78 and footer over a bright clip 1.76: both fixed by the patch.
- The pixel fonts carry a faint 1px smoothing fringe at 1:1 from Windows text rendering. Unchanged by this work; switching ClearType off in the lab made no measurable difference.

## Lab

Folder: `C:\Users\Administrator\AppData\Local\Temp\claude\77f498d6ed4aea62\5bed0d65-a11b-4548-8168-7f590932f097\scratchpad\gui-lab`. Variants in `lab\variants.js`, test driver `lab\driver.js`, raw numbers in `measure\contrast`, `measure\dense`, `measure\clips.json`, `measure\burst.json`, `measure\selftest.json`.

Re-run a measurement (Git Bash, from the lab folder, hidden window):

```
E="C:/Users/Administrator/server-launcher/node_modules/electron/dist/electron.exe"
LAB_MODE=shots LAB_ARG=clips LAB_VARIANTS=base,recommended "$E" .
node lab/report.js clips base,recommended
LAB_MODE=dense LAB_VARIANTS=recommended "$E" .
node lab/report.js dense recommended
```

Performance, still to do, on an idle PC with the window on screen (about 4 minutes each; the second line turns the GPU off as a stand-in for a weak PC):

```
LAB_SHOW=1 LAB_MODE=perf LAB_VARIANTS=base,op60,recommended,frost8-op60,frost16-op60,runnerup-2 "$E" .
LAB_SHOW=1 LAB_SOFTWARE=1 LAB_MODE=perf LAB_VARIANTS=base,op60,recommended,frost8-op60,frost16-op60,runnerup-2 "$E" .
```

It records dropped video frames, frame intervals and process CPU into `measure\perf.json` and `measure\perf-software.json`. Note that this PC's display runs at 300 Hz, so it is a best case in two ways.

Skipped: all performance measurement, and any look at a visible window. Risk: `FINDINGS.md` does not exist as a file, and the lab is in a temp folder.

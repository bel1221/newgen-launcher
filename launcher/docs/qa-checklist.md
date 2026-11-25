# Newgen Launcher – QA release checklist (runtime, fresh player)

Run on a release build (`cargo build --release`) with a "fresh PC": rename
`%APPDATA%\Newgen Launcher` and `%APPDATA%\.myserver-launcher` to `*.qa-backup` first,
restore them at the end. Run with `RUST_LOG=newgen_launcher=info,warn`, capture stdout/stderr.
Record PASS/FAIL + evidence (screenshot / log line) per row.

| # | Test | Expected |
|---|------|----------|
| 1 | First start | Background video plays, live player count shows, no errors in log |
| 2 | Log in as `QaTester` | Lands on Play screen, name shown top-right |
| 3 | First Play | Weapon burst, then Play button fills "Updating NN%" while Java + MC + Fabric download |
| 4 | Game starts | `.myserver-launcher\logs\latest.log` shows connecting/joining newgenmc.net |
| 5 | Game log button | Appears while game runs, shows game output |
| 6 | Close Minecraft | Launcher shows "Game closed", Play enabled again |
| 7 | Added mod blocked | Drop `qa-added-mod.jar` into `.myserver-launcher\mods\`, Play → native warning "Adding Mods is not Allowed, please remove "qa-added-mod.jar"…", game does not start. Delete jar after |
| 8 | Keep launcher open = off | Play → launcher minimizes when game runs. Close game, switch setting back on |
| 9 | RAM slider 2 GB | Play → java cmd line contains `-Xmx2048M`; close game |
| 10 | Second Play | No re-download, game starts quickly (time it) |
| 11 | No videos | Rename videos folder away → launcher shows plain background, no crash; rename back |
| 12 | EN ↔ VI | All text switches, nothing cut off/overlapping (screenshot both) |

Not covered automatically: server offline (needs network block), crash dialog (not built).

Cleanup: both `%APPDATA%` folders restored to original names, test data removed,
videos folder restored, dummy jar gone, settings restored.

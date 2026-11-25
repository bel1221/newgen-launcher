# Cosmetics and Emotes for NewGen: Beginner's Guide

Written 2026-11-26 for: Minecraft 1.21.11, Fabric loader 0.20.1, Paper-style server, offline mode (AuthMe).
Anything marked **(unverified)** could not be confirmed for 1.21.11 from official sources.

> **Update 2026-11-26:** the chosen direction is now a **client-side** system in `newgen-client`. See `client-cosmetics-design.md`. The server-plugin path below (HMCCosmetics) is kept as reference only.

---

## 1. Summary: what to do

1. **Emotes:** add 2 jars to the launcher's `mods/` folder: **Emotecraft (Fabric, MC1.21.11)** and **Player Animation Library**. Fabric API is already there. Rebuild and ship the launcher.
2. **Server:** put the **Emotecraft Paper plugin** (the `MC1.21.11` build) in `plugins/`. Without it, players can play emotes but others won't see them.
3. **Emotes for everyone:** put `.emotecraft` / `.json` emote files in the game's `emotes/` folder. You can ship them through the launcher's manifest sync. Players press **B** to open the emote wheel.
4. **Cosmetics (hats, backpacks, balloons):** do them **server-side** with a **server resource pack** and a cosmetics plugin. **HMCCosmetics** ($9.99) is the recommended one. Hook it into your existing Cosmetics / Battle Pass / VIP menus.
5. **Client mods for cosmetics are optional.** Because every player uses your launcher, your own `newgen-client` mod *could* draw capes and hats later. Do the server-side version first: it needs no launcher update for each new cosmetic.

---

## 2. Emotes with Emotecraft

### 2.1 What Emotecraft is

- It is a mod that adds animations ("emotes") to the player model: dances, waves, sitting poses, and so on. You open them from a wheel (default key **B**).
- **Client side:** every player needs the mod. Your launcher ships it, so this is automatic once you add the jars.
- **Server side:** the server needs Emotecraft too (as a Paper plugin or a Fabric server mod) so emotes are sent to other players. When a player plays an emote, the **whole emote file is sent through the server**. Other players see it even if they don't have that emote file.
- Licence: GPL-3.0. Free.

### 2.2 Files you need (Fabric client, MC 1.21.11)

The latest builds on Modrinth when this was written (Modrinth API, 2026-11-04):

| Jar | Required? | Where |
|---|---|---|
| `emotecraft-fabric-for-MC1.21.11-3.2.0-b.build.176.jar` | **Required** | https://modrinth.com/mod/emotecraft (filter: 1.21.11, Fabric) |
| `PlayerAnimationLibFabric-1.1.12+mc.1.21.11.jar` (Player Animation Library) | **Required** | https://modrinth.com/mod/player-animation-library |
| Fabric API | **Required** (you already ship `fabric-api-0.141.6+1.21.11.jar`) | already in `mods/` |
| `BendableCuboidsFabric-2.0.1+mc.1.21.11.jar` (Bendable Cuboids) | Optional: makes arms and legs bend smoothly at elbows and knees | https://modrinth.com/mod/bendable-cuboids |
| Searchables | Optional: search box in the emote menu | https://modrinth.com/mod/searchables |
| fabric-permissions-api | Optional: only matters on a *Fabric* server | https://modrinth.com/mod/fabric-permissions-api |

Notes:
- Older guides say you need "playerAnimator" or "bendy-lib". **That is outdated.** Emotecraft 3.x uses **Player Animation Library** (required) and **Bendable Cuboids** (optional).
- Only download from official sources: Modrinth, CurseForge, GitHub (KosmX/emotes), or Hangar. The Emotecraft page warns about fake copies.
- The file says `b.build` (beta). This is the normal release line for 1.21.11; there is no "stable" build with a different name.

**My suggestion:** ship the two required jars plus Bendable Cuboids, because emotes look much better with bending limbs. Skip Searchables unless players complain they can't find emotes.

### 2.3 Step by step: add it to the launcher (client)

1. Download the jars above (the **Fabric** files with `1.21.11` in the name, not NeoForge or Paper).
2. Copy them into `c:\Users\Administrator\server-launcher\server-launcher\mods\`, next to `fabric-api-...jar`.
3. If you use a sync manifest (`manifestUrl` in `config.json`): add each new jar to the manifest too. Every mod in the manifest must also be in `mods/`, or the launcher treats it as an "added mod" and blocks it.
4. Rebuild the launcher (`npm run dist` or your Rust build) and ship the new installer.
5. On PLAY, the launcher copies the new jars into `%APPDATA%\.myserver-launcher\mods`. Players don't do anything.

### 2.4 Step by step: server side (Paper)

1. On the same Modrinth page, filter by **Paper** and download `emotecraft-paper-for-MC1.21.11-3.2.0-b.build.176.jar`.
   - Hangar (hangar.papermc.io) still shows only an old 1.21.4 build. **Use Modrinth.**
2. Put it in your server's `plugins/` folder and restart. No other plugin is needed. ProtocolLib was only required by old versions.
3. A Fabric client and a Paper server work together. Emotecraft is built so client and server loaders can differ.
4. Optional server commands and permissions (give them with LuckPerms or similar):
   - `/emotes play <emote> <player> [forced]`: permission `emotes.play.player`
   - `/emotes stop <player>`: `emotes.stop.player` (`emotes.stop.forced` to stop a forced emote)
   - `/emotes reload`: `emotes.reload`
   - Players can also use client-only commands: `/emotes-client play <emote>` and `/emotes-client stop`.
5. **Using a proxy (Velocity/BungeeCord)?** Big emotes can kick players with "Payload may not be larger than ...".
   - On Velocity, add `-Dvelocity.max-plugin-message-payload-size=1048576` to the startup command.
   - BungeeCord/Waterfall can't raise this limit.
   - With no proxy, ignore this step.
6. **Offline mode / AuthMe:** the docs don't mention offline mode. Emotecraft only relays animation packets between connected players and doesn't contact Mojang, so it should work. **(unverified, so test it)**. One thing to check: whether a player who hasn't logged in to AuthMe yet can play emotes in the login area. If they can, you may want to block that.

### 2.5 Adding ready-made emotes

- **File types:**
  - `.emotecraft`: a single file.
  - `.json`: may come with a `.png` icon and/or a `.nbs` music file of the **same name**. Keep them together.
  - These are plain data files, not code.
- **Folder:** the `emotes` folder sits next to the `mods` folder. For your players that is
  `%APPDATA%\.myserver-launcher\emotes\`
  You can also open it in game: Mod Menu, then Emotecraft config, then **Open emotes folder**.
- **Where to get emotes:** the community library RedlanceEmotes (https://emotes.redlance.org), which the Emotecraft docs link to, or the Emotecraft Discord.
  - Check each emote's author and terms before you ship it to everyone (see Pitfalls).
- **Making emotes available to every player** (so it shows in their wheel):
  - **Option A, through the launcher (recommended):** host the files and list them in your sync manifest, for example `{ "path": "emotes/newgen_wave.emotecraft", "url": "https://.../newgen_wave.emotecraft", "sha1": "..." }`.
    - **Don't** add `"emotes"` to `managedFolders` unless you want to delete players' own emotes. Unlisted files in managed folders are deleted on every launch.
  - **Option B, nothing:** players who don't have an emote still *see* it when someone else plays it, because the server sends the whole animation. They just can't play it themselves.
  - Server-side emote folder for `/emotes play`: the docs don't say where the server loads emotes from. Older Emotecraft read a server `emotes/` folder. **(unverified for 3.x)**. Ask on the Emotecraft Discord if you want to force emotes from the server.

### 2.6 Using emotes in game (tell your players)

1. Press **B** to open the emote wheel. Left-click an emote to play it.
2. To fill the wheel:
   1. Open the emote menu.
   2. Click an emote, then click a wheel slot.
   3. Click **Done**.
3. To put an emote on its own key:
   1. Click the emote.
   2. Click the box left of **Reset** and press a key.
   3. Click **Done**.
4. You can change the B key in Options, then Controls. Simple Voice Chat's default keys don't use B (unverified for your config).

### 2.7 Making your own emotes (Blockbench, free)

1. Install **Blockbench** (https://www.blockbench.net).
2. In Blockbench, open File, then Plugins, and install **GeckoLib Animation Utils**.
3. Download the player rig `.bbmodel` from the `blender` folder of the **KosmX/emotes** GitHub repo (branch `dev`) and open it.
4. Animate the player on the timeline using keyframes on the head, body, arms and legs.
5. Export the animation as JSON (GeckoLib/Bedrock animation format). Exact export menu: **(unverified)**; the docs show `format_version 1.8.0` / `geckolib_format_version 2` in the example.
6. Optional: add a `player_animation_library` block with `name`, `description` and `author`. These are Minecraft text components, so colours and formatting work.
7. Optional extras the docs cover: looping, fade in/out, custom pivot points, MoLang (motion that reacts to the player), and music (`.nbs`, made with Note Block Studio).
8. Keep the file **under 1 MB** (the emote packet limit).
9. Put the `.json` in your own `emotes` folder, reopen the menu, and test it.
10. Then add it to the manifest (2.5, Option A).

Blender works too (there's a Blender guide in the docs). It's more powerful but harder for a beginner.

### 2.8 Testing checklist

1. Two accounts on two PCs (or two launcher instances with different offline names) on the server.
2. Player A presses B and plays a built-in emote. **Player B must see it.** If B doesn't, the server plugin is missing or the wrong version.
3. Player A plays a *custom* emote that B doesn't have. B should still see it.
4. Test with **Iris shaders on and off** and with Sodium. All are in your pack; they should be compatible, but this is **(unverified)** for these exact versions.
5. Test while not logged in to AuthMe (see 2.4, step 6).

---

## 3. Cosmetics

### 3.1 What your Cosmetics / Battle Pass / VIP screens are

The reference screenshots (`docs/reference/ingame-cosmetics.png`, `ingame-battlepass.png`, `ingame-vip-rank.png`) are **server-side GUIs, not client mods**:

- Each one is a normal chest/inventory screen. You can see the 9-wide slot grid and the player's own hotbar row at the bottom.
- It is re-skinned by a **server resource pack**. A custom font draws the orange panel, red title bar and buttons as one big image behind the slots. This is the "negative-space font / custom GUI texture" trick.
- The items inside (gems, crates, hats, the glider, the kart) are normal items with custom models from the same resource pack.
- So: **a server plugin opens the menu, and the resource pack makes it look pretty.** No client mod is involved.
- Any cosmetics system you add should also be server-side, so it plugs into those menus: buying, battle-pass rewards, VIP perks.
- "Game" and "Lobby" cosmetics in the screenshot (a glider, a go-kart) are typical of minigame networks. Those are usually **ModelEngine** models or custom plugins. That is more advanced than hats.

### 3.2 The options

| Approach | What players see | Works offline-mode? | Who needs what | Cost | Verdict |
|---|---|---|---|---|---|
| **A. HMCCosmetics + server resource pack** (Paper plugin) | Hats, backpacks, balloons, off-hand items, cosmetic armour, a "wardrobe" preview room. Visible to **everyone** | Yes. It's a normal server plugin and doesn't contact Mojang (UUID-keyed data still works with offline UUIDs) | Server: plugin + ProtocolLib 5+. Client: only the resource pack (auto-downloaded on join) | $9.99 one-time (Spigot / Polymart (now voxel.shop) / BuiltByBit). Source is GPL-3.0 on GitHub, so you *could* build it yourself | **Recommended** |
| **B. DIY vanilla items** (resource pack + `item_model` / `equippable` components, given with commands or your own plugin) | Hats only, and the hat really sits in the helmet slot, so players lose real helmet armour | Yes | Server: resource pack + commands/plugin | Free | OK for a quick test, bad for PvP |
| **C. ModelEngine** (+ optional ItemsAdder / Oraxen / Nexo) | Animated 3D models: pets, mounts, gliders, karts | Yes | Server plugin(s) + resource pack | Paid (ModelEngine 4 is premium) | Later, for "Game/Lobby" cosmetics like in your screenshot |
| **D. Public client cosmetic mods** (cape/cosmetic mods with their own online service) | Capes/hats only for people with the same mod | Usually **no**: most link cosmetics to a real Mojang/Microsoft account | Client mod for everyone | Varies | Not a fit (offline accounts, locked pack) |
| **E. Your own `newgen-client` mod draws cosmetics** | Anything you code (capes, wings, hats) | Yes, you control it (key by player name) | Client mod update + an API listing who owns what | Free, but a lot of Java/Fabric work | Possible later. Unlike most servers, *all* your players run your mod |

Sources for HMCCosmetics: voxel.shop/Polymart listing (v2.9.2, Spigot + Paper, requires ProtocolLib 5.0+, optional ItemsAdder/Oraxen/ModelEngine 4). The BuiltByBit changelog says v2.9.0 added 1.21.11 resource-pack support. I could not open its docs to check every feature on 1.21.11, so treat exact 1.21.11 behaviour as **(unverified until you test it)**.

### 3.3 Recommended path: HMCCosmetics + one server resource pack

**Why this one:**
- It works for every player with **zero launcher changes** per new cosmetic. You add a hat by editing the server's resource pack and a config file.
- It runs fine on an offline-mode Paper server.
- It's server-side, so it fits the menu style you already have, and you can sell or reward cosmetics through your battle pass and VIP rank.

**Step by step:**

1. **Buy and install:**
   1. Buy HMCCosmetics (Spigot resource 100107, Polymart/voxel.shop 1879, or BuiltByBit).
   2. Download **ProtocolLib** (latest dev build that supports 1.21.11, from https://ci.dmulloy2.net or Hangar/SpigotMC).
   3. Put both jars in `plugins/` and restart once so the configs are generated.
   4. Read the plugin's own install notes (https://docs.hibiscusmc.com). It may also ask for **HibiscusCommons** (unverified).
2. **Make or buy the models:**
   1. Open Blockbench and choose "Java Block/Item".
   2. Model a hat around the head (16×16 px textures keep the Minecraft style).
   3. Export it as a Java item model `.json` plus its texture `.png`.
   4. Backpacks and balloons are also just item models, positioned by the plugin.
3. **Build the resource pack:**
   1. Put your models in a resource pack for 1.21.11 (`pack.mcmeta` with the right `pack_format`/version range for 1.21.11; check the Minecraft Wiki "Pack format" page).
   2. 1.21.4+ uses **item model definitions** (`assets/<namespace>/items/<name>.json`) and the `item_model` component. Older tutorials use `CustomModelData` numbers. Follow HMCCosmetics' examples for 1.21.11.
   3. If you already have a resource pack for your menus, **add the cosmetics to that same pack.** The server can only push one pack the simple way.
4. **Host the pack and make the server send it:**
   1. Upload the `.zip` somewhere with a direct download link (your web host, or a free pack host such as mc-packs.net).
   2. In `server.properties`, set:
      - `resource-pack=<direct URL>`
      - `resource-pack-sha1=<sha1>` (get it with `Get-FileHash pack.zip -Algorithm SHA1`)
      - `require-resource-pack=true`
   3. Change the URL or SHA-1 every time you update the pack so clients download it again.
5. **Configure the cosmetics:**
   1. In `plugins/HMCCosmetics/cosmetics/`, create an entry per item: type (helmet/backpack/balloon/offhand), the item and its model, and a **permission** like `hmccosmetics.cosmetic.newgen_crown`.
   2. In the menus config, add them to the cosmetics menu.
6. **Connect it to your economy and ranks:**
   1. Give the permission to the VIP group (LuckPerms), as a battle-pass reward (the reward runs `lp user <name> permission set ...`), or from a shop/crate.
   2. Point your existing **Cosmetics** button to `/cosmetics` or `/wardrobe`.
7. **Test:**
   1. Equip a hat. A second player must see it.
   2. Wear real armour. The hat must still show and the armour must still protect.
   3. Test with Iris shaders on.
   4. Rejoin and check the hat is remembered.

**Later steps (optional):** ModelEngine for animated "Game/Lobby" cosmetics (gliders, karts, pets). Or a cape/wing renderer in `newgen-client` that reads a JSON list from your website (`{"PlayerName": ["wings_gold"]}`). That one does need a launcher update for each new model, unless the mod downloads textures at runtime.

---

## 4. What changes in the launcher

| For | Change | Where |
|---|---|---|
| Emotecraft | Add `emotecraft-fabric-for-MC1.21.11-...jar`, `PlayerAnimationLibFabric-...+mc.1.21.11.jar` (and optionally `BendableCuboidsFabric-...jar`) | `server-launcher\mods\`, then rebuild and ship a new installer. If you use `manifestUrl`, list them in the manifest too |
| Server-wide emotes | List emote files in the sync manifest with paths `emotes/<file>`. Don't make `emotes` a managed folder unless you want to forbid player emotes | The manifest JSON at `manifestUrl` (`config.json`) |
| Optional: open the emotes folder | Nothing needed. Players can use the in-game "Open emotes folder" button | none |
| Cosmetics (recommended path) | **Nothing.** The resource pack comes from the server (`server.properties`) when the player joins | Server only |
| Cosmetics (client path E, later) | New `newgen-client` build with cosmetic rendering, and a website JSON listing who owns what | `client-mod` branch, `mods\newgen-client-x.y.z.jar` |
| Resource pack preloading (optional) | You *could* ship the pack in `resourcepacks/` through the manifest to skip the download prompt, but the server-pushed pack is simpler and always up to date | Manifest |

The emotes folder is outside `mods/`, so the launcher's "Adding Mods is not Allowed" check doesn't touch it. Players can add their own emotes there, and that's harmless because emote files are data, not code.

---

## 5. Pitfalls

- **Version mismatch:**
  - Every jar must say **1.21.11** and **Fabric** (client) or **Paper** (server).
  - A 1.21.10 or 1.21.4 jar will crash the game or be ignored.
  - Your `placeholder-api-2.8.2+1.21.10.jar` is already a 1.21.10 build. It works today, but watch it when you update.
  - Hangar's Emotecraft page is out of date (1.21.4); use Modrinth.
- **Keep versions in step:** when you update the client Emotecraft, update the server plugin to the same build number. Mismatched builds may fail to sync emotes **(unverified, but common)**.
- **Old dependency names:** guides mentioning playerAnimator or bendy-lib are for Emotecraft 2.x. You need Player Animation Library.
- **Offline mode:**
  - Never use cosmetic/cape mods that log in to a cloud service with a Microsoft account. Offline players have no such account.
  - Server plugins (Emotecraft, HMCCosmetics) work, but player data is keyed by **offline UUID**. If you ever switch the server to online mode, players' UUIDs change and owned cosmetics would need migrating.
- **AuthMe:** make sure players can't emote or open cosmetics before logging in (test it).
- **Resource pack:**
  - Only one server pack is pushed the simple way. Merge menus and cosmetics into one pack.
  - Use a *direct* download link (not a Google Drive or Dropbox preview page).
  - Update the SHA-1 every time.
- **Proxy payload limit:** big emotes over BungeeCord can kick players. Use Velocity with the JVM flag, or keep emotes small.
- **Licences:**
  - Emotecraft and Player Animation Library are free (GPL-3.0 / MIT). Shipping the unmodified jars in your launcher is fine under those licences. Keep their licence files and credit them.
  - **Emotes from the community library belong to their authors.** Many are fan-made copies of Fortnite or other game dances, and the dance or music may be copyrighted. Only ship emotes whose author allows redistribution, or make your own.
  - **HMCCosmetics** is a paid licence ("use on projects you own with attribution"). Don't share the jar.
  - **Cosmetic models from marketplaces** (BuiltByBit, etc.) have their own licences. Most forbid resale or redistribution outside your server.
  - Don't copy textures or menu art from other servers (for example, if those reference screenshots came from another network). Make your own versions.

---

## 6. Sources

- Emotecraft on Modrinth (overview, requirements, keybind B, emotes folder, licence): https://modrinth.com/mod/emotecraft
- Modrinth API, Emotecraft builds for 1.21.11 (file names, dependencies): https://api.modrinth.com/v2/project/emotecraft/version?game_versions=%5B%221.21.11%22%5D
- Dependency projects: https://modrinth.com/mod/player-animation-library, https://modrinth.com/mod/bendable-cuboids, https://modrinth.com/mod/searchables, https://modrinth.com/mod/fabric-permissions-api
- Emotecraft docs, installing (Paper needs no other plugin): https://docs.zigythebird.com/emotecraft/gettingstarted/installing
- Emotecraft docs, custom emotes (formats, folder, library): https://docs.zigythebird.com/emotecraft/gettingstarted/customemotes
- Emotecraft docs, using (wheel, binding): https://docs.zigythebird.com/emotecraft/gettingstarted/using
- Emotecraft docs, commands and permissions: https://docs.zigythebird.com/emotecraft/gettingstarted/commands
- Emotecraft docs, proxies (payload limit): https://docs.zigythebird.com/emotecraft/gettingstarted/proxies
- Emotecraft docs, creating emotes and Blockbench: https://docs.zigythebird.com/emotecraft/creatingemotes/ and https://docs.zigythebird.com/emotecraft/creatingemotes/blockbench
- Emotecraft source (GPL-3.0, server sync): https://github.com/KosmX/emotes
- Emotecraft on Hangar (outdated, 1.21.4): https://hangar.papermc.io/dima_dencep/emotecraft
- Emote library: https://emotes.redlance.org
- HMCCosmetics source (GPL-3.0, "premium plugin"): https://github.com/HibiscusMC/HMCCosmetics
- HMCCosmetics listing (v2.9.2, $9.99, ProtocolLib 5+, cosmetic types): https://voxel.shop/resource/hmccosmetics.1879
- HMCCosmetics updates (1.21.11 resource-pack support in v2.9.0): https://builtbybit.com/resources/hmccosmetics.31893/updates
- HMCCosmetics docs: https://docs.hibiscusmc.com
- Launcher behaviour (bundled mods, manifest, managedFolders): `server-launcher\README.md` ("Mods" section) and `launcher\src\sync.rs`, `launcher\src\mods.rs`

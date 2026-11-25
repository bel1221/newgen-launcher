# NewGen Client Cosmetics & Emotes: Design

## Current plan (2026-11-26): local-only emotes with sound

**The user's decision:**
- Emotes play **client-side only**. Only the player who emotes sees them, and only they hear the optional sound clip.
- No service, no networking, no server plugin.

The networked design further down is **shelved**. It's kept for reference if shared emotes or cosmetics are wanted later.

Code checked: branch `origin/client-mod` and the Design Manager's revamp (`newgen-client-revamp`, branch `client-mod-revamp`):
- Mod id `newgen_client`, Mojang mappings, MC 1.21.11, Fabric API 0.141.6.
- Features are `Mod` subclasses with `Setting`s, registered in `NewgenClient.onInitializeClient`.
- UI helpers are in `gui/Ui.java` (`Ui.BLUE = 0xFF2B4BFF`, `Ui.GLASS`, `Ui.tr()` → `lang/en_us.json` / `vi_vn.json`).
- `FreelookMod` already switches the camera to third person and back, which is the same pattern emotes need.
- *Side finding for the launcher team:* neither branch contains the `config/launcher-auth.json` → join-token hand-off that the README describes (no match for `launcher-auth`, `joinToken` or a custom payload). The auto-login hand-off is **not implemented in the mod yet**.

### L1. Build vs reuse: build it inside `newgen-client` on Player Animation Library

| | Emotecraft, client-only (no Paper plugin) | **Own "Emotes" feature in newgen-client + PAL** |
|---|---|---|
| Sound per emote | On the 1.21.11 line (3.2.0 builds 133–176) only **`.nbs` note-block songs** (changelogs: "Fix nbs", "Update NoteBlockLib"). Real audio (**`.opus`**, mono, 1 MB, shared with the emote) is in the current docs but appears only in newer versions for MC 26.x **(not in any 1.21.11 changelog)** | Any **OGG** clip, through normal Minecraft sounds |
| Adding audio without forking | Maybe: listen to Emotecraft's client "emote played" event and play our own OGG. That event API is **(unverified)** | Native |
| Works on a server without Emotecraft? | Its Modrinth page says on vanilla servers "the mod won't be active". Whether it still plays locally there is **(unverified)**. **This is a blocker risk** for newgenmc.net, which wouldn't run the plugin | Yes, it's purely local by design |
| Look and language | Its own menus, its own style | Revamp style (#2B4BFF, sentence case), EN/VI via existing `Ui.tr` |
| Locked modpack | +2–3 jars (Emotecraft GPL, PAL, optional Bendable Cuboids). Players can drop any emote file into `emotes/` | PAL only, packed **inside** the newgen-client jar (jar-in-jar), so the launcher's `mods/` list doesn't change. The emote list is curated by you |
| Effort | Hours | Days (see L6) |

**Decision: build it.** It is the only option that gives real audio clips on 1.21.11, works on a server without plugins, and matches the menus.

Dependency for `build.gradle`:
- Repository `https://repo.redlance.org/public`
- `modImplementation "com.zigythebird.playeranim:PlayerAnimationLibFabric:1.1.12+mc.1.21.11"`
- plus `include` with the same coordinates, to pack it in the jar.

PAL is MIT-licensed, so packing it is fine; include its licence text.

If Emotecraft is ever added too: both use PAL, so they can co-exist, but don't ship two emote wheels.

### L2. How the player sees themselves

- **Starting an emote:**
  1. Remember the current camera.
  2. Switch to `THIRD_PERSON_FRONT`, so you see your own face. A setting picks *Front / Back / Don't change*.
  3. Restore the camera when the emote ends or is cancelled, like `FreelookMod` does.
  4. If Freelook is active, leave the camera alone.
- **First person:** the default switches to third person, because in first person you'd only see arm movement. "Don't change" keeps first person. PAL's first-person arm rendering is **(unverified)**, so treat that mode as best-effort.
- **Cancel the emote (and its sound) on:**
  - movement input (WASD, jump, sneak)
  - attack or use (left/right click)
  - taking damage
  - riding, swimming, gliding, sleeping, or death
  - opening the inventory
  - changing dimension or disconnecting
- **Blocked from starting** while doing any of the above.
- Looping emotes run until cancelled. Pressing the wheel key again also stops them.
- **Gameplay is untouched.** The server sees a player standing still and other players see nothing, so local-only emotes can't be exploited.

### L3. Audio

- **Format:** OGG Vorbis (Minecraft's only sound format), **mono**, 44.1 kHz, quality around 4–5 (Audacity → Export → OGG).
- **Where the sound is defined:** in the mod's `assets/newgen_client/sounds.json`, for example:
  `"emote.wave": { "sounds": [ { "name": "newgen_client:emotes/wave", "stream": true } ] }`
  - Use `"stream": true` for clips over about 4 s.
  - The client can play a `sounds.json` id without registering it in code.
- **How it plays:** as a non-positional `SimpleSoundInstance`, started on the same tick as the animation and stopped on cancel (keep the instance; `soundManager.stop(...)`).
  - It's local, so **only the player hears it**. Nothing goes to the server.
- **Volume:**
  - Category `SoundSource.PLAYERS`, so the vanilla "Players" slider turns it down.
  - Plus the Emotes feature's own *Emote sound* switch and *Emote volume* (0–100 %).
  - A custom vanilla sound category would need mixins; not worth it.
- **Limits:** clip ≤ 15 s (loop the animation, not a long track), file ≤ about 300 KB, and normalise every clip to the same loudness so no clip is much louder than the others.
- **Licence:** only use clips you made, bought with a licence, or got under CC0/CC-BY (credit those).
  - **No** pop-music snippets, TikTok sounds, Fortnite emote audio or other game sounds. These are copyrighted even when they're short.
  - Keep a `CREDITS.txt` in the assets.

### L4. Content pipeline

**Files per emote** (namespace `newgen_client`):

```
assets/newgen_client/
  player_animations/wave.json     Blockbench animation; the animation inside is named "wave"
  sounds/emotes/wave.ogg          optional
  sounds.json                     entry "emote.wave" (optional)
  emotes/wave.json                {"animation":"newgen_client:wave","sound":"newgen_client:emote.wave","loop":false,"icon":"newgen_client:textures/emotes/wave.png","order":10}
  textures/emotes/wave.png        32×32 icon for the wheel
  lang/en_us.json, vi_vn.json     "newgen_client.emote.wave": "Wave" / "Vẫy tay"
```

- The mod builds the emote list by reading every `assets/*/emotes/*.json` on each resource reload, so a new emote needs **no Java change**.
- `player_animations/` is PAL's documented folder, and the ID comes from the animation name inside the file.

**Where the files live:**
- **MVP: bundled inside the newgen-client jar.** A new emote means a new jar, which goes into the launcher's `mods/` folder → rebuild and ship the launcher.
- **Later (optional): a "NewGen Emotes" resource pack** that the launcher syncs through the existing manifest into `resourcepacks/`, so emotes ship without a launcher update.
  - This depends on PAL reading `player_animations` through the normal client resource reload, so resource packs can add files **(unverified; test first)**.
  - Don't list `resourcepacks` in `managedFolders`, or players' own packs get deleted.

**How the server owner adds an emote with a sound:**
1. **Blockbench:** install the *GeckoLib Animation Utils* plugin and open the player rig `.bbmodel` from KosmX/emotes (`blender` folder, `dev` branch).
2. Create an animation named `wave` and keyframe the head, body, arms and legs.
3. Export the animation JSON and save it as `player_animations/wave.json`.
4. **Sound:** in Audacity, trim to ≤ 15 s, make it mono, normalise, and export as OGG to `sounds/emotes/wave.ogg`. Add the `sounds.json` entry.
5. **Meta:** add `emotes/wave.json`, the 32×32 icon, and the EN and VI names.
6. **Build:** in the `newgen-client-revamp` folder run `gradlew build` with the portable JDK. The jar is in `build/libs/`.
7. **Test:**
   1. Copy the jar to the launcher's `mods/` and press PLAY.
   2. In singleplayer: hold the wheel key, pick *Wave*, then check the animation, the sound, the camera switching, and that moving cancels it.
   3. Repeat on newgenmc.net.
   4. Faster loop while editing: press F3+T to reload resources without restarting **(this assumes PAL reloads with F3+T, unverified)**.
8. Ship: rebuild the launcher with the new jar.

### L5. UI: emote wheel

- **Key:**
  - *Hold* **B** (rebindable; registered as a `KeyMapping` in the Newgen category and as the feature's `Setting.Key`).
  - Release over a slot to play, or click a slot.
  - Esc or releasing in the centre cancels.
  - B is free because Emotecraft isn't shipped. Check it doesn't clash with Simple Voice Chat in your setup **(unverified)**.
- **Look:**
  - 8 slots in a ring over the game, which stays visible and isn't paused. Each slot is a `Ui.GLASS` round disc with the icon.
  - The hovered slot fills **#2B4BFF** (`Ui.BLUE`).
  - The centre shows the emote name in sentence case (`Ui.UI` font) plus a small speaker icon if the emote has a sound.
  - Mouse *direction* picks the slot, so no precise aiming is needed.
- **Choosing slots:** an "Emotes" card in the Mods menu (category Utility) with these settings:
  - wheel key
  - camera (Front / Back / Don't change)
  - emote sound on/off
  - emote volume
  - "Edit wheel": click a slot, then pick an emote from a grid
  - Saved like other mod settings.
- **Strings:** all under `newgen_client.emotes.*` in `en_us.json` and `vi_vn.json`. For example "Emotes", "Edit wheel", "Emote volume", "Camera when emoting".

### L6. MVP, effort, who builds it

**MVP:**
1. PAL packed in the jar.
2. The "Emotes" feature: JSON-driven list, play, cancel rules, camera switching, OGG sound with volume.
3. The 8-slot wheel with a fixed order (first 8 by `order`).
4. Settings card and EN/VI strings.
5. **6 starter emotes**, 3 of them with sound.

| Piece | Effort | Who |
|---|---|---|
| PAL setup, emote loader, play/cancel/camera, sound | ~3–4 days | **Design Manager**. They already build this mod with the portable JDK and know its `Mod`/`Ui` code |
| Wheel + settings card + EN/VI | ~2–3 days | **Design Manager** (it's their style system) |
| "Edit wheel" picker | ~1–2 days | Design Manager, after the MVP |
| 6 animations + clips | ~3–5 days, art-heavy | Whoever is best in Blockbench on the team, or commissioned. **Content, not code, is the real bottleneck** |
| Test pass (singleplayer, newgenmc.net, Iris on/off, cancel cases, volume) | ~1 day | **QA Engineer** (contract) |

**Total about 2 weeks. No new hire needed.** HR should consider a short Fabric contract only if PAL on 1.21.11 turns out harder than its docs suggest (rendering or mixin problems); about a week would be the trigger.

### L7. Cosmetics (the user decides)

Local-only cosmetics are **worth little**: the point of cosmetics is that others see them. Alone they only show in F5, the inventory preview and screenshots. They would need the shelved networked design below to matter. **Your call.**

---

# Shelved: networked design (kept for reference)

Written 2026-11-26. Replaces the server-plugin path (HMCCosmetics) in `cosmetics-emotes-guide.md`.

**Goal:**
- Cosmetics (hats, back items, capes, later wings and auras) and emotes live in the `newgen-client` Fabric mod (MC 1.21.11).
- Only players who run the NewGen client see them.
- They work on offline-mode servers with **no server plugin**.

**(unverified)** marks anything I could not confirm.

**Caveat on the mod source (outdated):** when this was written, the `client-mod` branch was not in the local repo. It has been fetched since; see the current plan above. So this design is based on the README's description of the mod:
- It reads `config/launcher-auth.json`, deletes it, and sends the join token on a plugin channel.
- It is not based on the mod's actual code. Check the hooks below against it.

---

## 1. Architecture

**The core problem:** the server relays nothing. Client A has to learn from somewhere else what player B is wearing and when B starts an emote.

| Option | How | Works on any server? | Verdict |
|---|---|---|---|
| **(a) NewGen cosmetics service** | HTTPS for catalog, ownership and equipping. One WebSocket per client for live presence and emote events, grouped into "rooms" by server address | Yes, any server both players can join | **Recommended** |
| (b) Plugin-message relay | Clients send cosmetics/emotes on a custom channel and a server plugin forwards them | No, needs a plugin on every server | Only as an optional extra on newgenmc.net (lower latency), not the base |
| (c1) Peer-to-peer | Clients talk to each other directly | NAT and firewalls break it, and it leaks players' IPs to each other | No |
| (c2) Hide data in the skin or the tab-list name | Encode the loadout in something the server already relays | Too fragile and tiny, and the server can change those fields | No |
| (c3) Ship Emotecraft and relay its emotes through a proxy | Emotecraft has an official **proxy API** for servers without Emotecraft. "Online Emotes" uses it with a public WebSocket relay | Yes | Viable for emotes only. No ownership checks, and it sends *full* emote data from any file a player has. See §4 |

### Recommended design: presence rooms

When a player joins a world, the client sends this to the service over its WebSocket:

```json
{ "hello": { "protocol": 1, "serverKey": "<sha256 of normalised host:port>", "name": "<in-game name>" } }
```

- The service puts the client in room `serverKey`.
- It sends back every other NewGen player in that room, each with their **equipped loadout**.
- After that it pushes `join` / `leave` / `equip` / `emote` / `stop` events.
- Client A matches room members to the players in its world **by name**. In offline mode, the name and the offline UUID are the same identity.
- It only draws cosmetics for players who are present in the room. **It never looks up cosmetics by name alone.** So a non-NewGen player called "Bob" on another server can't show the real Bob's cosmetics: the real Bob's account isn't in that room.

Details:
- `serverKey`:
  - Lowercase the host and add the port (default 25565). Hash it so the service doesn't store raw addresses.
  - Players who type different addresses for the same server (an IP vs a domain) end up in different rooms. For newgenmc.net that's fine. **Known limitation.**
  - Singleplayer: don't connect.
- Bandwidth:
  - Events are tiny: ids and timestamps, no animation data.
  - Assets (models, textures, animations) come from a CDN, are checked against a SHA-1, and are cached on disk.
- Failure: if the service is down, the game works normally and cosmetics just don't show. Reconnect with backoff.
- Which servers: "any server" really means **any offline-mode server**. The launcher starts the game with an offline profile, so players can't join online-mode servers anyway.

---

## 2. Identity on offline servers

Names are spoofable, so **the service must authenticate the client and must never trust a name the client sends.**

1. **Launcher** (Rust, `launcher\src\auth.rs`): after the existing login, call a new endpoint before each launch:
   `POST {apiBaseUrl}/launcher/cosmetics-token { sessionToken }` → `{ cosmeticsToken, expiresAt }`
   - The token is random, tied to the account and lasts about 24 h.
   - Write it to a **separate** file, `config/newgen-cosmetics.json` (`{ username, token, expiresAt }`).
   - Don't reuse `joinToken`. It is single-use and the server plugin consumes it (`/launcher/verify`).
2. **Mod:** read the file at startup, **delete it**, and keep the token in memory only.
   - Open `wss://cosmetics.newgenmc.net/live` with `Authorization: Bearer <token>`.
   - Call the HTTPS endpoints with the same token.
3. **Service:** the name in the room is the **account name tied to the token**, not the `name` field the client sends.
   - If the in-game name isn't the account name, reject the hello. The launcher always uses the account name as the offline name.
   - So nobody can join a room as someone else, and nobody can equip items they don't own.
4. Each login rotates the token. Rate-limit per account, and allow revoking a token (ban).

If `apiBaseUrl` is empty (offline dev mode), there's no token, so cosmetics are off.

---

## 3. Rendering cosmetics (Fabric 1.21.11)

**Model format: Blockbench `.bbmodel`, cubes only.**
- The launcher already parses `.bbmodel` (`launcher\src\burst.rs`, `cards.rs`). The same file can then drive the launcher preview *and* the game.
- The mod gets a small loader:
  1. Cubes (from/to, rotation, pivot, per-face UV) are turned into quads in the player model's coordinate space.
  2. They are cached once per item.
  3. They are drawn straight to a `VertexConsumer`.
- Textures: PNG from the CDN, loaded as `NativeImage` → `DynamicTexture`, registered as `newgen:cosmetic/<id>`.
- Animated items (flapping wings): start with simple procedural motion (sine on a bone). Add Blockbench keyframes later.
- Alternative: **GeckoLib** (`geckolib-fabric-1.21.11-5.4.6.jar` exists on Modrinth). It's heavier and has its own format; only worth it if you want complex animated models.

**Render hooks:**
- **Hats and back items:** add a feature renderer to the player renderer (Fabric API `LivingEntityFeatureRendererRegistrationCallback`). Attach to the head or body model part so the item follows head turning, sneaking and emote poses.
  - Rendering changed a lot in 1.21.2 (render states) and again around 1.21.9 (submit/command-queue rendering). Exact 1.21.11 signatures are **(unverified)**; copy them from Fabric's 1.21.11 example mods.
- **Capes:** don't draw these yourself. Swap the cape texture in the player's skin textures with a small mixin. Vanilla then draws the cape with correct physics, and also on elytra.
- **Wings and auras (later):** wings are a body-attached feature with procedural flapping. Auras are client-side particles around the player, throttled.

**Interactions:**
- **Armour:** hats draw over helmets. Each item gets a flag `hideWithHelmet`.
- **Invisibility:** don't draw on invisible players or in spectator mode.
- **First person:** features don't draw for the local player in first person. In F5 they do.
- **Iris shaders:** use vanilla render layers (`entityCutoutNoCull` / `entityTranslucent`) so shader packs treat cosmetics like any entity. Avoid custom shaders and emissive tricks in the MVP. Iris compatibility is **(unverified until tested)**.
- **Sodium / MoreCulling:** they don't touch entity feature rendering much. Test anyway.

**Performance:**
- Quads are pre-baked per item.
- Skip cosmetics beyond 32 blocks and limit how many players get full detail.
- Load textures lazily.
- Settings option **"Show other players' cosmetics"**, so PvP players can turn them off.

---

## 4. Emotes

**Animation engine: Player Animation Library (PAL, MIT licence)**, `PlayerAnimationLibFabric-1.1.12+mc.1.21.11.jar`.
- The mod registers its own layer at priority 1000 ("cosmetic emotes") in client init:
  `PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(id, 1000, ...)`
- It plays an emote on any player, local or remote, with:
  `PlayerAnimationAccess.getPlayerAnimationLayer(player, id)` → `controller.triggerAnimation(animId)`
- PAL loads Blockbench/Blender JSON animations, including the older PlayerAnimator/Emotecraft format. So emotes made with Emotecraft's Blockbench rig work. The *files* belong to their authors (see §7).
- Animations normally load from `assets/<ns>/player_animations`.
  - Loading downloaded files at runtime is **(unverified)**. Emotecraft does it, so the API exists. Otherwise, register the downloaded files as a generated client resource pack.

**Two ways to do it. Choose one (decision 1):**

- **E1, own system (recommended):** PAL plus our own emote wheel.
  - Clients send `{ emote: "<id>", t: <server-clock ms> }` over the WebSocket.
  - The service only forwards ids the sender **owns**, rate-limited to about 1 per second.
  - Receivers download the animation from the CDN by id (once, then cached) and play it, seeking forward by the delay.
  - Pros:
    - Ownership works, so emotes can be sold or earned.
    - No arbitrary data passes through the relay.
    - One system with cosmetics.
- **E2, ship Emotecraft unchanged + write an Emotecraft proxy in newgen-client.**
  - It uses the official proxy API (`io.github.kosmx.emotes:emotesAPI`, CC-BY-4.0; template is CC0) and relays over our WebSocket.
  - Pros: fastest emote MVP. You get Emotecraft's wheel, menu and formats for free.
  - Cons:
    - Every player can play *any* emote file they drop in their `emotes` folder, so emotes can't be sold.
    - Full emote data (up to 1 MB) goes through our relay.
    - Emotecraft (GPL-3.0) must be shipped as its own unmodified jar. Its API must not be bundled.
  - Don't just ship "Online Emotes" with the public Redlance relay: it sends players' names, UUIDs and server IPs to a third party, with no auth.

**Wheel and keys (E1):**
- Hold a key to open the wheel (8 slots), release on an emote.
- Default key **G**. B is Emotecraft's key; G is Simple Voice Chat's group key, so pick a free one and check in game **(unverified)**.
- Moving, jumping or getting hit stops the emote and sends `stop`.
- Receivers also stop it if the player moves more than about 0.3 blocks.
- Late joiners get the active looping emotes (with their start time) in the room snapshot.

**Recommendation:** E1. If you want emotes in players' hands within a week, ship E2 first and replace it with E1 later. Don't ship both: that gives two wheels.

---

## 5. Content pipeline and distribution

- **Catalog:** `GET /cosmetics/catalog` returns a versioned JSON (ETag) with entries like:
  `{ id, type: hat|back|cape|wings|aura|emote, name, rarity, model: url+sha1, texture: url+sha1, anim: url+sha1, flags }`
  - Assets live in object storage behind a CDN (for example Cloudflare R2).
  - **A new cosmetic means:** upload the files, add a catalog entry, done. No launcher or mod update.
  - The mod only needs updating for *new kinds* of cosmetic (for example the first aura).
- **Cache:** `%APPDATA%\.myserver-launcher\newgen-cosmetics\cache\<sha1>`. This is outside `mods/`, so the launcher's mod check doesn't touch it.
- **Bundled fallback:** ship 3–4 starter items in the mod jar so something shows even offline.
- **Ownership:** a table `owned(account, itemId, source, grantedAt)`. Items are granted by:
  - Admin command or API (`POST /admin/grant`, shared secret). Your server's battle-pass, VIP and crate plugins call it, so the in-game Cosmetics, Battle Pass and VIP menus keep working.
  - The website shop, later.
- **Equipping:** `PUT /cosmetics/me/equipped { hat, back, cape, emotes[8] }`. The service checks ownership, then pushes `equip` to the player's room.
  - **MVP: in-game wardrobe.** A Fabric screen (keybind or button) with vanilla's GUI entity preview shows the player with the items on. This is the easiest true 3D preview.
  - **Later: launcher Wardrobe screen** (Rust/Freya). The launcher already renders `.bbmodel` for the weapon cards, but it would also need a skinned player model, which is extra work. It uses the same API.

---

## 6. MVP vs later, effort, who builds what

**MVP (worth shipping):**
1. Token hand-off (launcher → mod).
2. Service with auth, catalog, ownership, equipping and WebSocket rooms.
3. Hats, back items and capes.
4. 6 emotes on a wheel (E1, or E2 as a stopgap).
5. In-game wardrobe.
6. "Hide others' cosmetics" toggle.
7. Grants via admin API.
8. Starter content: 5 hats, 2 back items, 3 capes, 6 emotes.

| Piece | Rough effort | Who |
|---|---|---|
| Launcher: request `cosmeticsToken`, write `config/newgen-cosmetics.json` | ~0.5 day | **Main Developer** (Rust) |
| Cosmetics service: auth exchange, catalog, ownership, equip, WebSocket rooms, admin grant. Rust (axum + tokio) fits the Main Developer, or extend whatever serves `/launcher/login` | ~1–1.5 weeks | **Main Developer**, or the backend owner |
| Fabric mod: token/WebSocket client, `.bbmodel` loader + renderer, cape mixin, PAL emotes + wheel, wardrobe screen, settings | ~3–5 weeks | **New hire needed: Java/Fabric mod developer.** Nobody on the team writes Fabric/Java today. HR decides (contract would fit) |
| Content: Blockbench models, textures, emote animations | ~1–2 weeks for the starter set, then ongoing | **3D/Blockbench artist** (contract or commissions), or buy licensed packs |
| Test pass: 2+ clients, Iris on/off, offline server, service down | ~2 days | **QA Engineer** (the contract role) |

**Later:**
- Wings and auras, animated models.
- Launcher Wardrobe with 3D preview.
- Shop and payments.
- Emote sounds.
- Optional plugin-message fast path on newgenmc.net.
- Moderation tools (revoke an item, mute someone's emotes).

---

## 7. Risks

- **Licences:**
  - PAL is MIT: fine to depend on and ship.
  - Emotecraft is GPL-3.0. Only ship it as an unmodified, separate jar (E2). **Don't copy its code into newgen-client**, or newgen-client becomes GPL.
  - EmotesAPI is CC-BY-4.0: credit it.
  - Online Emotes is OSL-3.0: don't fork its relay into a closed service.
  - Community emote files belong to their authors. Many copy Fortnite or other copyrighted dances and music, so ship only originals or emotes you have permission for.
- **Mojang EULA / Usage Guidelines:**
  - Selling cosmetics that give no gameplay advantage is the accepted model; Lunar and Badlion do it client-side.
  - Re-read the current Minecraft Usage Guidelines (minecraft.net/usage-guidelines) before charging money **(not re-checked for this doc)**.
  - Don't use Mojang or Microsoft branding on items.
- **Cheating and abuse:**
  - The client can be modified, so **every rule lives in the service**: ownership, rate limits, account = name, size limits.
  - Large wings can hide players in PvP. Keep sizes in check, and keep the hide toggle.
  - A stolen token gives cosmetics access only, not the game account. Keep it short-lived.
- **Privacy:** the service learns which server (hashed) each player is on. Say so in the launcher's privacy text.
- **Hosting:**
  - One small VPS can hold thousands of idle WebSockets, and a CDN with low or no egress fees serves the assets. The cost should be small, but that's an estimate, not a quote **(unverified)**.
  - If it's down, the game still works.
- **Minecraft updates:** player rendering APIs change almost every minor version (1.21.2, 1.21.9...). Keep the renderer in one small module and expect a port with each MC update. PAL also needs a matching build.
- **Mod source unknown:** this was designed without the `client-mod` branch (not in the local repo). Check where its token-reading and client-init code live before starting.

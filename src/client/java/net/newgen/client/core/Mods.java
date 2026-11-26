package net.newgen.client.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The list of all mods, and loading/saving them to config/newgen_client.json. */
public final class Mods {
	public static final List<Mod> ALL = new ArrayList<>();

	private static final Logger LOG = LoggerFactory.getLogger("newgen_client");
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("newgen_client.json");

	private Mods() {
	}

	public static void register(String category, Mod mod) {
		mod.category = category;
		ALL.add(mod);
	}

	public static void register(Mod mod) {
		ALL.add(mod);
	}

	/** Every HUD element, on or off (the HUD mods and the indicators of mods). */
	public static List<HudMod> allHuds() {
		List<HudMod> out = new ArrayList<>();
		for (Mod mod : ALL) {
			if (mod instanceof HudMod hud) out.add(hud);
			if (mod.hud != null) out.add(mod.hud);
		}
		return out;
	}

	/** Every HUD element that is drawn now: the enabled HUD mods and the indicators of enabled mods. */
	public static List<HudMod> huds() {
		List<HudMod> out = new ArrayList<>();
		for (Mod mod : ALL) {
			if (!mod.enabled) continue;
			if (mod instanceof HudMod hud) out.add(hud);
			if (mod.hud != null && mod.showHud.value) out.add(mod.hud);
		}
		return out;
	}

	/** A fresh profile (no config file yet) starts with four HUD elements stacked on the left; saved layouts are never touched. */
	private static void firstRun() {
		float[][] place = {{0f, 0f}, {0f, 0.09f}, {0f, 0.2f}, {0f, 0.75f}};
		String[] ids = {"fps", "cps", "coordinates", "keystrokes"};
		for (int i = 0; i < ids.length; i++) {
			for (Mod mod : ALL) {
				if (!mod.id.equals(ids[i]) || !(mod instanceof HudMod hud)) continue;
				hud.enabled = true;
				hud.x = place[i][0];
				hud.y = place[i][1];
			}
		}
	}

	// ---- menu state kept with the config (under "_menu"; older versions ignore it, and without it nothing is lost) ----

	private static final String META = "_menu";
	/** Starred mods (the Favourites tab) and mods put away (the Hidden tab; they leave every other tab). */
	public static final java.util.Set<String> favourites = new java.util.LinkedHashSet<>(), hidden = new java.util.LinkedHashSet<>();
	/** Mods whose options were opened at least once: the rest carry a "New" tag. */
	public static final java.util.Set<String> seen = new java.util.HashSet<>();
	/** When each mod was last changed in the menu (the "Recently changed" sort). */
	public static final java.util.Map<String, Long> changedAt = new java.util.HashMap<>();
	/** The player's own card order (the "Custom" sort); mods not in it follow in registration order. */
	public static final List<String> order = new ArrayList<>();
	/** A key per mod that turns it on or off in game (Keybinds page); Setting.Key codes. */
	public static final java.util.Map<String, Integer> toggleKeys = new java.util.HashMap<>();
	private static final java.util.Map<String, Boolean> toggleHeld = new java.util.HashMap<>();
	private static final java.util.Map<String, Setting.Key> toggleProbes = new java.util.HashMap<>();

	public static void touch(Mod mod) {
		changedAt.put(mod.id, System.currentTimeMillis());
	}

	/** Position of a mod in the custom order. */
	public static int orderOf(Mod mod) {
		int i = order.indexOf(mod.id);
		return i >= 0 ? i : order.size() + ALL.indexOf(mod);
	}

	/** Puts a mod where another one is in the custom order. */
	public static void moveTo(Mod mod, Mod target) {
		List<Mod> sorted = new ArrayList<>(ALL);
		sorted.sort(java.util.Comparator.comparingInt(Mods::orderOf));
		int to = sorted.indexOf(target);
		sorted.remove(mod);
		sorted.add(Math.max(0, Math.min(sorted.size(), to)), mod);
		order.clear();
		for (Mod m : sorted) order.add(m.id);
	}

	/** Each tick: the per-mod toggle keys flip their mod (only in game, never while a screen is open). */
	public static void tickToggleKeys(net.minecraft.client.Minecraft mc) {
		if (mc.player == null) return;
		if (mc.screen != null) {
			// Typing in chat, a sign or an anvil never flips a mod. Keys held when the screen closes count as already
			// held, so closing chat with the key still down does not fire it.
			for (Mod mod : ALL) if (toggleKeys.containsKey(mod.id)) toggleHeld.put(mod.id, true);
			return;
		}
		for (Mod mod : ALL) {
			Integer code = toggleKeys.get(mod.id);
			if (code == null || code == -1) continue;
			Setting.Key probe = toggleProbes.computeIfAbsent(mod.id, id -> new Setting.Key("toggle", "", -1));
			probe.value = code;
			boolean down = probe.isDown(mc), was = toggleHeld.getOrDefault(mod.id, false);
			toggleHeld.put(mod.id, down);
			if (!down || was) continue;
			mod.enabled = !mod.enabled;
			touch(mod);
			save();
			mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.translatable(
					mod.enabled ? "newgen_client.toast.turned_on" : "newgen_client.toast.turned_off", mod.name), false);
		}
	}

	private static void readMeta(JsonObject m) {
		if (m == null) {
			// A config from before this menu (or none): every mod there is counts as seen, so only later additions say "New".
			for (Mod mod : ALL) seen.add(mod.id);
			return;
		}
		try {
			for (var e : m.getAsJsonArray("favourites")) favourites.add(e.getAsString());
			for (var e : m.getAsJsonArray("hidden")) hidden.add(e.getAsString());
			for (var e : m.getAsJsonArray("seen")) seen.add(e.getAsString());
			for (var e : m.getAsJsonArray("order")) order.add(e.getAsString());
			for (var e : m.getAsJsonObject("changed").entrySet()) changedAt.put(e.getKey(), e.getValue().getAsLong());
			for (var e : m.getAsJsonObject("toggle_keys").entrySet()) toggleKeys.put(e.getKey(), e.getValue().getAsInt());
		} catch (Exception e) {
			LOG.warn("Bad menu state in {}, keeping what was read", FILE);
		}
	}

	private static JsonObject writeMeta() {
		JsonObject m = new JsonObject();
		m.add("favourites", array(favourites));
		m.add("hidden", array(hidden));
		m.add("seen", array(seen));
		m.add("order", array(order));
		JsonObject changed = new JsonObject();
		changedAt.forEach(changed::addProperty);
		m.add("changed", changed);
		JsonObject keys = new JsonObject();
		toggleKeys.forEach(keys::addProperty);
		m.add("toggle_keys", keys);
		return m;
	}

	private static com.google.gson.JsonArray array(java.util.Collection<String> values) {
		com.google.gson.JsonArray a = new com.google.gson.JsonArray();
		values.forEach(a::add);
		return a;
	}

	private static java.util.Map<Mod, Boolean> defaultEnabled;

	/**
	 * Loads another saved state over the running one (a profile switch): every mod back to its defaults first, so
	 * anything the state does not mention is the default, then the file as on start-up.
	 */
	public static void reload() {
		for (Mod mod : ALL) {
			mod.reset();
			mod.enabled = defaultEnabled.getOrDefault(mod, false);
			if (mod instanceof HudMod hud) {
				hud.locked = false;
				hud.group = "";
			}
			if (mod.hud != null) {
				mod.hud.locked = false;
				mod.hud.group = "";
			}
		}
		favourites.clear();
		hidden.clear();
		seen.clear();
		changedAt.clear();
		order.clear();
		toggleKeys.clear();
		load();
	}

	/** The config file of the running state (profiles copy it in and out). */
	public static Path file() {
		return FILE;
	}

	public static void load() {
		if (defaultEnabled == null) {
			// Defaults are taken once, before any file is read; a reload must not take the loaded values as defaults.
			defaultEnabled = new java.util.HashMap<>();
			for (Mod mod : ALL) {
				defaultEnabled.put(mod, mod.enabled);
				for (Setting s : mod.settings) s.def = s.toJson();
			}
		}
		if (!Files.exists(FILE)) {
			firstRun();
			readMeta(null);
			save(); // written now, so the preset applies once; if the folder is not writable it is simply the default each launch
			return;
		}
		JsonObject root;
		try {
			root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
		} catch (Exception e) {
			LOG.warn("Could not read {}, using defaults", FILE, e);
			readMeta(null);
			return;
		}
		for (Mod mod : ALL) {
			// A broken or missing entry only resets that one mod or value, never the whole file.
			try {
				JsonObject o = root.getAsJsonObject(mod.id);
				if (o != null) read(mod, o);
			} catch (Exception e) {
				LOG.warn("Bad config entry for {}, using defaults", mod.id);
			}
		}
		readMeta(root.has(META) && root.get(META).isJsonObject() ? root.getAsJsonObject(META) : null);
	}

	/** One mod's saved entry back into the mod (loading, and undo in the menu). */
	public static void read(Mod mod, JsonObject o) {
		if (o.has("enabled")) mod.enabled = o.get("enabled").getAsBoolean();
		if (mod instanceof HudMod hud) {
			if (o.has("x")) hud.x = clamp01(o.get("x").getAsFloat());
			if (o.has("y")) hud.y = clamp01(o.get("y").getAsFloat());
			if (o.has("locked")) hud.locked = o.get("locked").getAsBoolean();
			if (o.has("group")) hud.group = o.get("group").getAsString();
		}
		if (mod.hud != null) {
			if (o.has("hud_x")) mod.hud.x = clamp01(o.get("hud_x").getAsFloat());
			if (o.has("hud_y")) mod.hud.y = clamp01(o.get("hud_y").getAsFloat());
			if (o.has("hud_locked")) mod.hud.locked = o.get("hud_locked").getAsBoolean();
			if (o.has("hud_group")) mod.hud.group = o.get("hud_group").getAsString();
		}
		JsonObject settings = o.getAsJsonObject("settings");
		if (settings == null) return;
		for (Setting s : mod.settings) {
			try {
				if (settings.has(s.id)) s.fromJson(settings.get(s.id));
			} catch (Exception e) {
				LOG.warn("Bad value for {}.{}, using default", mod.id, s.id);
			}
		}
		mod.migrate(settings);
	}

	public static void save() {
		JsonObject root = new JsonObject();
		for (Mod mod : ALL) root.add(mod.id, write(mod));
		root.add(META, writeMeta());
		writeFile(root);
	}

	/** One mod's entry as saved (saving, and the undo snapshot in the menu). */
	public static JsonObject write(Mod mod) {
		JsonObject o = new JsonObject();
		o.addProperty("enabled", mod.enabled);
		if (mod instanceof HudMod hud) {
			o.addProperty("x", hud.x);
			o.addProperty("y", hud.y);
			o.addProperty("locked", hud.locked);
			o.addProperty("group", hud.group);
		}
		if (mod.hud != null) {
			o.addProperty("hud_x", mod.hud.x);
			o.addProperty("hud_y", mod.hud.y);
			o.addProperty("hud_locked", mod.hud.locked);
			o.addProperty("hud_group", mod.hud.group);
		}
		JsonObject settings = new JsonObject();
		for (Setting s : mod.settings) settings.add(s.id, s.toJson());
		o.add("settings", settings);
		return o;
	}

	private static void writeFile(JsonObject root) {
		try {
			// Write to a temp file first so a crash mid-write cannot destroy the old config.
			Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
			Files.createDirectories(FILE.getParent());
			Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
			Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOG.error("Could not save {}", FILE, e);
		}
	}

	public static float clamp01(float v) {
		return Float.isNaN(v) ? 0 : Math.max(0, Math.min(1, v));
	}
}

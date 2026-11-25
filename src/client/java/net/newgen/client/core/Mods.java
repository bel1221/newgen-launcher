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

	public static void load() {
		for (Mod mod : ALL) for (Setting s : mod.settings) s.def = s.toJson();
		if (!Files.exists(FILE)) return;
		JsonObject root;
		try {
			root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
		} catch (Exception e) {
			LOG.warn("Could not read {}, using defaults", FILE, e);
			return;
		}
		for (Mod mod : ALL) {
			// A broken or missing entry only resets that one mod or value, never the whole file.
			try {
				JsonObject o = root.getAsJsonObject(mod.id);
				if (o == null) continue;
				if (o.has("enabled")) mod.enabled = o.get("enabled").getAsBoolean();
				if (mod instanceof HudMod hud) {
					if (o.has("x")) hud.x = clamp01(o.get("x").getAsFloat());
					if (o.has("y")) hud.y = clamp01(o.get("y").getAsFloat());
				}
				if (mod.hud != null) {
					if (o.has("hud_x")) mod.hud.x = clamp01(o.get("hud_x").getAsFloat());
					if (o.has("hud_y")) mod.hud.y = clamp01(o.get("hud_y").getAsFloat());
				}
				JsonObject settings = o.getAsJsonObject("settings");
				if (settings == null) continue;
				for (Setting s : mod.settings) {
					try {
						if (settings.has(s.id)) s.fromJson(settings.get(s.id));
					} catch (Exception e) {
						LOG.warn("Bad value for {}.{}, using default", mod.id, s.id);
					}
				}
				mod.migrate(settings);
			} catch (Exception e) {
				LOG.warn("Bad config entry for {}, using defaults", mod.id);
			}
		}
	}

	public static void save() {
		JsonObject root = new JsonObject();
		for (Mod mod : ALL) {
			JsonObject o = new JsonObject();
			o.addProperty("enabled", mod.enabled);
			if (mod instanceof HudMod hud) {
				o.addProperty("x", hud.x);
				o.addProperty("y", hud.y);
			}
			if (mod.hud != null) {
				o.addProperty("hud_x", mod.hud.x);
				o.addProperty("hud_y", mod.hud.y);
			}
			JsonObject settings = new JsonObject();
			for (Setting s : mod.settings) settings.add(s.id, s.toJson());
			o.add("settings", settings);
			root.add(mod.id, o);
		}
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

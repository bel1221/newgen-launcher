package net.newgen.client.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Profiles: whole sets of mod settings to switch between (PvP, building, recording...). The running state is always
 * config/newgen_client.json; every profile is a copy of it in config/newgen_profiles/<id>.json, plus the game's own
 * key bindings under "_vanilla_keys" (so each profile has its own keys). profiles.json lists them, which one is
 * active, and each one's name, icon and star. Without profiles.json (an older install) the running state becomes the
 * first profile, "Default": nothing is lost.
 */
public final class Profiles {
	public static final class Profile {
		public final String id;
		public String name;
		public int icon;
		public boolean favourite;

		Profile(String id, String name, int icon, boolean favourite) {
			this.id = id;
			this.name = name;
			this.icon = icon;
			this.favourite = favourite;
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger("newgen_client");
	private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("newgen_profiles");
	private static final Path INDEX = DIR.resolve("profiles.json");
	private static final String KEYS = "_vanilla_keys", MAGIC = "newgen-profile:";

	public static final List<Profile> ALL = new ArrayList<>();
	private static String active;

	private Profiles() {
	}

	public static Profile active() {
		for (Profile p : ALL) if (p.id.equals(active)) return p;
		return ALL.isEmpty() ? null : ALL.get(0);
	}

	/** Reads profiles.json; called after Mods.load(). */
	public static void load() {
		ALL.clear();
		try {
			if (Files.exists(INDEX)) {
				JsonObject o = JsonParser.parseString(Files.readString(INDEX)).getAsJsonObject();
				for (var e : o.getAsJsonArray("profiles")) {
					JsonObject p = e.getAsJsonObject();
					ALL.add(new Profile(p.get("id").getAsString(), p.get("name").getAsString(),
							p.has("icon") ? p.get("icon").getAsInt() : 0, p.has("favourite") && p.get("favourite").getAsBoolean()));
				}
				active = o.has("active") ? o.get("active").getAsString() : null;
			}
		} catch (Exception e) {
			LOG.warn("Could not read {}, starting with one profile", INDEX, e);
			ALL.clear();
		}
		if (ALL.isEmpty()) {
			ALL.add(new Profile("default", "Default", 0, false));
			active = "default";
			saveIndex();
		}
		if (active() != null) active = active().id;
	}

	private static void saveIndex() {
		JsonObject o = new JsonObject();
		o.addProperty("active", active);
		JsonArray list = new JsonArray();
		for (Profile p : ALL) {
			JsonObject j = new JsonObject();
			j.addProperty("id", p.id);
			j.addProperty("name", p.name);
			j.addProperty("icon", p.icon);
			j.addProperty("favourite", p.favourite);
			list.add(j);
		}
		o.add("profiles", list);
		write(INDEX, o);
	}

	public static void changed() {
		saveIndex();
	}

	private static void write(Path path, JsonObject o) {
		try {
			Files.createDirectories(path.getParent());
			Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
			Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(o));
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOG.error("Could not save {}", path, e);
		}
	}

	private static Path fileOf(Profile p) {
		return DIR.resolve(p.id + ".json");
	}

	/** The running state as a profile: the mods' config plus the game's key bindings. */
	private static JsonObject current() {
		Mods.save();
		JsonObject o;
		try {
			o = JsonParser.parseString(Files.readString(Mods.file())).getAsJsonObject();
		} catch (Exception e) {
			o = new JsonObject();
		}
		JsonObject keys = new JsonObject();
		for (KeyMapping km : Minecraft.getInstance().options.keyMappings) keys.addProperty(km.getName(), km.saveString());
		o.add(KEYS, keys);
		return o;
	}

	/** Profile data (its file, or for the active one the running state). */
	private static JsonObject data(Profile p) {
		if (p.id.equals(active)) return current();
		try {
			return JsonParser.parseString(Files.readString(fileOf(p))).getAsJsonObject();
		} catch (Exception e) {
			return new JsonObject();
		}
	}

	/** Switches: keeps the running state in the old profile, then loads the new one (mods and game keys). */
	public static void switchTo(Profile p) {
		if (p.id.equals(active)) return;
		Profile old = active();
		if (old != null) write(fileOf(old), current());
		JsonObject next = data(p);
		JsonObject keys = next.has(KEYS) && next.get(KEYS).isJsonObject() ? next.getAsJsonObject(KEYS) : null;
		next.remove(KEYS);
		write(Mods.file(), next);
		active = p.id;
		saveIndex();
		Mods.reload();
		if (keys != null) {
			Minecraft mc = Minecraft.getInstance();
			for (KeyMapping km : mc.options.keyMappings) {
				if (!keys.has(km.getName())) continue;
				try {
					km.setKey(InputConstants.getKey(keys.get(km.getName()).getAsString()));
				} catch (Exception e) {
					// an unknown key name: keep the current binding
				}
			}
			KeyMapping.resetMapping();
			mc.options.save();
		}
	}

	private static String newId(String name) {
		String base = name.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
		if (base.isEmpty()) base = "profile";
		String id = base;
		for (int n = 2; exists(id); n++) id = base + "_" + n;
		return id;
	}

	private static boolean exists(String id) {
		for (Profile p : ALL) if (p.id.equals(id)) return true;
		return false;
	}

	/** A name not used yet: "name", "name 2", ... */
	public static String freeName(String name) {
		String out = name;
		for (int n = 2; nameUsed(out); n++) out = name + " " + n;
		return out;
	}

	private static boolean nameUsed(String name) {
		for (Profile p : ALL) if (p.name.equalsIgnoreCase(name)) return true;
		return false;
	}

	private static Profile add(String name, int icon, JsonObject data) {
		Profile p = new Profile(newId(name), freeName(name), icon, false);
		write(fileOf(p), data);
		ALL.add(p);
		saveIndex();
		return p;
	}

	/** A new profile with every mod at its defaults (the game keys as they are now). */
	public static Profile create(String name) {
		JsonObject o = new JsonObject();
		o.add(KEYS, current().getAsJsonObject(KEYS));
		return add(name, 0, o);
	}

	public static Profile duplicate(Profile p) {
		return add(p.name, p.icon, data(p));
	}

	public static void delete(Profile p) {
		if (p.id.equals(active) || ALL.size() <= 1) return;
		ALL.remove(p);
		try {
			Files.deleteIfExists(fileOf(p));
		} catch (IOException e) {
			LOG.warn("Could not delete {}", fileOf(p));
		}
		saveIndex();
	}

	/** Puts a deleted profile back (Undo). */
	public static void restore(Profile p, JsonObject data, int at) {
		write(fileOf(p), data);
		ALL.add(Math.min(at, ALL.size()), p);
		saveIndex();
	}

	public static JsonObject snapshot(Profile p) {
		return data(p);
	}

	// ---- sharing ----

	private static JsonObject packed(Profile p) {
		JsonObject o = new JsonObject();
		o.addProperty("name", p.name);
		o.addProperty("icon", p.icon);
		o.add("data", data(p));
		return o;
	}

	/** A line of text to paste anywhere: the profile as base64 JSON behind a short tag. */
	public static String toText(Profile p) {
		return MAGIC + Base64.getEncoder().encodeToString(packed(p).toString().getBytes(StandardCharsets.UTF_8));
	}

	/** A pasted line back into a new profile; null when the text is not one. */
	public static Profile fromText(String text) {
		try {
			String t = text.trim();
			if (!t.startsWith(MAGIC)) return null;
			return unpack(new String(Base64.getDecoder().decode(t.substring(MAGIC.length())), StandardCharsets.UTF_8));
		} catch (Exception e) {
			return null;
		}
	}

	private static Profile unpack(String json) {
		JsonObject o = JsonParser.parseString(json).getAsJsonObject();
		JsonObject data = o.getAsJsonObject("data");
		if (data == null) return null;
		return add(o.has("name") ? o.get("name").getAsString() : "Imported", o.has("icon") ? o.get("icon").getAsInt() : 0, data);
	}

	/** The profile as a .zip holding profile.json. */
	public static void toZip(Profile p, Path zip) throws IOException {
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
			out.putNextEntry(new ZipEntry("profile.json"));
			out.write(new GsonBuilder().setPrettyPrinting().create().toJson(packed(p)).getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
	}

	/** A .zip from toZip back into a new profile; null when it has no profile.json. */
	public static Profile fromZip(Path zip) throws IOException {
		try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
			for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
				if (!e.getName().equals("profile.json")) continue;
				ByteArrayOutputStream buf = new ByteArrayOutputStream();
				byte[] b = new byte[8192];
				int total = 0;
				for (int n; (n = in.read(b)) > 0; ) {
					total += n;
					if (total > 4 << 20) throw new IOException("profile.json is too large");
					buf.write(b, 0, n);
				}
				return unpack(buf.toString(StandardCharsets.UTF_8));
			}
		}
		return null;
	}
}

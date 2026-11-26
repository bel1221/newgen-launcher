package net.newgen.client.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** One option of a mod: on/off, number in a range, colour, choice from a list, key, text. */
public abstract class Setting {
	public final String id;
	public final String name;

	protected Setting(String id, String name) {
		this.id = id;
		this.name = name;
	}

	abstract JsonElement toJson();

	abstract void fromJson(JsonElement json);

	/** The value before the config file was read (set by Mods.load); "reset to defaults" goes back to it. */
	JsonElement def;

	public void reset() {
		if (def != null) fromJson(def);
	}

	/** True when the value differs from its default (the "changed" dot and reset button in the menu). */
	public boolean changed() {
		return def != null && !def.equals(toJson());
	}

	/** The value as saved, and back: for undo. */
	public JsonElement save() {
		return toJson();
	}

	public void load(JsonElement json) {
		fromJson(json);
	}

	public static class Bool extends Setting {
		public boolean value;

		public Bool(String id, String name, boolean value) {
			super(id, name);
			this.value = value;
		}

		JsonElement toJson() {
			return new JsonPrimitive(value);
		}

		void fromJson(JsonElement json) {
			value = json.getAsBoolean();
		}
	}

	public static class Num extends Setting {
		public final double min, max, step;
		/** Shown as a box the value is typed into, instead of a slider. */
		public boolean box;
		private double value;

		public Num(String id, String name, double value, double min, double max, double step) {
			super(id, name);
			this.min = min;
			this.max = max;
			this.step = step;
			set(value);
		}

		public double get() {
			return value;
		}

		/** Snaps to the step and keeps the value inside the range. */
		public void set(double v) {
			v = min + Math.round((v - min) / step) * step;
			value = Math.max(min, Math.min(max, v));
		}

		public String text() {
			return step >= 1 ? String.valueOf((int) value) : String.format("%.1f", value);
		}

		JsonElement toJson() {
			return new JsonPrimitive(value);
		}

		void fromJson(JsonElement json) {
			set(json.getAsDouble());
		}
	}

	/** Colour as 0xAARRGGBB. */
	public static class Color extends Setting {
		public int value;

		public Color(String id, String name, int value) {
			super(id, name);
			this.value = value;
		}

		/** Chroma: the hue runs round the colour wheel over time (speed: cycles a minute), keeping value's alpha. */
		public boolean chroma;
		public double chromaSpeed = 12;

		/** The colour to draw now: value, or the running chroma hue. */
		public int current() {
			if (!chroma) return value;
			float hue = (float) ((System.currentTimeMillis() * chromaSpeed / 60000.0) % 1.0);
			return value & 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.75f, 1f) & 0xFFFFFF;
		}

		/** A plain colour saves as "#AARRGGBB" (as before); a chroma one as an object, so old configs still read. */
		JsonElement toJson() {
			JsonPrimitive hex = new JsonPrimitive(String.format("#%08X", value));
			if (!chroma) return hex;
			com.google.gson.JsonObject o = new com.google.gson.JsonObject();
			o.add("value", hex);
			o.addProperty("chroma", true);
			o.addProperty("speed", chromaSpeed);
			return o;
		}

		void fromJson(JsonElement json) {
			if (json.isJsonObject()) {
				com.google.gson.JsonObject o = json.getAsJsonObject();
				value = (int) Long.parseLong(o.get("value").getAsString().substring(1), 16);
				chroma = o.has("chroma") && o.get("chroma").getAsBoolean();
				if (o.has("speed")) chromaSpeed = o.get("speed").getAsDouble();
			} else {
				value = (int) Long.parseLong(json.getAsString().substring(1), 16);
				chroma = false;
			}
		}
	}

	/** One of a fixed list of named options. */
	public static class Choice extends Setting {
		public final String[] options;
		public String value;
		/** Shown as a row of buttons, one per option, instead of the cycler. */
		public boolean radio;

		public Choice(String id, String name, String value, String... options) {
			super(id, name);
			this.options = options;
			this.value = value;
		}

		JsonElement toJson() {
			return new JsonPrimitive(value);
		}

		void fromJson(JsonElement json) {
			String v = json.getAsString();
			for (String o : options) if (o.equals(v)) value = v;
		}
	}

	/** A keyboard key as a GLFW key code, or a mouse button as MOUSE + button (-100 = left button); -1 = not bound. */
	public static class Key extends Setting {
		public static final int MOUSE = -100;
		public int value;

		public boolean isMouse() {
			return value >= MOUSE && value < MOUSE + 8;
		}

		/** Toggle mode: a press switches the key on, the next press off (instead of on only while held). */
		public boolean toggle;
		private boolean was, latched;

		/** True while held in game (or, in toggle mode, between two presses). Never true from a press while a screen is open,
		 * so typing in chat never triggers a mod. */
		public boolean isDown(Minecraft mc) {
			boolean raw = held(mc);
			if (!toggle) return raw;
			if (raw && !was) latched = !latched;
			was = raw;
			return latched;
		}

		private boolean held(Minecraft mc) {
			if (mc.screen != null) return false;
			if (isMouse()) return GLFW.glfwGetMouseButton(mc.getWindow().handle(), value - MOUSE) == GLFW.GLFW_PRESS;
			return value > 0 && InputConstants.isKeyDown(mc.getWindow(), value);
		}

		public Key(String id, String name, int value) {
			super(id, name);
			this.value = value;
		}

		/** A held key saves as a number (as before); a toggle key as {value, toggle}, so old configs still read. */
		JsonElement toJson() {
			if (!toggle) return new JsonPrimitive(value);
			com.google.gson.JsonObject o = new com.google.gson.JsonObject();
			o.addProperty("value", value);
			o.addProperty("toggle", true);
			return o;
		}

		void fromJson(JsonElement json) {
			if (json.isJsonObject()) {
				value = json.getAsJsonObject().get("value").getAsInt();
				toggle = json.getAsJsonObject().has("toggle") && json.getAsJsonObject().get("toggle").getAsBoolean();
			} else {
				value = json.getAsInt();
				toggle = false;
			}
			latched = false;
		}
	}

	/** Free text the player types. */
	public static class Text extends Setting {
		public String value;

		public Text(String id, String name, String value) {
			super(id, name);
			this.value = value;
		}

		JsonElement toJson() {
			return new JsonPrimitive(value);
		}

		void fromJson(JsonElement json) {
			value = json.getAsString();
		}
	}

	/** Text kept in the config but not shown in the menu (the emote wheel's slots). */
	public static class Hidden extends Text {
		public Hidden(String id, String value) {
			super(id, id, value);
		}
	}

	/** A button in a mod's options that opens or does something; it stores nothing. */
	public static class Action extends Setting {
		public final String label;
		public final Runnable run;

		public Action(String id, String name, String label, Runnable run) {
			super(id, name);
			this.label = label;
			this.run = run;
		}

		JsonElement toJson() {
			return com.google.gson.JsonNull.INSTANCE;
		}

		void fromJson(JsonElement json) {
		}
	}

	// ---- setting types added in Wave 1A ----

	/** Two values in one range: a from-to slider. */
	public static class Range extends Setting {
		public final double min, max, step;
		public double lo, hi;

		public Range(String id, String name, double lo, double hi, double min, double max, double step) {
			super(id, name);
			this.min = min;
			this.max = max;
			this.step = step;
			set(lo, hi);
		}

		public void set(double a, double b) {
			lo = snap(Math.min(a, b));
			hi = snap(Math.max(a, b));
		}

		private double snap(double v) {
			v = min + Math.round((v - min) / step) * step;
			return Math.max(min, Math.min(max, v));
		}

		public String text(double v) {
			return step >= 1 ? String.valueOf((int) v) : String.format("%.1f", v);
		}

		JsonElement toJson() {
			com.google.gson.JsonArray a = new com.google.gson.JsonArray();
			a.add(lo);
			a.add(hi);
			return a;
		}

		void fromJson(JsonElement json) {
			if (json.isJsonArray() && json.getAsJsonArray().size() == 2)
				set(json.getAsJsonArray().get(0).getAsDouble(), json.getAsJsonArray().get(1).getAsDouble());
		}
	}

	/** Any number of options from a fixed list. */
	public static class Multi extends Setting {
		public final String[] options;
		public final java.util.Set<String> selected = new java.util.LinkedHashSet<>();

		public Multi(String id, String name, String[] options, String... selected) {
			super(id, name);
			this.options = options;
			this.selected.addAll(java.util.List.of(selected));
		}

		public boolean has(String option) {
			return selected.contains(option);
		}

		public void toggle(String option) {
			if (!selected.remove(option)) selected.add(option);
		}

		JsonElement toJson() {
			com.google.gson.JsonArray a = new com.google.gson.JsonArray();
			for (String o : options) if (selected.contains(o)) a.add(o);
			return a;
		}

		void fromJson(JsonElement json) {
			if (!json.isJsonArray()) return;
			selected.clear();
			for (JsonElement e : json.getAsJsonArray()) for (String o : options) if (o.equals(e.getAsString())) selected.add(o);
		}
	}

	/** An order of fixed items the player drags into place (which line comes first, ...). */
	public static class Order extends Setting {
		public final java.util.List<String> items = new java.util.ArrayList<>();
		private final java.util.List<String> known;

		public Order(String id, String name, String... items) {
			super(id, name);
			known = java.util.List.of(items);
			this.items.addAll(known);
		}

		public void move(int from, int to) {
			if (from < 0 || from >= items.size() || to < 0 || to >= items.size() || from == to) return;
			items.add(to, items.remove(from));
		}

		JsonElement toJson() {
			com.google.gson.JsonArray a = new com.google.gson.JsonArray();
			items.forEach(a::add);
			return a;
		}

		/** Keeps the saved order of items that still exist and appends new ones, so an update never loses an item. */
		void fromJson(JsonElement json) {
			if (!json.isJsonArray()) return;
			items.clear();
			for (JsonElement e : json.getAsJsonArray()) if (known.contains(e.getAsString()) && !items.contains(e.getAsString())) items.add(e.getAsString());
			for (String k : known) if (!items.contains(k)) items.add(k);
		}
	}

	/** An item or a status effect by registry id ("minecraft:golden_apple"), chosen in a searchable picker. */
	public static class Pick extends Text {
		public enum Kind { ITEM, EFFECT }

		public final Kind kind;

		public Pick(String id, String name, Kind kind, String value) {
			super(id, name, value);
			this.kind = kind;
		}
	}

	/** A file on disk, typed or chosen with the system's file dialog. */
	public static class FilePath extends Text {
		public final String[] extensions; // patterns such as "*.png"

		public FilePath(String id, String name, String value, String... extensions) {
			super(id, name, value);
			this.extensions = extensions;
		}
	}
}

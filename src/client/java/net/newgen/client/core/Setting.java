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

		JsonElement toJson() {
			return new JsonPrimitive(String.format("#%08X", value));
		}

		void fromJson(JsonElement json) {
			value = (int) Long.parseLong(json.getAsString().substring(1), 16);
		}
	}

	/** One of a fixed list of named options. */
	public static class Choice extends Setting {
		public final String[] options;
		public String value;

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

		/** True while held in game. Always false while a screen is open, so typing in chat never triggers a mod. */
		public boolean isDown(Minecraft mc) {
			if (mc.screen != null) return false;
			if (isMouse()) return GLFW.glfwGetMouseButton(mc.getWindow().handle(), value - MOUSE) == GLFW.GLFW_PRESS;
			return value > 0 && InputConstants.isKeyDown(mc.getWindow(), value);
		}

		public Key(String id, String name, int value) {
			super(id, name);
			this.value = value;
		}

		JsonElement toJson() {
			return new JsonPrimitive(value);
		}

		void fromJson(JsonElement json) {
			value = json.getAsInt();
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
}

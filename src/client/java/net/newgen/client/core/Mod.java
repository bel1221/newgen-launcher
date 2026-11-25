package net.newgen.client.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

/** A feature the player can switch on or off in the Newgen Mods menu. */
public class Mod {
	public final String id;
	public final String name;
	public final String description;
	public boolean enabled;
	/** Menu group: "HUD", "Visual", "Controls" or "Utility". Set by Mods.register. */
	public String category = "HUD";
	public final List<Setting> settings = new ArrayList<>();
	/** State indicator this mod draws on the HUD (e.g. "[Sprinting (Toggled)]"); null = none. See indicator(). */
	public HudMod hud;
	/** The "Show indicator" switch of hud. */
	public Setting.Bool showHud;

	public Mod(String id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	/**
	 * Gives the mod a small HUD element that shows its state. Call it in the constructor, after the mod's own settings.
	 * The element is moved and resized in the HUD editor like any HUD; its settings are listed with the mod's.
	 * text returns the lines to show; empty = nothing to show right now.
	 */
	protected void indicator(float x, float y, Function<Minecraft, List<String>> text) {
		showHud = bool("indicator", "Show indicator", true);
		hud = new HudMod(id, name, description, x, y, "indicator_", "Indicator ") {
			@Override
			protected List<String> lines(Minecraft mc) {
				return text.apply(mc);
			}
		};
		hud.background.value = false;
		settings.addAll(hud.settings);
	}

	/** Called after loading with the saved settings of this mod: carry over values that were stored under an old name. */
	protected void migrate(JsonObject saved) {
		if (hud != null) hud.migrate(saved);
	}

	/** Every setting back to its default; HUD elements also go back to their default place. */
	public void reset() {
		for (Setting s : settings) s.reset();
		if (hud != null) hud.reset();
	}

	// Helpers to declare settings; they show up in the menu in the order they are declared.

	protected Setting.Bool bool(String id, String name, boolean value) {
		Setting.Bool s = new Setting.Bool(id, name, value);
		settings.add(s);
		return s;
	}

	protected Setting.Num num(String id, String name, double value, double min, double max, double step) {
		Setting.Num s = new Setting.Num(id, name, value, min, max, step);
		settings.add(s);
		return s;
	}

	protected Setting.Color color(String id, String name, int value) {
		Setting.Color s = new Setting.Color(id, name, value);
		settings.add(s);
		return s;
	}

	protected Setting.Choice choice(String id, String name, String value, String... options) {
		Setting.Choice s = new Setting.Choice(id, name, value, options);
		settings.add(s);
		return s;
	}

	protected Setting.Key key(String id, String name, int value) {
		Setting.Key s = new Setting.Key(id, name, value);
		settings.add(s);
		return s;
	}

	protected Setting.Text text(String id, String name, String value) {
		Setting.Text s = new Setting.Text(id, name, value);
		settings.add(s);
		return s;
	}
}

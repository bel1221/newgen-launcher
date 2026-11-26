package net.newgen.client.mods.visual;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class NametagMod extends Mod {
	public static final NametagMod I = new NametagMod();

	public final Setting.Bool own = bool("own", "Show own name tag in third person", true);
	public final Setting.Num background = num("background", "Background opacity", 0.2, 0, 1, 0.1);
	public final Setting.Num height = num("height", "Height offset", 0, -1, 1, 0.05);
	public final Setting.Num scale = num("scale", "Name tag scale", 1, 0.5, 2, 0.05);
	public final Setting.Color textColor = color("text_color", "Text colour", 0xFFFFFFFF);
	public final Setting.Choice shadow = choice("shadow", "Text shadow", "None", "None", "Shadow");
	public final Setting.Bool customBackground = bool("custom_background", "Custom background colour", false);
	public final Setting.Color backgroundColor = color("background_color", "Background colour", 0x40000000);
	public final Setting.Bool rounded = bool("rounded", "Rounded corners", false);
	public final Setting.Bool inventory = bool("inventory", "Show own name tag in inventory", false);
	public final Setting.Bool hideF1 = bool("hide_f1", "Hide name tags while the HUD is hidden (F1)", false);

	private NametagMod() {
		super("nametag", "Nametag", "Own name tag in third person and name tag background opacity");
	}

	/** True when name tags must not be drawn at all right now. */
	public boolean hidden() {
		return enabled && hideF1.value && Minecraft.getInstance().options.hideGui;
	}

	/** The text colour for vanilla's colour c (white, or half see-through white when sneaking). */
	public int text(int c) {
		if (!enabled) return c;
		int rgb = textColor.current() & 0xFFFFFF;
		return c >>> 24 == 0xFF ? 0xFF000000 | rgb : c & 0xFF000000 | rgb;
	}

	/** The background colour for vanilla's background c (0 = none). */
	public int background(int c) {
		return enabled && c != 0 && customBackground.value ? backgroundColor.current() : c;
	}
}

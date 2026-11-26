package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class ShinyPotsMod extends Mod {
	public static final ShinyPotsMod I = new ShinyPotsMod();

	public final Setting.Bool customGlint = bool("custom_glint", "Custom glint colour (all glints)", false);
	public final Setting.Color glintColor = color("glint_color", "Glint colour", 0xFFB070FF);

	private ShinyPotsMod() {
		super("shiny_pots", "Shiny Pots", "Gives potions the enchantment glint back");
	}
}

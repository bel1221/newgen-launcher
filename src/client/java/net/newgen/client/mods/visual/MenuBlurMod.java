package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class MenuBlurMod extends Mod {
	public static final MenuBlurMod I = new MenuBlurMod();
	public final Setting.Num strength = num("strength", "Blur strength (0 = none)", 8, 0, 20, 1);

	private MenuBlurMod() {
		super("menu_blur", "Menu Blur", "Strength of the blur behind menus");
	}
}

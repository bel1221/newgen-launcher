package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class LightingMod extends Mod {
	public static final LightingMod I = new LightingMod();
	public final Setting.Num brightness = num("brightness", "Brightness boost", 10, 1, 15, 1);

	private LightingMod() {
		super("lighting", "Lighting", "Brightens dark places beyond the vanilla brightness slider");
	}
}

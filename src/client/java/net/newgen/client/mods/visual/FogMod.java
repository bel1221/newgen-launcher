package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class FogMod extends Mod {
	public static final FogMod I = new FogMod();
	public final Setting.Bool distance = bool("distance", "Remove render distance fog", true);
	public final Setting.Bool environment = bool("environment", "Remove water, lava and effect fog", false);

	private FogMod() {
		super("fog", "Fog", "Removes fog");
	}
}

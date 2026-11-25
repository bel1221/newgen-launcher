package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class HurtCamMod extends Mod {
	public static final HurtCamMod I = new HurtCamMod();
	public final Setting.Num strength = num("strength", "Shake strength (0 = off)", 0, 0, 2, 0.1);

	private HurtCamMod() {
		super("hurt_cam", "Hurt Cam", "Changes or removes the camera shake when you take damage");
	}
}

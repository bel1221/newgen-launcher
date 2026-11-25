package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class FovMod extends Mod {
	public static final FovMod I = new FovMod();
	public final Setting.Num fov = num("fov", "Field of view", 90, 30, 150, 1);
	public final Setting.Bool dynamic = bool("dynamic", "Dynamic FOV (sprint, speed, bow)", false);

	private FovMod() {
		super("fov", "FOV", "Custom field of view, optionally without sprint and speed changes");
	}
}

package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class FovMod extends Mod {
	public static final FovMod I = new FovMod();

	public final Setting.Num fov = num("fov", "Field of view", 90, 30, 150, 1);
	public final Setting.Bool dynamic = bool("dynamic", "Dynamic FOV (sprint, speed, bow)", false);
	public final Setting.Num sprint = num("sprint", "Sprint FOV factor", 1, 0, 2, 0.1);
	public final Setting.Num flying = num("flying", "Flying FOV factor", 1, 0, 2, 0.1);
	public final Setting.Num bow = num("bow", "Bow FOV factor", 1, 0, 2, 0.1);
	public final Setting.Num speed = num("speed", "Speed effect FOV factor", 1, 0, 2, 0.1);
	public final Setting.Num slowness = num("slowness", "Slowness effect FOV factor", 1, 0, 2, 0.1);
	public final Setting.Num underwater = num("underwater", "Underwater FOV factor", 1, 0, 2, 0.1);
	public final Setting.Bool lock = bool("lock", "Static FOV lock (ignore all effects)", false);

	private FovMod() {
		super("fov", "FOV", "Custom field of view, optionally without sprint and speed changes");
	}

	/** True when every FOV effect (also spyglass and water) is switched off. */
	public boolean locked() {
		return enabled && lock.value;
	}
}

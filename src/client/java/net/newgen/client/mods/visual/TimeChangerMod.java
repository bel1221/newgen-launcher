package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class TimeChangerMod extends Mod {
	public static final TimeChangerMod I = new TimeChangerMod();
	public final Setting.Num time = num("time", "Time (0 sunrise, 6000 noon, 18000 midnight)", 6000, 0, 23500, 500);

	private TimeChangerMod() {
		super("time_changer", "Time Changer", "Shows a fixed time of day on your screen only");
	}
}

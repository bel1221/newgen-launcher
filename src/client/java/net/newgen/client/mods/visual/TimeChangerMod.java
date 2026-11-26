package net.newgen.client.mods.visual;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class TimeChangerMod extends Mod {
	public static final TimeChangerMod I = new TimeChangerMod();

	public final Setting.Num time = num("time", "Time (0 sunrise, 6000 noon, 18000 midnight)", 6000, 0, 23500, 500);
	public final Setting.Bool fast = bool("fast", "Fast time", false);
	public final Setting.Num speed = num("speed", "Fast time speed", 20, 1, 200, 1);
	public final Setting.Key forward = key("forward_key", "Time forward key", -1);
	public final Setting.Key back = key("back_key", "Time back key", -1);

	/** Time added to the setting by fast time and the keys; only on your screen. */
	private double offset;
	private double lastTime = Double.NaN;

	private TimeChangerMod() {
		super("time_changer", "Time Changer", "Shows a fixed time of day on your screen only");
	}

	public void tick(Minecraft mc) {
		if (!enabled) return;
		// Moving the slider starts again from the chosen time.
		if (time.get() != lastTime) {
			offset = 0;
			lastTime = time.get();
		}
		if (fast.value) offset += speed.get();
		if (forward.isDown(mc)) offset += 100;
		if (back.isDown(mc)) offset -= 100;
		offset %= 24000;
	}

	/** The time of day to show. */
	public long dayTime() {
		return Math.floorMod((long) (time.get() + offset), 24000L);
	}
}

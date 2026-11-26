package net.newgen.client.mods.utility;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Lower FPS and volume while the game window is not focused; back to normal on focus. Hooks: FramerateLimitMixin, SoundEngineVolumeMixin. */
public class UnfocusedLimitsMod extends Mod {
	public static UnfocusedLimitsMod I;

	private final Setting.Bool limitFps = bool("limit_fps", "Limit FPS when unfocused", true);
	private final Setting.Num fps = num("unfocused_fps", "FPS when unfocused", 15, 1, 120, 1);
	private final Setting.Bool lowerVolume = bool("lower_volume", "Lower volume when unfocused", true);
	private final Setting.Num volume = num("unfocused_volume", "Volume when unfocused (%)", 0, 0, 100, 5);

	private float lastFactor = 1f;

	public UnfocusedLimitsMod() {
		super("unfocused_limits", "Unfocused Limits", "Lower FPS and mute or quieten the game while its window is in the background");
		I = this;
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private static boolean unfocused() {
		return !Minecraft.getInstance().isWindowActive();
	}

	/** The frame limit to use: vanilla's, or lower while unfocused. */
	public static int fps(int vanilla) {
		UnfocusedLimitsMod m = I;
		if (m == null || !m.enabled || !m.limitFps.value || !unfocused()) return vanilla;
		return Math.min(vanilla, (int) m.fps.get());
	}

	/** Volume multiplier for every sound right now. */
	public static float volumeFactor() {
		UnfocusedLimitsMod m = I;
		if (m == null || !m.enabled || !m.lowerVolume.value || !unfocused()) return 1f;
		return (float) m.volume.get() / 100f;
	}

	/** Sounds already playing take the new volume when focus (or a setting) changes. */
	private void tick(Minecraft mc) {
		float f = volumeFactor();
		if (f != lastFactor) {
			lastFactor = f;
			mc.getSoundManager().refreshCategoryVolume(SoundSource.MASTER);
		}
	}
}

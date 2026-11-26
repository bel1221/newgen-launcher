package net.newgen.client.mods.controls;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import net.newgen.client.mods.utility.InvLogic;

// Wave 3 utility 1
/** Mouse wheel over the hotbar: invert, stop at the ends, or switch off; optional sliding selection box.
 * Hooks: MouseHandlerHotbarMixin (scroll) and GuiHotbarMixin (selection box). */
public class HotbarTweaksMod extends Mod {
	public static HotbarTweaksMod INSTANCE;

	private final Setting.Bool invert = bool("invert", "Invert scroll", false);
	private final Setting.Bool noWrap = bool("no_wrap", "Stop at the ends", false);
	private final Setting.Bool disable = bool("disable_scroll", "Disable scrolling", false);
	private final Setting.Bool smooth = bool("smooth", "Smooth selection box", false);
	private final Setting.Num speed = num("smooth_speed", "Slide speed", 20, 5, 60, 1);

	private float shown = -1;
	private long lastFrame;

	public HotbarTweaksMod() {
		super("hotbar_tweaks", "Hotbar Tweaks", "Change how the mouse wheel moves through the hotbar");
		INSTANCE = this;
	}

	/** The slot after a wheel step, or -1 to let vanilla decide. */
	public static int next(double scroll, int current, int size) {
		HotbarTweaksMod m = INSTANCE;
		if (m == null || !m.enabled) return -1;
		if (m.disable.value) return current;
		return InvLogic.nextHotbar(current, scroll, size, m.invert.value, !m.noWrap.value);
	}

	/** The selection box's x: vanilla's x, or a sliding one. */
	public static int selectionX(int x, int selected) {
		HotbarTweaksMod m = INSTANCE;
		long now = System.nanoTime();
		if (m == null || !m.enabled || !m.smooth.value) {
			if (m != null) m.shown = -1;
			return x;
		}
		float dt = m.lastFrame == 0 ? 0 : Math.min(0.1f, (now - m.lastFrame) / 1e9f);
		m.lastFrame = now;
		if (m.shown < 0) m.shown = selected;
		m.shown += (selected - m.shown) * Math.min(1f, dt * (float) m.speed.get());
		if (Math.abs(selected - m.shown) < 0.01f) m.shown = selected;
		return x + Math.round((m.shown - selected) * 20);
	}
}

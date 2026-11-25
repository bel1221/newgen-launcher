package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

public class StopwatchMod extends HudMod {
	private final Setting.Key toggle = key("toggle", "Start / stop key", GLFW.GLFW_KEY_LEFT_BRACKET);
	private final Setting.Key reset = key("reset", "Reset key", GLFW.GLFW_KEY_RIGHT_BRACKET);

	private long total;
	private long startedAt = -1; // -1 = stopped
	private boolean toggleWasDown, resetWasDown;

	public StopwatchMod() {
		super("stopwatch", "Stopwatch", "Stopwatch you start, stop and reset with keys", 0.5f, 0.1f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		long now = System.currentTimeMillis();
		// Keys read as released while a screen is open, so typing in chat does not start the stopwatch.
		boolean toggleDown = toggle.isDown(mc), resetDown = reset.isDown(mc);
		if (toggleDown && !toggleWasDown) {
			if (startedAt < 0) {
				startedAt = now;
			} else {
				total += now - startedAt;
				startedAt = -1;
			}
		}
		if (resetDown && !resetWasDown) {
			total = 0;
			startedAt = -1;
		}
		toggleWasDown = toggleDown;
		resetWasDown = resetDown;

		long ms = total + (startedAt < 0 ? 0 : now - startedAt);
		return List.of(String.format("%02d:%02d.%02d", ms / 60000, ms / 1000 % 60, ms / 10 % 100));
	}
}

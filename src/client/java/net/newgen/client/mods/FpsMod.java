package net.newgen.client.mods;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.Setting;
import net.newgen.client.mods.hud.ValueHud;

public class FpsMod extends ValueHud {
	private final Setting.Bool label = bool("label", "Show \"FPS\" label", true);
	private final Setting.Choice mode = choice("display_mode", "Display", "Current", "Current", "Average", "Frame time");

	public FpsMod() {
		super("fps", "FPS", "Frame rate", 0f, 0f, 60, 30, 500);
	}

	@Override
	protected double sample(Minecraft mc) {
		return mc.getFps();
	}

	@Override
	protected String text(double value, Minecraft mc) {
		return switch (mode.value) {
			case "Average" -> Math.round(average()) + (label.value ? " FPS (avg)" : "");
			case "Frame time" -> String.format("%.1f", 1000.0 / Math.max(1, value)) + " ms";
			default -> (int) value + (label.value ? " FPS" : "");
		};
	}
}

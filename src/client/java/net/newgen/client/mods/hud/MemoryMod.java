package net.newgen.client.mods.hud;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.Setting;

public class MemoryMod extends ValueHud {
	private final Setting.Bool percentOnly = bool("percent_only", "Percent only", false);
	private final Setting.Choice mode = choice("display_mode", "Display", "Used / max", "Used / max", "Percent", "Used", "Free");

	public MemoryMod() {
		super("memory", "Memory", "Memory the game uses", 0f, 0.27f, 60, 85, 100);
	}

	/** The value graded and shown in spikes is the percent used. */
	@Override
	protected double sample(Minecraft mc) {
		Runtime rt = Runtime.getRuntime();
		return (rt.totalMemory() - rt.freeMemory()) * 100.0 / rt.maxMemory();
	}

	@Override
	protected String number(double v) {
		return Math.round(v) + "%";
	}

	@Override
	protected String text(double value, Minecraft mc) {
		Runtime rt = Runtime.getRuntime();
		long used = rt.totalMemory() - rt.freeMemory(), max = rt.maxMemory();
		String percent = used * 100 / max + "%";
		String m = percentOnly.value ? "Percent" : mode.value;
		return switch (m) {
			case "Percent" -> "Mem: " + percent;
			case "Used" -> "Mem: " + (used >> 20) + " MB";
			case "Free" -> "Free: " + ((max - used) >> 20) + " MB";
			default -> "Mem: " + (used >> 20) + " / " + (max >> 20) + " MB (" + percent + ")";
		};
	}
}

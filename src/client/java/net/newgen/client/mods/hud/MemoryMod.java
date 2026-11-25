package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class MemoryMod extends HudMod {
	private final Setting.Bool percentOnly = bool("percent_only", "Percent only", false);

	public MemoryMod() {
		super("memory", "Memory", "Memory the game uses", 0f, 0.27f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		Runtime rt = Runtime.getRuntime();
		long used = rt.totalMemory() - rt.freeMemory(), max = rt.maxMemory();
		String percent = used * 100 / max + "%";
		if (percentOnly.value) return List.of("Mem: " + percent);
		return List.of("Mem: " + (used >> 20) + " / " + (max >> 20) + " MB (" + percent + ")");
	}
}

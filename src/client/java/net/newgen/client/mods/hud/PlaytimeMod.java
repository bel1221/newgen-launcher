package net.newgen.client.mods.hud;

import java.lang.management.ManagementFactory;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;

public class PlaytimeMod extends HudMod {
	private static final long GAME_START = ManagementFactory.getRuntimeMXBean().getStartTime();

	public PlaytimeMod() {
		super("playtime", "Playtime", "Time since you started the game", 1f, 0.1f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		long s = (System.currentTimeMillis() - GAME_START) / 1000;
		return List.of(String.format("%dh %02dm %02ds", s / 3600, s / 60 % 60, s % 60));
	}
}

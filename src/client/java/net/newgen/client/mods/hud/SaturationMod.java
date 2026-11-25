package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;

public class SaturationMod extends HudMod {
	public SaturationMod() {
		super("saturation", "Saturation", "Your hidden food saturation", 0.7f, 0.9f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null) return List.of();
		return List.of(String.format("Saturation: %.1f", mc.player.getFoodData().getSaturationLevel()));
	}
}

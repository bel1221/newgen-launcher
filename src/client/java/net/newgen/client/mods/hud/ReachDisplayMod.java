package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class ReachDisplayMod extends HudMod {
	private final Setting.Num decimals = num("decimals", "Decimals", 2, 0, 3, 1);

	public ReachDisplayMod() {
		super("reach_display", "Reach Display", "Distance of your last attack", 0.5f, 0.65f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		Combat.update(mc);
		if (Combat.reach < 0) return List.of("No hit yet");
		return List.of(String.format("%." + (int) decimals.get() + "f blocks", Combat.reach));
	}
}

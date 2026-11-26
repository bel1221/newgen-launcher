package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;

public class DayCounterMod extends HudMod {
	public DayCounterMod() {
		super("day_counter", "Day Counter", "In-game day of the world", 1f, 0.05f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.level == null) return List.of();
		return List.of(net.minecraft.client.resources.language.I18n.get("newgen_client.hud.day_counter.text", mc.level.getDayTime() / 24000 + 1));
	}
}

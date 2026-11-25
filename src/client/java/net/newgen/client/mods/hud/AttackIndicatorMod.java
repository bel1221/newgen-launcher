package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class AttackIndicatorMod extends HudMod {
	private final Setting.Bool hideFull = bool("hide_full", "Hide when fully charged", true);

	public AttackIndicatorMod() {
		super("attack_indicator", "Attack Indicator", "Attack charge in percent", 0.5f, 0.56f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null) return List.of();
		int charge = Math.round(mc.player.getAttackStrengthScale(0f) * 100);
		return charge >= 100 && hideFull.value ? List.of() : List.of("Attack: " + charge + "%");
	}
}

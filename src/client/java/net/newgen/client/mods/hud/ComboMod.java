package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class ComboMod extends HudMod {
	private final Setting.Bool hideZero = bool("hide_zero", "Hide when no combo", true);
	private final Setting.Num timeout = num("timeout", "Combo ends after (seconds)", 3, 1, 10, 1);

	public ComboMod() {
		super("combo", "Combo", "Hits in a row without being hit", 0.5f, 0.6f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		Combat.comboTimeout = (long) timeout.get() * 1000;
		Combat.update(mc);
		if (Combat.combo == 0) return hideZero.value ? List.of() : List.of("No combo");
		return List.of(Combat.combo + " Combo");
	}
}

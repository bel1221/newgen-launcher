package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;

public class PvpInfoMod extends HudMod {
	public PvpInfoMod() {
		super("pvp_info", "PvP Info", "Hits dealt, hits taken and best combo; resets on world change", 0f, 0.5f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		Combat.update(mc);
		return List.of(net.minecraft.client.resources.language.I18n.get("newgen_client.hud.pvp_info.hits", Combat.hitsDealt), net.minecraft.client.resources.language.I18n.get("newgen_client.hud.pvp_info.taken", Combat.hitsTaken), net.minecraft.client.resources.language.I18n.get("newgen_client.hud.pvp_info.best", Combat.bestCombo));
	}
}

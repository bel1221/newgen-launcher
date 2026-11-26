package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class PackDisplayMod extends HudMod {
	private final Setting.Bool all = bool("all", "List all active packs", false);

	public PackDisplayMod() {
		super("pack_display", "Pack Display", "Name of your active resource pack", 0f, 1f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		List<String> names = new ArrayList<>();
		for (Pack pack : mc.getResourcePackRepository().getSelectedPacks()) {
			// Skip the game's own resources and the ones Fabric adds for mods.
			if (pack.getId().equals("vanilla") || pack.getId().startsWith("fabric")) continue;
			names.add(pack.getTitle().getString());
		}
		if (names.isEmpty()) return List.of(net.minecraft.client.resources.language.I18n.get("newgen_client.hud.pack_display.default"));
		// The last selected pack is the one on top.
		return all.value ? names.reversed() : List.of(names.getLast());
	}
}

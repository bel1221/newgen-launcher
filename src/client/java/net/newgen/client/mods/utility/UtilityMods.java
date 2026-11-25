package net.newgen.client.mods.utility;

import net.newgen.client.core.Mods;

/** Registers every mod of the "Utility" group. Add one line per mod: Mods.register(CATEGORY, new XMod()); */
public final class UtilityMods {
	public static final String CATEGORY = "Utility";

	private UtilityMods() {
	}

	public static void register() {
		Mods.register(CATEGORY, new ChatMod());
		Mods.register(CATEGORY, new InventoryMod());
		Mods.register(CATEGORY, new NickHiderMod());
		Mods.register(CATEGORY, new ScreenshotMod());
		Mods.register(CATEGORY, new ScrollableTooltipsMod());
		Mods.register(CATEGORY, new ShulkerPreviewMod());
		Mods.register(CATEGORY, new HorseStatsMod());
		Mods.register(CATEGORY, new WaypointsMod());
		Mods.register(CATEGORY, new MinimapMod());
	}
}

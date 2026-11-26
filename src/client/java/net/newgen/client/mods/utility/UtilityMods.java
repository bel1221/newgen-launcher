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
		// Wave 3 utility 2
		Mods.register(CATEGORY, new SoundVolumesMod());
		Mods.register(CATEGORY, new PackOrganizerMod());
		Mods.register(CATEGORY, new ScreenshotManagerMod());
		Mods.register(CATEGORY, new UnfocusedLimitsMod());
		Mods.register(CATEGORY, new BetterF1Mod());
		Mods.register(CATEGORY, new ConfirmQuitMod());
		Mods.register(CATEGORY, new UhcOverlayMod());
		Mods.register(CATEGORY, new PingNametagsMod());
		Mods.register(CATEGORY, new KillSoundsMod());
	}
}

package net.newgen.client.mods.hud;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Mods;

/** Registers every mod of the "HUD" group. Add one line per mod: Mods.register(CATEGORY, new XMod()); */
public final class HudMods {
	public static final String CATEGORY = "HUD";

	// Mods that change a vanilla HUD part; the mixins in mixin/hud read these.
	public static final VanillaPartMod ACTION_BAR = new VanillaPartMod("action_bar", "Action Bar",
			"Hide, scale or move the message above the hotbar", 0.5f, 1f);
	public static final VanillaPartMod BOSSBAR = new VanillaPartMod("bossbar", "Bossbar",
			"Hide, scale or move the boss bars", 0.5f, 0f);
	public static final VanillaPartMod SCOREBOARD = new VanillaPartMod("scoreboard", "Scoreboard",
			"Hide, scale or move the sidebar scoreboard", 1f, 0.5f);
	public static final VanillaPartMod TAB = new VanillaPartMod("tab", "Tab",
			"Hide, scale or move the player list", 0.5f, 0f);
	public static final VanillaPartMod TITLES = new VanillaPartMod("titles", "Titles",
			"Hide, scale or move the big titles", 0.5f, 0.5f);
	public static final Mod LOCATOR_BAR = new Mod("locator_bar", "Locator Bar",
			"Hides the locator bar, so the XP bar shows instead");
	public static final F3DisplayMod F3_DISPLAY = new F3DisplayMod();
	public static final PotionEffectsMod POTION_EFFECTS = new PotionEffectsMod();

	private HudMods() {
	}

	public static void register() {
		Combat.init();
		Mods.register(CATEGORY, ACTION_BAR);
		Mods.register(CATEGORY, new ArmorStatusMod());
		Mods.register(CATEGORY, new AttackIndicatorMod());
		Mods.register(CATEGORY, BOSSBAR);
		Mods.register(CATEGORY, new ClockMod());
		Mods.register(CATEGORY, new ComboMod());
		Mods.register(CATEGORY, new CooldownsMod());
		Mods.register(CATEGORY, new DayCounterMod());
		Mods.register(CATEGORY, new DirectionHudMod());
		Mods.register(CATEGORY, F3_DISPLAY);
		Mods.register(CATEGORY, new ItemCounterMod());
		Mods.register(CATEGORY, new ItemTrackerMod());
		Mods.register(CATEGORY, new KeystrokesMod());
		Mods.register(CATEGORY, LOCATOR_BAR);
		Mods.register(CATEGORY, new MediaPlayerMod());
		Mods.register(CATEGORY, new MemoryMod());
		Mods.register(CATEGORY, new MomentumMod());
		Mods.register(CATEGORY, new PackDisplayMod());
		Mods.register(CATEGORY, new PingMod());
		Mods.register(CATEGORY, new PlaytimeMod());
		Mods.register(CATEGORY, new PotionCounterMod());
		Mods.register(CATEGORY, POTION_EFFECTS);
		Mods.register(CATEGORY, new PvpInfoMod());
		Mods.register(CATEGORY, new ReachDisplayMod());
		Mods.register(CATEGORY, new SaturationMod());
		Mods.register(CATEGORY, SCOREBOARD);
		Mods.register(CATEGORY, new ServerAddressMod());
		Mods.register(CATEGORY, new StopwatchMod());
		Mods.register(CATEGORY, TAB);
		Mods.register(CATEGORY, TITLES);
		Mods.register(CATEGORY, new TotemCounterMod());
		Mods.register(CATEGORY, new ViewAnglesMod());
		Mods.register(CATEGORY, new WailaMod());
	}
}

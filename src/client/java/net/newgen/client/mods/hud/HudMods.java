package net.newgen.client.mods.hud;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Mods;

/** Registers every mod of the "HUD" group. Add one line per mod: Mods.register(CATEGORY, new XMod()); */
public final class HudMods {
	public static final String CATEGORY = "HUD";

	// Mods that change a vanilla HUD part; the mixins in mixin/hud read these.
	public static final FadingPartMod ACTION_BAR = new FadingPartMod("action_bar", "Action Bar",
			"Hide, scale or move the message above the hotbar", 0.5f, 1f, true);
	public static final VanillaPartMod BOSSBAR = new VanillaPartMod("bossbar", "Bossbar",
			"Hide, scale or move the boss bars", 0.5f, 0f);
	public static final ScoreboardMod SCOREBOARD = new ScoreboardMod();
	public static final TabMod TAB = new TabMod();
	public static final FadingPartMod TITLES = new FadingPartMod("titles", "Titles",
			"Hide, scale or move the big titles", 0.5f, 0.5f, false);
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
		// Wave 3 HUD 1
		Mods.register(CATEGORY, new SpeedMod());
		Mods.register(CATEGORY, new TpsMod());
		Mods.register(CATEGORY, new PlayerPreviewMod());
		Mods.register(CATEGORY, new PlayerHeadMod());
		Mods.register(CATEGORY, new BatteryMod());
		Mods.register(CATEGORY, new FacingMod());
		Mods.register(CATEGORY, new AnalogClockMod());
		Mods.register(CATEGORY, new CustomTextMod());
		Mods.register(CATEGORY, new CustomImageMod());
		Mods.register(CATEGORY, new ShapeMod());
		Mods.register(CATEGORY, new BiomeMod());
		Mods.register(CATEGORY, new BlockAboveMod());
		// Wave 3 HUD 2
		Mods.register(CATEGORY, new ChunkCounterMod());
		Mods.register(CATEGORY, new EntityCounterMod());
		Mods.register(CATEGORY, new BlockPlaceCountMod());
		Mods.register(CATEGORY, new ItemLoreMod());
		Mods.register(CATEGORY, new ShadersMod());
		Mods.register(CATEGORY, new InventoryHudMod());
		Mods.register(CATEGORY, new LightLevelMod());
		Mods.register(CATEGORY, new SeedMod());
		Mods.register(CATEGORY, new MouseStrokesMod());
		Mods.register(CATEGORY, new HotbarKeysMod());
		Mods.register(CATEGORY, new ElytraCrosshairMod());
		Mods.register(CATEGORY, new StyledSubtitlesMod());
	}
}

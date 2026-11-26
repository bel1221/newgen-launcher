package net.newgen.client.mods.hud;

import net.newgen.client.core.Setting;

// Wave 3 vanilla HUD
/** The hunger bar as a vanilla part, plus "no hunger shake": the drumsticks stop jiggling at zero saturation. */
public class HungerPartMod extends VanillaPartMod {
	public final Setting.Bool noShake = bool("no_shake", "No hunger shake", true);

	public HungerPartMod() {
		super("hunger_bar", "Hunger Bar", "Hide, scale or move the hunger bar; no shake", 0.5f, 1f);
	}

	/** The saturation the bar sees: never zero with the shake off, so it never jiggles. */
	public float saturation(float vanilla) {
		return enabled && noShake.value ? Math.max(vanilla, 1f) : vanilla;
	}
}

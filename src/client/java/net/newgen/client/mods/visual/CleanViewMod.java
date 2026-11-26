package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Clean View: hides your own effect particles, lightning bolts, the wither's armour glow and the nausea wobble. */
public class CleanViewMod extends Mod {
	public static final CleanViewMod I = new CleanViewMod();
	public final Setting.Bool ownParticles = bool("own_particles", "Hide your own effect particles", true);
	public final Setting.Bool lightning = bool("lightning", "Hide lightning bolts", false);
	public final Setting.Bool witherAura = bool("wither_aura", "Hide the wither armour glow", false);
	public final Setting.Bool nausea = bool("nausea", "No nausea or portal wobble", true);

	private CleanViewMod() {
		super("clean_view", "Clean View", "Hides visual noise: your own effect particles, lightning, wither glow, nausea");
	}
}

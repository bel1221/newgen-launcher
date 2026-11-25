package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class ParticleChangerMod extends Mod {
	public static final ParticleChangerMod I = new ParticleChangerMod();
	public final Setting.Num multiplier = num("multiplier", "Crit and sharpness particle multiplier", 2, 1, 10, 1);
	public final Setting.Bool hideBlockBreak = bool("hide_block_break", "Hide block break particles", false);

	private ParticleChangerMod() {
		super("particle_changer", "Particle Changer", "More hit particles, fewer block particles");
	}
}

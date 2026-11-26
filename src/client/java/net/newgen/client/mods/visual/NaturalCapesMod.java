package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Capes that hang naturally: never swing into the body or flip over the head, and sway less to the sides. */
public class NaturalCapesMod extends Mod {
	public static final NaturalCapesMod I = new NaturalCapesMod();
	public final Setting.Num maxSwing = num("max_swing", "Highest swing (degrees)", 80, 20, 150, 5);
	public final Setting.Num sideways = num("sideways", "Side sway (%)", 50, 0, 100, 10);

	private NaturalCapesMod() {
		super("natural_capes", "Natural Capes", "Capes no longer clip into the body or flip up; calmer side sway");
	}
}

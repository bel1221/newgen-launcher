package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class NametagMod extends Mod {
	public static final NametagMod I = new NametagMod();
	public final Setting.Bool own = bool("own", "Show own name tag in third person", true);
	public final Setting.Num background = num("background", "Background opacity", 0.2, 0, 1, 0.1);

	private NametagMod() {
		super("nametag", "Nametag", "Own name tag in third person and name tag background opacity");
	}
}

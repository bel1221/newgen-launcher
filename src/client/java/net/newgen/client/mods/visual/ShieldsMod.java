package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class ShieldsMod extends Mod {
	public static final ShieldsMod I = new ShieldsMod();
	public final Setting.Num lower = num("lower", "Lower shield by", 0.3, 0, 0.8, 0.1);

	private ShieldsMod() {
		super("shields", "Shields", "Holds the shield lower in first person so it covers less of the screen");
	}
}

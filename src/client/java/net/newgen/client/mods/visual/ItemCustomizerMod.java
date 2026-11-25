package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class ItemCustomizerMod extends Mod {
	public static final ItemCustomizerMod I = new ItemCustomizerMod();
	public final Setting.Num x = num("x", "Sideways", 0, -1, 1, 0.1);
	public final Setting.Num y = num("y", "Up / down", 0, -1, 1, 0.1);
	public final Setting.Num z = num("z", "Closer / further", 0, -1, 1, 0.1);

	private ItemCustomizerMod() {
		super("item_customizer", "Item Customizer", "Moves the items you hold in first person");
	}
}

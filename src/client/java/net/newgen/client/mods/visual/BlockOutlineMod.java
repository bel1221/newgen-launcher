package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class BlockOutlineMod extends Mod {
	public static final BlockOutlineMod I = new BlockOutlineMod();
	public final Setting.Color color = color("color", "Colour", 0xFF00AAFF);
	public final Setting.Num width = num("width", "Line width", 2, 0.5, 5, 0.5);

	private BlockOutlineMod() {
		super("block_outline", "Block Outline", "Colour and width of the outline around the block you look at");
	}
}

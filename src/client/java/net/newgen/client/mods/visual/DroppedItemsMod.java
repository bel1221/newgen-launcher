package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Dropped items: lie flat on the ground, stop spinning and bobbing, change size, one model per stack. */
public class DroppedItemsMod extends Mod {
	public static final DroppedItemsMod I = new DroppedItemsMod();
	public final Setting.Bool physics = bool("physics", "Lie flat (item physics)", true);
	public final Setting.Bool still = bool("still", "No spinning or bobbing", true);
	public final Setting.Num size = num("size", "Size", 1, 0.5, 2, 0.1);
	public final Setting.Bool single = bool("single", "One model per stack", false);

	private DroppedItemsMod() {
		super("dropped_items", "Dropped Items", "How items on the ground look: flat, still, bigger or smaller, unstacked");
	}
}

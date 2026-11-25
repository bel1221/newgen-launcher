package net.newgen.client.mods.visual;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class GuiScaleMod extends Mod {
	public static final GuiScaleMod I = new GuiScaleMod();
	public final Setting.Num scale = num("scale", "GUI scale", 2, 1, 8, 1);

	private int applied = -1;

	private GuiScaleMod() {
		super("gui_scale", "GUI Scale", "Overrides the GUI scale without changing the video setting");
	}

	public void tick(Minecraft mc) {
		int want = enabled ? (int) scale.get() : -1;
		if (want == applied) return;
		applied = want;
		mc.resizeDisplay();
	}
}

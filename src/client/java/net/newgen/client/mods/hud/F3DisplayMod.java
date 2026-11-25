package net.newgen.client.mods.hud;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

/** Restyles the text of the F3 debug screen. Applied by DebugScreenOverlayMixin. */
public class F3DisplayMod extends Mod {
	public final Setting.Bool background = bool("background", "Background", false);
	public final Setting.Bool textShadow = bool("text_shadow", "Text shadow", true);
	public final Setting.Color textColor = color("text_color", "Text colour", 0xFFE0E0E0);

	public F3DisplayMod() {
		super("f3_display", "F3 Display", "Style of the F3 debug text");
	}
}

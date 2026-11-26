package net.newgen.client.mods.hud;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.Setting;

/** Titles and action bar: a vanilla part with fade and alignment options. The hooks are in GuiMixin. */
public class FadingPartMod extends VanillaPartMod {
	public final Setting.Bool fade = bool("fade", "Fade out", true);
	/** Only the action bar has its own fade time; titles get theirs from the server. */
	public final Setting.Num fadeTime;
	public final Setting.Choice align = choice("align", "Alignment", "Center", "Left", "Center", "Right");

	public FadingPartMod(String id, String name, String description, float pivotX, float pivotY, boolean ownFadeTime) {
		super(id, name, description, pivotX, pivotY);
		fadeTime = ownFadeTime ? num("fade_time", "Fade-out time (ticks)", 20, 1, 100, 1) : null;
	}

	/** Divisor the action bar uses to turn its remaining ticks into alpha (vanilla 20). */
	public float fadeTicks(float vanilla) {
		if (!enabled) return vanilla;
		if (!fade.value) return 0.0001F; // alpha jumps straight to full and stays until the message ends
		return fadeTime != null ? (float) fadeTime.get() : vanilla;
	}

	/** Alpha for the titles: full the whole time when fading is off. */
	public int titleAlpha(int vanilla) {
		return enabled && !fade.value ? 255 : vanilla;
	}

	/**
	 * X of a line of text drawn centred on the screen middle at partScale (vanilla's own scale, e.g. 4 for titles),
	 * moved to the chosen side. width is the text width in its own units.
	 */
	public int alignX(int x, int width, float partScale) {
		if (!enabled || align.value.equals("Center")) return x;
		int half = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
		float s = partScale * (float) scale.get();
		return align.value.equals("Left") ? Math.round((4 - half) / s) : Math.round((half - 4) / s) - width;
	}
}

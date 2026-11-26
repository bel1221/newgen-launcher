package net.newgen.client.mods;

import java.util.ArrayList;
import java.util.List;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import net.newgen.client.gui.Ui;

/**
 * The menus' look and feel: colour palette (five presets, or Custom with its own colours from the colour picker),
 * an optional accent animation, interface sounds, reduced motion. Applied every client tick (and the accent every
 * frame), so changes show at once.
 */
public class ThemeMod extends Mod {
	public static final String CUSTOM = "Custom";
	private final Setting.Choice palette = choice("palette", "Palette", Ui.PALETTES[0].name(), names());
	/** Our own volume for the press, hover and open sounds (on top of the vanilla Master and Interface sliders). */
	private final Setting.Num sounds = num("ui_sounds", "Interface sounds (%)", 60, 0, 100, 5);
	/** Minecraft has no reduce-motion option, so the menus have their own: no entrance, no growing slots. */
	private final Setting.Bool reduceMotion = bool("reduce_motion", "Reduce motion", false);
	/** The accent mark can breathe (Pulse) or drift a little round its hue (Shift); never with Reduce motion. */
	private final Setting.Choice motion = choice("accent_motion", "Accent animation", "Off", "Off", "Pulse", "Shift");
	private final Setting.Num speed = num("accent_speed", "Animation speed", 1, 0.25, 4, 0.25);
	// The Custom palette: the picker's chroma on the accent cycles its hue.
	private final Setting.Color accent = color("custom_accent", "Custom accent", 0xFFFFFFFF);
	private final Setting.Color glass = color("custom_glass", "Custom panels", 0x52000000);
	private final Setting.Color buttons = color("custom_buttons", "Custom buttons", 0x38000000);

	public ThemeMod() {
		super("theme", "Theme", "Colour palette, interface sounds and motion of the NewGen menus");
		enabled = true;
	}

	private static String[] names() {
		List<String> out = new ArrayList<>();
		for (Ui.Palette p : Ui.PALETTES) out.add(p.name());
		out.add(CUSTOM);
		return out.toArray(String[]::new);
	}

	public void tick() {
		enabled = true; // the Theme tab cannot be switched off; an old saved "off" turns back on
		apply();
		Ui.soundVolume = (float) (sounds.get() / 100);
		Ui.reduceMotion = reduceMotion.value;
	}

	private void apply() {
		if (CUSTOM.equals(palette.value)) Ui.custom(accent.current(), glass.value, buttons.value);
		else Ui.use(palette.value);
	}

	/** Before every frame of a screen: the palette (a chroma accent moves smoothly) and the accent animation. */
	public void frame() {
		apply();
		if (Ui.reduceMotion || motion.value.equals("Off")) return;
		double t = System.currentTimeMillis() / 1000.0 * speed.get();
		int base = Ui.ACCENT;
		if (motion.value.equals("Pulse")) {
			// Breathes between full and 60 % strength, once every two seconds at speed 1.
			float k = (float) (0.5 + 0.5 * Math.sin(t * Math.PI));
			Ui.ACCENT = base & 0xFFFFFF | Math.round((base >>> 24) * (0.6f + 0.4f * k)) << 24;
		} else {
			float[] hsb = java.awt.Color.RGBtoHSB(base >> 16 & 0xFF, base >> 8 & 0xFF, base & 0xFF, null);
			float hue = (float) (hsb[0] + 0.06 * Math.sin(t * Math.PI / 2));
			// A white or grey accent has no hue to shift: it stays as it is.
			if (hsb[1] < 0.05f) return;
			float sat = hsb[1];
			Ui.ACCENT = base & 0xFF000000 | java.awt.Color.HSBtoRGB(hue, sat, hsb[2]) & 0xFFFFFF;
		}
	}
}

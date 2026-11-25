package net.newgen.client.mods;

import java.util.Arrays;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import net.newgen.client.gui.Ui;

/** The menus' look and feel: colour palette, interface sounds, reduced motion. Applied every client tick, so changes show at once. */
public class ThemeMod extends Mod {
	private final Setting.Choice palette = choice("palette", "Palette", Ui.PALETTES[0].name(),
			Arrays.stream(Ui.PALETTES).map(Ui.Palette::name).toArray(String[]::new));
	/** Our own volume for the press, hover and open sounds (on top of the vanilla Master and Interface sliders). */
	private final Setting.Num sounds = num("ui_sounds", "Interface sounds (%)", 60, 0, 100, 5);
	/** Minecraft has no reduce-motion option, so the menus have their own: no entrance, no growing slots. */
	private final Setting.Bool reduceMotion = bool("reduce_motion", "Reduce motion", false);

	public ThemeMod() {
		super("theme", "Theme", "Colour palette, interface sounds and motion of the NewGen menus");
		enabled = true;
	}

	public void tick() {
		Ui.use(enabled ? palette.value : Ui.PALETTES[0].name());
		Ui.soundVolume = enabled ? (float) (sounds.get() / 100) : 0.6f;
		Ui.reduceMotion = enabled && reduceMotion.value;
	}
}

package net.newgen.client.mods;

import java.util.Arrays;

import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import net.newgen.client.gui.Ui;

/** The menus' colour palette (Ui.PALETTES). Applied every client tick, so a change shows at once on every screen. */
public class ThemeMod extends Mod {
	private final Setting.Choice palette = choice("palette", "Palette", Ui.PALETTES[0].name(),
			Arrays.stream(Ui.PALETTES).map(Ui.Palette::name).toArray(String[]::new));

	public ThemeMod() {
		super("theme", "Theme", "Colour palette of the NewGen menus");
		enabled = true;
	}

	public void tick() {
		Ui.use(enabled ? palette.value : Ui.PALETTES[0].name());
	}
}

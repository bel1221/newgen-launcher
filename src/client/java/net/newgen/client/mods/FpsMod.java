package net.newgen.client.mods;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class FpsMod extends HudMod {
	private final Setting.Bool label = bool("label", "Show \"FPS\" label", true);

	public FpsMod() {
		super("fps", "FPS", "Frame rate", 0f, 0f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of(label.value ? mc.getFps() + " FPS" : String.valueOf(mc.getFps()));
	}
}

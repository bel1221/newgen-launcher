package net.newgen.client.mods.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class DamageTintMod extends Mod {
	public static final DamageTintMod I = new DamageTintMod();
	private final Setting.Num below = num("below", "Show below health %", 40, 5, 100, 5);
	private final Setting.Num intensity = num("intensity", "Intensity", 0.6, 0.1, 1, 0.1);
	private final Setting.Color color = color("color", "Colour", 0xFFFF0000);
	private final Setting.Num size = num("size", "Edge size (% of screen)", 16, 5, 50, 1);

	private DamageTintMod() {
		super("damage_tint", "Damage Tint", "Coloured screen edges when your health is low");
	}

	public void render(GuiGraphics g, Minecraft mc) {
		if (!enabled || mc.player == null || mc.options.hideGui || !mc.player.isAlive()) return;
		float health = mc.player.getHealth() / mc.player.getMaxHealth() * 100;
		float limit = (float) below.get();
		if (health >= limit) return;
		int alpha = Math.round((1 - health / limit) * (float) intensity.get() * 200);
		int rgb = color.value & 0xFFFFFF;
		int w = g.guiWidth(), h = g.guiHeight(), steps = 16, depth = Math.max(w, h) * (int) size.get() / 100;
		// The vignette is built from thin strips that fade out towards the middle of the screen.
		for (int i = 0; i < steps; i++) {
			int a = alpha * (steps - i) * (steps - i) / (steps * steps) / 2;
			if (a <= 0) continue;
			int c = a << 24 | rgb;
			int from = depth * i / steps, to = depth * (i + 1) / steps;
			g.fill(0, from, w, to, c);
			g.fill(0, h - to, w, h - from, c);
			g.fill(from, 0, to, h, c);
			g.fill(w - to, 0, w - from, h, c);
		}
	}
}

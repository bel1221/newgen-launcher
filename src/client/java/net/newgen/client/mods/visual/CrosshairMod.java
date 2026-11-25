package net.newgen.client.mods.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class CrosshairMod extends Mod {
	public static final CrosshairMod I = new CrosshairMod();
	private final Setting.Choice shape = choice("shape", "Shape", "Cross", "Cross", "Dot", "Cross + dot");
	private final Setting.Num size = num("size", "Arm length", 5, 1, 20, 1);
	private final Setting.Num gap = num("gap", "Gap", 2, 0, 10, 1);
	private final Setting.Num thickness = num("thickness", "Thickness", 1, 1, 6, 1);
	private final Setting.Color color = color("color", "Colour", 0xFFFFFFFF);
	private final Setting.Bool highlight = bool("highlight", "Other colour when aiming at a mob or player", true);
	private final Setting.Color targetColor = color("target_color", "Aiming colour", 0xFFFF4040);
	private final Setting.Bool outline = bool("outline", "Black outline", false);

	private CrosshairMod() {
		super("crosshair", "Crosshair", "Custom crosshair in place of the vanilla one");
	}

	/** Draws the crosshair; called in place of the vanilla crosshair sprite. */
	public void draw(GuiGraphics g) {
		int c = highlight.value && Minecraft.getInstance().crosshairPickEntity instanceof LivingEntity ? targetColor.value : color.value;
		int t = (int) thickness.get(), len = (int) size.get(), gp = (int) gap.get();
		int x = (g.guiWidth() - t) / 2, y = (g.guiHeight() - t) / 2;
		if (!shape.value.equals("Dot")) {
			bar(g, x - gp - len, y, x - gp, y + t, c);
			bar(g, x + t + gp, y, x + t + gp + len, y + t, c);
			bar(g, x, y - gp - len, x + t, y - gp, c);
			bar(g, x, y + t + gp, x + t, y + t + gp + len, c);
		}
		if (!shape.value.equals("Cross")) bar(g, x, y, x + t, y + t, c);
	}

	private void bar(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
		if (outline.value) g.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xFF000000);
		g.fill(x1, y1, x2, y2, c);
	}
}

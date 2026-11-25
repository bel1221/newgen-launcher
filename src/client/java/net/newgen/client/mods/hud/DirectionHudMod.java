package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class DirectionHudMod extends HudMod {
	// In the order of Minecraft's yaw: 0 = south, 90 = west.
	private static final String[] NAMES = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	private final Setting.Bool degrees = bool("degrees", "Show degrees", true);
	private final Setting.Num stripWidth = num("width", "Width", 160, 80, 320, 20);
	private final Setting.Color minorColor = color("minor_color", "Colour of NE, SE, SW, NW", 0xFFAAAAAA);

	public DirectionHudMod() {
		super("direction_hud", "Direction HUD", "Compass strip showing where you face", 0.5f, 0.02f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	@Override
	protected int[] size(Minecraft mc) {
		return mc.player == null ? null : new int[] {(int) stripWidth.get(), degrees.value ? 20 : 9};
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		float yaw = mc.player.getYRot();
		int WIDTH = (int) stripWidth.get();
		for (int i = 0; i < NAMES.length; i++) {
			float off = Mth.wrapDegrees(i * 45 - yaw);
			if (Math.abs(off) > 85) continue;
			int center = WIDTH / 2 + Math.round(off * WIDTH / 180f);
			centered(g, mc, NAMES[i], center, 0, i % 2 == 0 ? textColor.value : minorColor.value);
		}
		if (degrees.value) {
			// Compass bearing: north = 0, east = 90.
			int bearing = Math.floorMod(Math.round(yaw + 180), 360);
			centered(g, mc, bearing + "°", WIDTH / 2, 11, textColor.value);
		}
	}

	private void centered(GuiGraphics g, Minecraft mc, String text, int center, int y, int color) {
		g.drawString(mc.font, text, center - mc.font.width(text) / 2, y, color, textShadow.value);
	}
}

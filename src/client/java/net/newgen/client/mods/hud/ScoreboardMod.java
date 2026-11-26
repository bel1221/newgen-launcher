package net.newgen.client.mods.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.newgen.client.core.Setting;

/** Sidebar scoreboard: hide the score numbers, own colours, a border and the side of the screen. Hooks are in GuiMixin. */
public class ScoreboardMod extends VanillaPartMod {
	public final Setting.Bool hideNumbers = bool("hide_numbers", "Hide red score numbers", false);
	public final Setting.Bool customColors = bool("custom_colors", "Custom colours", false);
	public final Setting.Color titleColor = color("title_color", "Title background colour", 0x66000000);
	public final Setting.Color backgroundColor = color("background_color", "Background colour", 0x4D000000);
	public final Setting.Bool border = bool("border", "Border", false);
	public final Setting.Color borderColor = color("border_color", "Border colour", 0xFF000000);
	public final Setting.Num borderWidth = num("border_width", "Border width", 1, 1, 4, 1);
	public final Setting.Choice side = choice("side", "Side", "Right", "Right", "Left");

	// Box drawn this frame and last frame, in vanilla's coordinates (before our scale and move).
	private int minX, minY, maxX, maxY;
	private int lastMinX = Integer.MAX_VALUE;
	private boolean sidePushed;

	public ScoreboardMod() {
		super("scoreboard", "Scoreboard", "Hide, scale or move the sidebar scoreboard", 1f, 0.5f);
	}

	@Override
	public boolean begin(GuiGraphics g) {
		minX = minY = Integer.MAX_VALUE;
		maxX = maxY = Integer.MIN_VALUE;
		sidePushed = false;
		if (enabled && !hide.value && side.value.equals("Left") && lastMinX != Integer.MAX_VALUE) {
			// The width is only known once drawn, so this uses last frame's box; it changes rarely.
			int w = g.guiWidth();
			float s = (float) scale.get();
			g.pose().pushMatrix();
			g.pose().translate(1 - w + s * (w - lastMinX), 0);
			sidePushed = true;
		}
		boolean skip = super.begin(g);
		if (skip && sidePushed) {
			g.pose().popMatrix();
			sidePushed = false;
		}
		return skip;
	}

	@Override
	public void end(GuiGraphics g) {
		if (enabled && border.value && minX <= maxX) {
			int c = borderColor.current(), b = (int) borderWidth.get();
			g.fill(minX - b, minY - b, maxX + b, minY, c);
			g.fill(minX - b, maxY, maxX + b, maxY + b, c);
			g.fill(minX - b, minY, minX, maxY, c);
			g.fill(maxX, minY, maxX + b, maxY, c);
		}
		if (minX <= maxX) lastMinX = minX;
		super.end(g);
		if (sidePushed) g.pose().popMatrix();
		sidePushed = false;
	}

	/** Called for every background rectangle vanilla fills; returns the colour to use. */
	public int fill(int x1, int y1, int x2, int y2, int color, boolean title) {
		minX = Math.min(minX, Math.min(x1, x2));
		maxX = Math.max(maxX, Math.max(x1, x2));
		minY = Math.min(minY, Math.min(y1, y2));
		maxY = Math.max(maxY, Math.max(y1, y2));
		if (!enabled || !customColors.value) return color;
		return title ? titleColor.current() : backgroundColor.current();
	}
}

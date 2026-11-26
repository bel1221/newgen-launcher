package net.newgen.client.mods.hud;

import net.minecraft.client.gui.GuiGraphics;

/** Round shapes and slanted lines drawn with GuiGraphics fills (one fill per row), for the Analog Clock and Shape HUDs. */
public final class HudShapes {
	private HudShapes() {
	}

	/** Half the width of an ellipse with radii rx, ry at the row whose centre is dy from the middle; 0 outside. */
	static double halfWidth(double rx, double ry, double dy) {
		if (rx <= 0 || ry <= 0 || Math.abs(dy) >= ry) return 0;
		return rx * Math.sqrt(1 - dy * dy / (ry * ry));
	}

	/** A filled ellipse inside the box (x, y, w, h). */
	public static void disc(GuiGraphics g, int x, int y, int w, int h, int color) {
		double rx = w / 2.0, ry = h / 2.0, cx = x + rx;
		for (int row = 0; row < h; row++) {
			double hw = halfWidth(rx, ry, row + 0.5 - ry);
			int a = (int) Math.round(cx - hw), b = (int) Math.round(cx + hw);
			if (b > a) g.fill(a, y + row, b, y + row + 1, color);
		}
	}

	/** An elliptic ring t pixels thick inside the box (x, y, w, h); each row is filled once, so see-through colours stay even. */
	public static void ring(GuiGraphics g, int x, int y, int w, int h, int t, int color) {
		double rx = w / 2.0, ry = h / 2.0, cx = x + rx;
		for (int row = 0; row < h; row++) {
			double dy = row + 0.5 - ry;
			int a = (int) Math.round(cx - halfWidth(rx, ry, dy)), b = (int) Math.round(cx + halfWidth(rx, ry, dy));
			if (b <= a) continue;
			double inner = halfWidth(rx - t, ry - t, dy);
			int ia = (int) Math.round(cx - inner), ib = (int) Math.round(cx + inner);
			if (inner <= 0 || ib <= ia) {
				g.fill(a, y + row, b, y + row + 1, color);
			} else {
				g.fill(a, y + row, ia, y + row + 1, color);
				g.fill(ib, y + row, b, y + row + 1, color);
			}
		}
	}

	/** A straight line t pixels thick from (x0, y0) to (x1, y1), stamped as small squares. */
	public static void line(GuiGraphics g, double x0, double y0, double x1, double y1, int t, int color) {
		double len = Math.hypot(x1 - x0, y1 - y0);
		int steps = Math.max(1, (int) Math.ceil(len));
		int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;
		for (int i = 0; i <= steps; i++) {
			double f = (double) i / steps;
			int px = (int) Math.floor(x0 + (x1 - x0) * f - t / 2.0 + 0.5), py = (int) Math.floor(y0 + (y1 - y0) * f - t / 2.0 + 0.5);
			if (px == lastX && py == lastY) continue;
			g.fill(px, py, px + t, py + t, color);
			lastX = px;
			lastY = py;
		}
	}
}

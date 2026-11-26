package net.newgen.client.mods.utility;

/**
 * The chat background as one rounded panel: vanilla fills each line's strip separately; these are collected and drawn
 * once, as the box around them all with its corners cut in steps, in the strongest colour of the strips.
 * One instance is reused frame after frame: {@link #reset} starts a frame, nothing is allocated while drawing.
 */
public final class RoundedChatBackground {
	/** The chat's own fill (x1, y1, x2, y2, colour), so it works for drawing and clicking alike. */
	public interface Fill {
		void fill(int x1, int y1, int x2, int y2, int color);
	}

	private Fill fill;
	private int radius;
	private int left, top, right, bottom, color;
	private boolean any;

	public RoundedChatBackground(Fill fill, int radius) {
		reset(fill, radius);
	}

	/** A new frame: forget the strips of the last one. */
	public void reset(Fill fill, int radius) {
		this.fill = fill;
		this.radius = radius;
		left = top = Integer.MAX_VALUE;
		right = bottom = Integer.MIN_VALUE;
		color = 0;
		any = false;
	}

	/** One line's strip (coordinates may come in either order). */
	public void add(int x1, int y1, int x2, int y2, int color) {
		left = Math.min(left, Math.min(x1, x2));
		right = Math.max(right, Math.max(x1, x2));
		top = Math.min(top, Math.min(y1, y2));
		bottom = Math.max(bottom, Math.max(y1, y2));
		if (!any || (color >>> 24) > (this.color >>> 24)) this.color = color;
		any = true;
	}

	/** Draws the panel once, if any strip came in: the same rectangles as {@link #rows}, without building them. */
	public void flush() {
		if (!any) return;
		any = false;
		int r = Math.min(radius, Math.min((right - left) / 2, (bottom - top) / 2));
		if (r <= 0) {
			fill.fill(left, top, right, bottom, color);
			return;
		}
		fill.fill(left, top + r, right, bottom - r, color);
		for (int i = 0; i < r; i++) {
			int inset = inset(r, i);
			fill.fill(left + inset, top + i, right - inset, top + i + 1, color);
			fill.fill(left + inset, bottom - i - 1, right - inset, bottom - i, color);
		}
	}

	/** How far row i of a corner of radius r is cut in from the box side. */
	private static int inset(int r, int i) {
		double dy = r - i - 0.5;
		return (int) Math.round(r - Math.sqrt(r * r - dy * dy));
	}

	/** The rectangles that make up a rounded box: the middle band plus stepped rows for the corners. */
	static int[][] rows(int left, int top, int right, int bottom, int radius) {
		int r = Math.min(radius, Math.min((right - left) / 2, (bottom - top) / 2));
		if (r <= 0) return new int[][] {{left, top, right, bottom}};
		int[][] out = new int[1 + 2 * r][];
		out[0] = new int[] {left, top + r, right, bottom - r};
		for (int i = 0; i < r; i++) {
			int inset = inset(r, i);
			out[1 + 2 * i] = new int[] {left + inset, top + i, right - inset, top + i + 1};
			out[2 + 2 * i] = new int[] {left + inset, bottom - i - 1, right - inset, bottom - i};
		}
		return out;
	}
}

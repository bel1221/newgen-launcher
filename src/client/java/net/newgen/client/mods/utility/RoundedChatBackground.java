package net.newgen.client.mods.utility;

/**
 * The chat background as one rounded panel: vanilla fills each line's strip separately; these are collected and drawn
 * once, as the box around them all with its corners cut in steps, in the strongest colour of the strips.
 */
public final class RoundedChatBackground {
	/** The chat's own fill (x1, y1, x2, y2, colour), so it works for drawing and clicking alike. */
	public interface Fill {
		void fill(int x1, int y1, int x2, int y2, int color);
	}

	private final Fill fill;
	private final int radius;
	private int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE, color;
	private boolean any;

	public RoundedChatBackground(Fill fill, int radius) {
		this.fill = fill;
		this.radius = radius;
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

	/** Draws the panel once, if any strip came in. */
	public void flush() {
		if (!any) return;
		any = false;
		for (int[] row : rows(left, top, right, bottom, radius)) fill.fill(row[0], row[1], row[2], row[3], color);
	}

	/** The rectangles that make up a rounded box: the middle band plus stepped rows for the corners. */
	static int[][] rows(int left, int top, int right, int bottom, int radius) {
		int r = Math.min(radius, Math.min((right - left) / 2, (bottom - top) / 2));
		if (r <= 0) return new int[][] {{left, top, right, bottom}};
		int[][] out = new int[1 + 2 * r][];
		out[0] = new int[] {left, top + r, right, bottom - r};
		for (int i = 0; i < r; i++) {
			// inset for row i from the edge: how far the circle's edge is from the box side
			double dy = r - i - 0.5;
			int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
			out[1 + 2 * i] = new int[] {left + inset, top + i, right - inset, top + i + 1};
			out[2 + 2 * i] = new int[] {left + inset, bottom - i - 1, right - inset, bottom - i};
		}
		return out;
	}
}

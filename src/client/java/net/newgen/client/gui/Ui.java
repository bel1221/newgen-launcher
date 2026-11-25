package net.newgen.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.newgen.client.NewgenClient;

/**
 * The shared look of the NewGen screens: dark see-through glass with rounded corners, thin soft outlines
 * and the Inter font. Colour is kept for what it means: blue = active, green = on, red = off.
 */
public final class Ui {
	public static final int TEXT = 0xFFFFFFFF, SOFT = 0xFFD5D8DE, DIM = 0xFFA4A9B3, LINE = 0x30FFFFFF, HOVER = 0x1CFFFFFF;
	/** GLASS is a panel over the blurred game, CARD a lighter layer on a panel, DARK a button or typing field. */
	public static final int GLASS = 0x73000000, CARD = 0x14FFFFFF, DARK = 0x66000000;
	public static final int BLUE = 0xFF4A8EF0, GREEN = 0xFF27A35E, RED = 0xFFB3274B;
	/** The text faces: UI = Inter Regular 10, CAPS = Inter SemiBold 8 (for spaced capitals), TITLE = Inter SemiBold 11. */
	public static final int UI = 0, CAPS = 1, TITLE = 2;

	private static final Identifier LOCKUP = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/lockup.png");
	private static final Identifier ICON = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/icon.png");
	private static final int LOCKUP_W = 800, LOCKUP_H = 197, ICON_SIZE = 128;
	/** White 48x48 rounded rectangle with corner radius 16, and its outline; drawn nine-slice with a tint. */
	private static final Identifier ROUND = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/round.png");
	private static final Identifier RING = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/ring.png");

	private static final String[] FACES = {"ui_", "caps_", "title_"};
	/** Moves a face down so its capitals sit in the same 7 pixel band as the Minecraft font's. */
	private static final float[] FACE_DY = {0, -0.5f, 0.5f};
	private static final int SCALES = 6;
	private static final Style[][] STYLES = new Style[FACES.length][SCALES + 1];

	private Ui() {
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ---- text ----

	/**
	 * The style that draws text in a face. The game samples font textures without smoothing, so every face
	 * has one font file per GUI scale (font/ui_2.json = oversample 2, ...): one font pixel is one screen pixel.
	 */
	public static Style style(int face) {
		int scale = Math.max(1, Math.min(SCALES, Minecraft.getInstance().getWindow().getGuiScale()));
		Style style = STYLES[face][scale];
		if (style == null) {
			style = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath(NewgenClient.ID, FACES[face] + scale)));
			STYLES[face][scale] = style;
		}
		return style;
	}

	private static Component styled(String s, int face) {
		return Component.literal(s).withStyle(style(face));
	}

	public static int width(String s, int face) {
		return Minecraft.getInstance().font.width(styled(s, face));
	}

	/** s cut to w pixels: the end is replaced by "..", or with keepEnd the start is dropped (a typing field shows what was typed last). */
	public static String fit(String s, int w, int face, boolean keepEnd) {
		if (width(s, face) <= w) return s;
		while (!s.isEmpty() && width(keepEnd ? s : s + "..", face) > w) {
			s = keepEnd ? s.substring(s.offsetByCodePoints(0, 1)) : s.substring(0, s.offsetByCodePoints(s.length(), -1));
		}
		return keepEnd ? s : s + "..";
	}

	/** y is where the top of a capital letter goes, as with the Minecraft font. */
	public static void text(GuiGraphics g, String s, int x, int y, int color, int face) {
		g.pose().pushMatrix();
		g.pose().translate(0, FACE_DY[face]);
		g.drawString(Minecraft.getInstance().font, styled(s, face), x, y, color, false);
		g.pose().popMatrix();
	}

	public static void centered(GuiGraphics g, String s, int centerX, int y, int color, int face) {
		text(g, s, centerX - width(s, face) / 2, y, color, face);
	}

	/** "Edit HUD layout" becomes "E D I T   H U D   L A Y O U T": the spaced capitals of bars and buttons. */
	public static String spaced(String s) {
		return String.join(" ", s.toUpperCase().split(""));
	}

	// ---- shapes ----

	/** A rounded rectangle with smooth edges. r is the corner radius; r = half the height gives a pill or a circle. */
	public static void round(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
		slice(g, ROUND, x, y, w, h, r, color, true, true);
	}

	/** The thin outline of round(). */
	public static void ring(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
		slice(g, RING, x, y, w, h, r, color, true, true);
	}

	/** round() with square top corners: the bar that closes a card. */
	public static void roundBottom(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
		slice(g, ROUND, x, y, w, h, r, color, false, true);
	}

	private static void slice(GuiGraphics g, Identifier texture, int x, int y, int w, int h, int r, int color, boolean top, boolean bottom) {
		r = Math.max(1, Math.min(r, Math.min(w, h) / 2));
		int[] xs = {x, x + r, x + w - r, x + w}, ys = {y, y + r, y + h - r, y + h};
		for (int row = 0; row < 3; row++) {
			// A square end takes the middle of the texture, which has no corners.
			int v = row == 0 && !top || row == 2 && !bottom ? 16 : row * 16;
			for (int col = 0; col < 3; col++) {
				int cw = xs[col + 1] - xs[col], ch = ys[row + 1] - ys[row];
				if (cw > 0 && ch > 0) g.blit(RenderPipelines.GUI_TEXTURED, texture, xs[col], ys[row], col * 16, v, cw, ch, 16, 16, 48, 48, color);
			}
		}
	}

	public static void panel(GuiGraphics g, int x, int y, int w, int h) {
		round(g, x, y, w, h, 6, GLASS);
		ring(g, x, y, w, h, 6, LINE);
	}

	/** A rounded button. fill is DARK (glass with an outline) or a strong colour; the label is drawn as given, in face. */
	public static void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean hot, int fill, int face) {
		int r = Math.min(4, h / 2);
		round(g, x, y, w, h, r, fill);
		if (fill >>> 24 != 0xFF) ring(g, x, y, w, h, r, LINE);
		if (hot) round(g, x, y, w, h, r, HOVER);
		centered(g, fit(label, w - 4, face, false), x + w / 2, y + (h - 7) / 2, TEXT, face);
	}

	/** Height the logo gets when it is drawn w wide. */
	public static int logoHeight(int w) {
		return w * LOCKUP_H / LOCKUP_W;
	}

	/** The NewGen logo with lettering, w wide, centred on centerX. */
	public static void logo(GuiGraphics g, int centerX, int y, int w) {
		if (w >= 40) g.blit(RenderPipelines.GUI_TEXTURED, LOCKUP, centerX - w / 2, y, 0, 0, w, logoHeight(w), LOCKUP_W, LOCKUP_H, LOCKUP_W, LOCKUP_H);
	}

	/** The square NewGen icon. */
	public static void icon(GuiGraphics g, int x, int y, int size) {
		g.blit(RenderPipelines.GUI_TEXTURED, ICON, x, y, 0, 0, size, size, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
	}

	/** A vanilla button (keyboard focus, click sound, narration) that looks like button(). */
	public static class Btn extends Button {
		private final int fill, face;

		public Btn(int x, int y, int w, int h, String label, int fill, int face, Runnable action) {
			super(x, y, w, h, Component.literal(label), button -> action.run(), DEFAULT_NARRATION);
			this.fill = fill;
			this.face = face;
		}

		@Override
		protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float delta) {
			String label = getMessage().getString();
			button(g, getX(), getY(), getWidth(), getHeight(), face == CAPS ? spaced(label) : label, isHoveredOrFocused(), fill, face);
		}
	}
}

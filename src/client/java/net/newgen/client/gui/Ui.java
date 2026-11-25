package net.newgen.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.newgen.client.NewgenClient;

/**
 * The shared look of the NewGen screens, matched to the Newgen Launcher: dark see-through glass, one accent
 * (the launcher's blue #2B4BFF) for the main action, on-states and focus, one radius scale (R_SM/R_MD/R_LG =
 * 4/6/8 GUI units, i.e. 8/12/16 px at GUI scale 2, as in the launcher) and Inter in sentence case.
 * "Off" is a neutral grey, never red: off is not an error.
 */
public final class Ui {
	public static final int TEXT = 0xFFFFFFFF, SOFT = 0xFFD5D8DE, DIM = 0xFFA4A9B3, LINE = 0x30FFFFFF, HOVER = 0x1CFFFFFF;
	/** GLASS is a panel over the blurred game, CARD a lighter layer on a panel, DARK a button or typing field. */
	public static final int GLASS = 0x73000000, CARD = 0x14FFFFFF, DARK = 0x66000000;
	public static final int BLUE = 0xFF2B4BFF;
	/** The track of a switch that is off. */
	public static final int OFF = 0x40FFFFFF;
	/** The radius scale: controls, cards, panels. */
	public static final int R_SM = 4, R_MD = 6, R_LG = 8;
	/** The text faces: UI = Inter Regular 10, CAPS = Inter SemiBold 10 (labels and buttons), TITLE = Inter SemiBold 11. */
	public static final int UI = 0, CAPS = 1, TITLE = 2;

	private static final Identifier LOCKUP = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/lockup.png");
	private static final Identifier ICON = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/icon.png");
	private static final int LOCKUP_W = 800, LOCKUP_H = 197, ICON_SIZE = 128;
	/** White 48x48 rounded rectangle with corner radius 16, and its outline; drawn nine-slice with a tint. */
	private static final Identifier ROUND = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/round.png");
	private static final Identifier RING = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/ring.png");

	private static final String[] FACES = {"ui_", "caps_", "title_"};
	/** Moves a face down so its capitals sit in the same 7 pixel band as the Minecraft font's. */
	private static final float[] FACE_DY = {0, 0, 0.5f};
	private static final int SCALES = 6;
	private static final Style[][] STYLES = new Style[FACES.length][SCALES + 1];

	private Ui() {
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** A string of this mod in the game's language (lang/en_us.json, lang/vi_vn.json): key "newgen_client.<key>". */
	public static String tr(String key) {
		return I18n.get("newgen_client." + key);
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

	// ---- shapes ----

	/** A rounded rectangle with smooth edges. r is the corner radius; r = half the height gives a pill or a circle. */
	public static void round(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
		slice(g, ROUND, x, y, w, h, r, color);
	}

	/** The thin outline of round(). */
	public static void ring(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
		slice(g, RING, x, y, w, h, r, color, true);
	}

	/** round() with square top corners: the strip that closes a card. */
	public static void roundBottom(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
		slice(g, ROUND, x, y, w, h, r, color, false);
	}

	private static void slice(GuiGraphics g, Identifier texture, int x, int y, int w, int h, int r, int color) {
		slice(g, texture, x, y, w, h, r, color, true);
	}

	private static void slice(GuiGraphics g, Identifier texture, int x, int y, int w, int h, int r, int color, boolean top) {
		r = Math.max(1, Math.min(r, Math.min(w, h) / 2));
		int[] xs = {x, x + r, x + w - r, x + w}, ys = {y, y + r, y + h - r, y + h};
		for (int row = 0; row < 3; row++) {
			// Square top corners take the middle of the texture, which has none.
			int v = row == 0 && !top ? 16 : row * 16;
			for (int col = 0; col < 3; col++) {
				int cw = xs[col + 1] - xs[col], ch = ys[row + 1] - ys[row];
				if (cw > 0 && ch > 0) g.blit(RenderPipelines.GUI_TEXTURED, texture, xs[col], ys[row], col * 16, v, cw, ch, 16, 16, 48, 48, color);
			}
		}
	}

	public static void panel(GuiGraphics g, int x, int y, int w, int h) {
		round(g, x, y, w, h, R_LG, GLASS);
		ring(g, x, y, w, h, R_LG, LINE);
	}

	/** A rounded button. fill is DARK (glass with an outline) or BLUE (the one main action); the label is drawn as given. */
	public static void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean hot, int fill, int face) {
		button(g, x, y, w, h, label, hot ? 1f : 0f, fill, face);
	}

	/** button() with the hover as an amount from 0 to 1, so a widget can fade it in and out. */
	public static void button(GuiGraphics g, int x, int y, int w, int h, String label, float hot, int fill, int face) {
		button(g, x, y, w, h, label, -1, hot, fill, face);
	}

	/**
	 * A button with an optional icon (index into the icon atlas, -1 = none) left of its label, the two centred
	 * together. An empty label gives an icon-only square button.
	 */
	public static void button(GuiGraphics g, int x, int y, int w, int h, String label, int icon, float hot, int fill, int face) {
		int r = Math.min(R_SM, h / 2);
		round(g, x, y, w, h, r, fill);
		if (fill >>> 24 != 0xFF) ring(g, x, y, w, h, r, hot > 0 ? lerp(LINE, 0x66FFFFFF, hot) : LINE);
		if (hot > 0) round(g, x, y, w, h, r, (int) (0x1C * hot) << 24 | 0xFFFFFF);
		int size = Math.min(10, h - 8);
		if (icon >= 0 && label.isEmpty()) {
			glyph(g, icon, x + (w - size) / 2, y + (h - size) / 2, size, TEXT);
			return;
		}
		String text = fit(label, w - 8 - (icon >= 0 ? size + 5 : 0), face, false);
		int tw = width(text, face), left = x + (w - tw - (icon >= 0 ? size + 5 : 0)) / 2;
		if (icon >= 0) {
			glyph(g, icon, left, y + (h - size) / 2, size, TEXT);
			left += size + 5;
		}
		text(g, text, left, y + (h - 7) / 2, TEXT, face);
	}

	// ---- icons ----

	/** The icon atlas: white line icons, 64x64 each, in one row; drawn tinted at any size. */
	private static final Identifier ICONS = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/icons.png");
	public static final int I_PLAY = 0, I_USER = 1, I_USERS = 2, I_GEAR = 3, I_GRID = 4, I_CLOSE = 5, I_BACK = 6,
			I_SEARCH = 7, I_LAYOUT = 8, I_GLOBE = 9, I_EYE = 10, I_MOUSE = 11, I_SLIDERS = 12;
	private static final int ICON_COUNT = 13;

	public static void glyph(GuiGraphics g, int icon, int x, int y, int size, int color) {
		g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, icon * 64, 0, size, size, 64, 64, ICON_COUNT * 64, 64, color);
	}

	/** An on/off switch, 20x10: a blue track with the knob right when on, a neutral track with the knob left when off. */
	public static void toggle(GuiGraphics g, int x, int y, boolean on, boolean hot) {
		round(g, x, y, 20, 10, 5, on ? BLUE : OFF);
		if (hot) round(g, x, y, 20, 10, 5, HOVER);
		round(g, on ? x + 11 : x + 1, y + 1, 8, 8, 4, 0xFFF2F3F5);
	}

	/** Each 8-bit channel of a and b mixed: t = 0 gives a, t = 1 gives b. */
	public static int lerp(int a, int b, float t) {
		int out = 0;
		for (int shift = 0; shift < 32; shift += 8) {
			int ca = a >>> shift & 0xFF, cb = b >>> shift & 0xFF;
			out |= Math.round(ca + (cb - ca) * t) << shift;
		}
		return out;
	}

	/** Ease-out (quartic) progress from 0 to 1 of an animation that started at startMs and lasts ms. */
	public static float ease(long startMs, int ms) {
		float t = Math.max(0f, Math.min(1f, (System.currentTimeMillis() - startMs) / (float) ms));
		float u = 1 - t;
		return 1 - u * u * u * u;
	}

	/** Height the logo gets when it is drawn w wide. */
	public static int logoHeight(int w) {
		return w * LOCKUP_H / LOCKUP_W;
	}

	/** The NewGen logo with lettering, w wide, centred on centerX. */
	public static void logo(GuiGraphics g, int centerX, int y, int w) {
		logo(g, centerX, y, w, 0xFFFFFFFF);
	}

	/** logo() with a tint; the top byte fades it in. */
	public static void logo(GuiGraphics g, int centerX, int y, int w, int color) {
		if (w >= 40) g.blit(RenderPipelines.GUI_TEXTURED, LOCKUP, centerX - w / 2, y, 0, 0, w, logoHeight(w), LOCKUP_W, LOCKUP_H, LOCKUP_W, LOCKUP_H, color);
	}

	/** The square NewGen icon. */
	public static void icon(GuiGraphics g, int x, int y, int size) {
		g.blit(RenderPipelines.GUI_TEXTURED, ICON, x, y, 0, 0, size, size, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
	}

	/** A vanilla button (keyboard focus, click sound, narration) that looks like button() and fades its hover. */
	public static class Btn extends Button {
		private final int fill, face, icon;
		/** 0..1: in over 150 ms, out over 120 ms. */
		private float hover;
		private long last = System.currentTimeMillis();

		public Btn(int x, int y, int w, int h, String label, int fill, int face, Runnable action) {
			this(x, y, w, h, label, -1, fill, face, action);
		}

		/** With an icon left of the label; an empty label gives an icon-only button (narrated as narration). */
		public Btn(int x, int y, int w, int h, String label, int icon, int fill, int face, Runnable action) {
			super(x, y, w, h, Component.literal(label), button -> action.run(), DEFAULT_NARRATION);
			this.fill = fill;
			this.face = face;
			this.icon = icon;
		}

		/** The text a screen reader says for an icon-only button; it is never drawn. */
		public Btn narration(String text) {
			this.narration = text;
			return this;
		}

		private String narration;

		@Override
		protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
			return narration != null ? Component.literal(narration) : super.createNarrationMessage();
		}

		@Override
		protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float delta) {
			long now = System.currentTimeMillis();
			float dt = Math.min(100, now - last);
			last = now;
			hover = isHoveredOrFocused() ? Math.min(1, hover + dt / 150f) : Math.max(0, hover - dt / 120f);
			button(g, getX(), getY(), getWidth(), getHeight(), getMessage().getString(), icon, hover, fill, face);
		}
	}
}

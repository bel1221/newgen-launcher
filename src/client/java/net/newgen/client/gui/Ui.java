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
 * The shared look of the NewGen screens: clear glass over the game or the title video, one accent from the chosen
 * palette (white by default) used only as a small mark, one radius scale (R_SM/R_MD/R_LG =
 * 4/6/8 GUI units, i.e. 8/12/16 px at GUI scale 2, as in the launcher) and Inter in sentence case.
 * "Off" is a neutral grey, never red: off is not an error.
 */
public final class Ui {
	// v3.1 colour: clear glass over the NewGen footage (dark, low-saturation night scenes: mean RGB 22-78), white type with a
	// soft shadow, hairline light borders, hover = the glass brightens. The accent is only ever a small mark, never a block.
	public static final int TEXT = 0xFFFFFFFF, SOFT = 0xFFE4E6EA, DIM = 0xFFB0B5BD;
	/** LINE a hairline border, HOVER the brightening on hover, SELECTED a chosen tab, chip or row. */
	public static final int LINE = 0x2EFFFFFF, HOVER = 0x14FFFFFF, SELECTED = 0x24FFFFFF;
	public static final int CARD = 0x0DFFFFFF;
	/**
	 * The palette presets (Mods > Theme > Palette). Each is a glass tint for panels (GLASS) and buttons (DARK)
	 * and the ACCENT: a small mark only (switch on, slider, focus ring, the
	 * active-row bar, the on-dot, the HUD element being moved). Hues come from the footage; no neon, no violet.
	 */
	public record Palette(String name, int accent, int glass, int dark) {
	}

	public static final Palette[] PALETTES = {
			new Palette("Glass", 0xFFFFFFFF, 0x52000000, 0x38000000),   // clear: white is the accent (default)
			new Palette("Moss", 0xFF9CCB8F, 0x520A120B, 0x380A120B),    // the leaves and the logo's sprout
			new Palette("Lagoon", 0xFF7CC4BF, 0x52081213, 0x38081213),  // the teal glow by the water
			new Palette("Lantern", 0xFFE6B872, 0x52130D07, 0x38130D07), // lanterns and the wooden sign
			new Palette("Sakura", 0xFFE7A3B8, 0x52130A0E, 0x38130A0E)}; // the pink blossoms

	/** The current palette's colours; set by use(). Panels, buttons, the main action and its type, the accent mark. */
	public static int GLASS, DARK, ACCENT;
	/** The main action (Play NewGen) in every palette: the launcher's green Play button, with its dark type (10.7:1). */
	public static final int PRIMARY = 0xFF9CCB8F, INK = 0xFF11171C;

	static {
		use(PALETTES[0].name());
	}

	/** Switches every menu to the named palette (unknown names fall back to Glass). */
	public static void use(String name) {
		Palette p = PALETTES[0];
		for (Palette q : PALETTES) if (q.name().equals(name)) p = q;
		blendTo(p.glass(), p.dark(), p.accent());
	}

	/** The Custom palette (Theme > Palette > Custom): the player's own accent, panel and button colours. */
	public static void custom(int accent, int glass, int dark) {
		blendTo(glass, dark, accent);
	}

	private static int toGlass, toDark, toAccent, fromGlass, fromDark, fromAccent;
	private static long blendStart;

	/** Moves the palette to new colours over 250 ms (a crossfade when the palette changes; at once with Reduce motion). */
	private static void blendTo(int glass, int dark, int accent) {
		if (blendStart == 0) {
			toGlass = fromGlass = GLASS = glass;
			toDark = fromDark = DARK = dark;
			toAccent = fromAccent = ACCENT = accent;
			blendStart = 1;
			return;
		}
		if (glass != toGlass || dark != toDark || accent != toAccent) {
			fromGlass = GLASS;
			fromDark = DARK;
			fromAccent = ACCENT;
			toGlass = glass;
			toDark = dark;
			toAccent = accent;
			blendStart = System.currentTimeMillis();
		}
		float k = ease(blendStart, 250);
		GLASS = lerp(fromGlass, toGlass, k);
		DARK = lerp(fromDark, toDark, k);
		ACCENT = lerp(fromAccent, toAccent, k);
	}

	// ---- search ----

	/** Lower case without accents (đ as d), so "dong ho" finds "Đồng hồ"; the same folding as the Mods search. */
	public static String fold(String text) {
		return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
				.replace('đ', 'd').replace('Đ', 'd').toLowerCase(java.util.Locale.ROOT);
	}

	/**
	 * Text with the part matching the search drawn in the accent (folded per character, so "dong" lights "Đồng").
	 * Returns the width drawn.
	 */
	public static int highlighted(GuiGraphics g, String s, int x, int y, int color, int face, String query) {
		String q = query == null ? "" : fold(query.trim());
		int at = -1, end = -1;
		if (!q.isEmpty()) {
			StringBuilder folded = new StringBuilder();
			int[] origin = new int[s.length() + 1];
			for (int i = 0; i < s.length(); i++) {
				String f = fold(String.valueOf(s.charAt(i)));
				for (int k = 0; k < f.length(); k++) {
					if (folded.length() < origin.length) origin[folded.length()] = i;
					folded.append(f.charAt(k));
				}
			}
			int hit = folded.indexOf(q);
			if (hit >= 0 && hit + q.length() <= folded.length() && folded.length() <= s.length()) {
				at = origin[hit];
				end = hit + q.length() < folded.length() ? origin[hit + q.length()] : s.length();
			}
		}
		if (at < 0) {
			text(g, s, x, y, color, face);
			return width(s, face);
		}
		String a = s.substring(0, at), m = s.substring(at, end), b = s.substring(end);
		text(g, a, x, y, color, face);
		int mx = x + width(a, face);
		text(g, m, mx, y, ACCENT, face);
		g.fill(mx, y + 9, mx + width(m, face), y + 10, ACCENT);
		text(g, b, mx + width(m, face), y, color, face);
		return width(s, face);
	}
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

	/** tr() with values for its %s / %d places. */
	public static String trf(String key, Object... args) {
		return I18n.get("newgen_client." + key, args);
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
		// Light type gets a soft shadow half a unit down and right, so it reads over bright parts of the footage.
		if ((color >> 16 & 0xFF) > 0x80) {
			g.pose().pushMatrix();
			g.pose().translate(0.5f, 0.5f);
			g.drawString(Minecraft.getInstance().font, styled(s, face), x, y, (int) ((color >>> 24) * 0.55f) << 24, false);
			g.pose().popMatrix();
		}
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

	/** A rounded button. fill is DARK (glass with a hairline) or PRIMARY (the one main action, a white pill with dark type). */
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
		button(g, x, y, w, h, label, icon, hot, false, fill, face);
	}

	public static void button(GuiGraphics g, int x, int y, int w, int h, String label, int icon, float hot, boolean pressed, int fill, int face) {
		int r = Math.min(R_SM, h / 2);
		boolean primary = fill == PRIMARY;
		int ink = primary ? INK : TEXT;
		round(g, x, y, w, h, r, fill);
		if (!primary) ring(g, x, y, w, h, r, hot > 0 ? lerp(LINE, 0x5CFFFFFF, hot) : LINE);
		// Hover brightens the glass (the white pill goes fully opaque); colours never swap.
		if (primary && pressed) round(g, x, y, w, h, r, 0x0FFFFFFF); // pressed: 6 % lighter, as in the launcher
		else if (primary && hot > 0) round(g, x, y, w, h, r, (int) (0x21 * hot) << 24); // hover: 13 % darker
		else if (hot > 0) round(g, x, y, w, h, r, (int) (0x14 * hot) << 24 | 0xFFFFFF);
		int size = Math.min(10, h - 8);
		if (icon >= 0 && label.isEmpty()) {
			glyph(g, icon, x + (w - size) / 2, y + (h - size) / 2, size, ink);
			return;
		}
		String text = fit(label, w - 8 - (icon >= 0 ? size + 5 : 0), face, false);
		int tw = width(text, face), left = x + (w - tw - (icon >= 0 ? size + 5 : 0)) / 2;
		if (icon >= 0) {
			glyph(g, icon, left, y + (h - size) / 2, size, ink);
			left += size + 5;
		}
		text(g, text, left, y + (h - 7) / 2, ink, face);
	}

	// ---- icons ----

	/** The icon atlas: white line icons, 64x64 each, in one row; drawn tinted at any size. */
	private static final Identifier ICONS = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/icons.png");
	public static final int I_PLAY = 0, I_USER = 1, I_USERS = 2, I_GEAR = 3, I_GRID = 4, I_CLOSE = 5, I_BACK = 6,
			I_SEARCH = 7, I_LAYOUT = 8, I_GLOBE = 9, I_EYE = 10, I_MOUSE = 11, I_SLIDERS = 12, I_PALETTE = 13,
			// menu glyphs drawn by "java tools/ModIcons.java menu" (the same pen as the mod icons)
			I_STAR = 14, I_STAR_ON = 15, I_RESET = 16, I_KEYBOARD = 17, I_HIDDEN = 18, I_UNDO = 19, I_COPY = 20, I_SORT = 21,
			I_BELL = 22, I_PLUS = 23, I_MORE = 24, I_DOWNLOAD = 25, I_UPLOAD = 26, I_SOUND = 27;
	private static final int ICON_COUNT = 28;

	/** The mod icons: smooth white line icons drawn by tools/ModIcons.java on a 24 grid, 64 px tiles, 16 a row in
	 * textures/mod_icons_<px>.png (one per on-screen pixel size), ids in mod_icons.txt; the same system as the menu glyphs. */
	private static java.util.Map<String, Integer> modIconIndex;

	/** Draws a mod's icon; false (nothing drawn) when it has none, so the caller can fall back. */
	public static boolean modIcon(GuiGraphics g, String id, int x, int y, int color) {
		return modIcon(g, id, x, y, 16, color);
	}

	/** modIcon() at a given size (16 on cards, 14 in headers); the 64 px tiles are sampled smoothly. */
	public static boolean modIcon(GuiGraphics g, String id, int x, int y, int size, int color) {
		if (modIconIndex == null) {
			modIconIndex = new java.util.HashMap<>();
			try (var in = Minecraft.getInstance().getResourceManager().open(Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/mod_icons.txt"))) {
				String[] ids = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\\R");
				for (int i = 0; i < ids.length; i++) if (!ids[i].isBlank()) modIconIndex.put(ids[i].trim(), i);
			} catch (Exception e) {
				// no icons: every caller falls back
			}
		}
		Integer i = modIconIndex.get(id);
		if (i == null) return false;
		// Each size (16, 14) has an atlas per GUI scale 1-8 (the most Minecraft offers), drawn at exactly size x scale pixels: shown 1:1, never resampled.
		int scale = Math.max(1, Math.min(8, Minecraft.getInstance().getWindow().getGuiScale()));
		int unit = size >= 15 ? 16 : 14, tile = unit * scale, rows = (modIconIndex.size() + 15) / 16;
		Identifier atlas = Identifier.fromNamespaceAndPath(NewgenClient.ID, "textures/mod_icons_" + tile + ".png");
		g.blit(RenderPipelines.GUI_TEXTURED, atlas, x, y, (i % 16) * tile, (i / 16) * tile, unit, unit, tile, tile, 16 * tile, rows * tile, color);
		return true;
	}

	public static void glyph(GuiGraphics g, int icon, int x, int y, int size, int color) {
		g.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, icon * 64, 0, size, size, 64, 64, ICON_COUNT * 64, 64, color);
	}

	/** An on/off switch, 20x10: an accent track with the knob right when on, a neutral track with the knob left when off. */
	public static void toggle(GuiGraphics g, int x, int y, boolean on, boolean hot) {
		round(g, x, y, 20, 10, 5, on ? ACCENT : OFF);
		if (hot) round(g, x, y, 20, 10, 5, HOVER);
		// The accents are light, so the knob is dark on an on-track and light on an off-track.
		round(g, on ? x + 11 : x + 1, y + 1, 8, 8, 4, on ? INK : 0xFFF2F3F5);
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

	/** Hides Minecraft's tutorial hint toasts (they sit where our panels are); advancement, recipe and system toasts stay. */
	public static void hideTutorialToasts() {
		var toasts = Minecraft.getInstance().getToastManager();
		var tutorial = toasts.getToast(net.minecraft.client.gui.components.toasts.TutorialToast.class,
				net.minecraft.client.gui.components.toasts.Toast.NO_TOKEN);
		if (tutorial != null) tutorial.hide();
	}

	/**
	 * Left / right as -1 / +1 (0 otherwise). The keypad 4 / 6 count too: a key event without the "extended" flag (some
	 * remote or scripted input) arrives as the keypad key, and the menus should still follow it.
	 */
	public static int horizontal(int key) {
		if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_6) return 1;
		return key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_4 ? -1 : 0;
	}

	/** Up / down as -1 / +1 (0 otherwise), keypad 8 / 2 included (see horizontal). */
	public static int vertical(int key) {
		if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_2) return 1;
		return key == org.lwjgl.glfw.GLFW.GLFW_KEY_UP || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_8 ? -1 : 0;
	}

	/** Any arrow as a step round a ring: right / down forward, left / up back. */
	public static int arrow(int key) {
		return horizontal(key) != 0 ? horizontal(key) : vertical(key);
	}

	// ---- interface sounds and motion (set by ThemeMod) ----

	/** 0..1; 0 = silent. */
	public static float soundVolume = 0.6f;
	public static boolean reduceMotion;

	/** Plays one of our interface sounds (ui.press, ui.hover, ui.open) in the Interface category, at the Theme volume. */
	public static void sound(String name) {
		if (soundVolume <= 0) return;
		Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
				net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(NewgenClient.ID, "ui." + name)), 1f, soundVolume));
	}

	/** Ease-out (quartic) progress from 0 to 1 of an animation that started at startMs and lasts ms. */
	public static float ease(long startMs, int ms) {
		if (reduceMotion) return 1f;
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

		/** Our press sound instead of the vanilla click. */
		@Override
		public void playDownSound(net.minecraft.client.sounds.SoundManager manager) {
			sound("press");
		}

		@Override
		protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float delta) {
			long now = System.currentTimeMillis();
			float dt = Math.min(100, now - last);
			last = now;
			hover = isHoveredOrFocused() ? Math.min(1, hover + dt / 150f) : Math.max(0, hover - dt / 120f);
			boolean pressed = isHovered() && org.lwjgl.glfw.GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), 0) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
			button(g, getX(), getY(), getWidth(), getHeight(), getMessage().getString(), icon, hover, pressed, fill, face);
		}
	}
}

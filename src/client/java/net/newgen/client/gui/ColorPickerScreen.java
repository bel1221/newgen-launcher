package net.newgen.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.Mods;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

/**
 * The colour picker, opened from any colour option: a saturation/brightness square, a hue strip and an alpha strip,
 * the hex code (typed, or copied and pasted), quick swatches, chroma (a running rainbow) with its speed, and reset.
 * <pre>
 * Colour name                                      [x]
 * +------------------------+ |h| |a|   [swatch]
 * |   saturation x         | |u| |l|   #AARRGGBB  [Copy] [Paste]
 * |   brightness           | |e| |p|   o o o o o o
 * +------------------------+ | | |h|   Chroma [switch]  speed ---o--
 *                                      [Reset]            [Done]
 * </pre>
 * Every change applies at once (the HUD behind updates live) and is saved when the picker closes.
 */
public class ColorPickerScreen extends Screen {
	private static final int[] SWATCHES = {0xFFFFFFFF, 0xFFAAAAAA, 0xFF000000, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55,
			0xFF55FFFF, 0xFF5599FF, 0xFFE7A3B8, 0xFF9CCB8F, 0xFFE6B872};
	private static final int PW = 260, PH = 150, SQ = 96, STRIP = 10, PAD = 10;
	private final Screen parent;
	private final Setting.Color color;
	private final String title;
	private float hue, sat, bri;
	private int alpha;
	private int px, py;
	private String dragging = ""; // "sv", "hue", "alpha", "speed"
	private String hex;
	private boolean typing;

	public ColorPickerScreen(Screen parent, Setting.Color color, String title) {
		super(Component.literal(title));
		this.parent = parent;
		this.color = color;
		this.title = title;
		fromValue();
	}

	private void fromValue() {
		float[] hsb = java.awt.Color.RGBtoHSB(color.value >> 16 & 0xFF, color.value >> 8 & 0xFF, color.value & 0xFF, null);
		hue = hsb[0];
		sat = hsb[1];
		bri = hsb[2];
		alpha = color.value >>> 24;
		hex = String.format("%08X", color.value);
	}

	private void toValue() {
		color.value = alpha << 24 | java.awt.Color.HSBtoRGB(hue, sat, bri) & 0xFFFFFF;
		if (!typing) hex = String.format("%08X", color.value);
	}

	@Override
	protected void init() {
		// 260x150 fits every window Minecraft allows at its GUI scale (the smallest is about 320x180 units); clamped anyway.
		px = Math.max(2, (width - PW) / 2);
		py = Math.max(2, (height - PH) / 2);
	}

	private int sqX() {
		return px + PAD;
	}

	private int sqY() {
		return py + 24;
	}

	private int hueX() {
		return sqX() + SQ + 8;
	}

	private int alphaX() {
		return hueX() + STRIP + 6;
	}

	private int sideX() {
		return alphaX() + STRIP + 12;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (parent != null) parent.renderBackground(g, mouseX, mouseY, delta);
		g.fill(0, 0, width, height, 0x40000000);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		Ui.panel(g, px, py, PW, PH);
		Ui.text(g, Ui.fit(title, PW - 40, Ui.UI, false), px + PAD, py + 8, Ui.TEXT, Ui.TITLE);
		closeButton(g, mouseX, mouseY);

		// Saturation (x) and brightness (y) for the current hue: columns of top-to-bottom gradients.
		int x0 = sqX(), y0 = sqY();
		for (int i = 0; i < SQ; i += 2) {
			int top = 0xFF000000 | java.awt.Color.HSBtoRGB(hue, i / (float) SQ, 1f) & 0xFFFFFF;
			g.fillGradient(x0 + i, y0, x0 + i + 2, y0 + SQ, top, 0xFF000000);
		}
		Ui.ring(g, x0 - 1, y0 - 1, SQ + 2, SQ + 2, 2, Ui.LINE);
		int cx = x0 + Math.round(sat * SQ), cy = y0 + Math.round((1 - bri) * SQ);
		Ui.ring(g, cx - 3, cy - 3, 6, 6, 3, 0xFFFFFFFF);

		// Hue strip, top to bottom.
		for (int i = 0; i < SQ; i += 4) {
			int a = 0xFF000000 | java.awt.Color.HSBtoRGB(i / (float) SQ, 1f, 1f), b = 0xFF000000 | java.awt.Color.HSBtoRGB((i + 4) / (float) SQ, 1f, 1f);
			g.fillGradient(hueX(), y0 + i, hueX() + STRIP, y0 + i + 4, a, b);
		}
		marker(g, hueX(), y0 + Math.round(hue * SQ));

		// Alpha strip over a checker, so see-through shows.
		for (int i = 0; i < SQ; i += 5) for (int k = 0; k < 2; k++) g.fill(alphaX() + k * 5, y0 + i, alphaX() + k * 5 + 5, y0 + Math.min(SQ, i + 5), ((i / 5 + k) & 1) == 0 ? 0xFF9A9A9A : 0xFF6A6A6A);
		int rgb = color.value & 0xFFFFFF;
		g.fillGradient(alphaX(), y0, alphaX() + STRIP, y0 + SQ, 0xFF000000 | rgb, rgb);
		marker(g, alphaX(), y0 + Math.round((1 - alpha / 255f) * SQ));

		// Side: the swatch, hex with copy and paste, quick swatches, chroma and speed.
		int sx = sideX(), w = px + PW - PAD - sx, y = y0;
		Ui.round(g, sx, y, 22, 22, 4, 0xFF8C8C8C);
		Ui.round(g, sx, y, 22, 22, 4, color.current());
		Ui.ring(g, sx, y, 22, 22, 4, Ui.LINE);
		Ui.round(g, sx + 26, y + 4, w - 26, 14, Ui.R_SM, Ui.DARK);
		Ui.ring(g, sx + 26, y + 4, w - 26, 14, Ui.R_SM, typing ? 0x80FFFFFF : Ui.LINE);
		Ui.text(g, "#" + hex + (typing ? "_" : ""), sx + 31, y + 7, Ui.TEXT, Ui.UI);
		y += 28;
		int bw = (w - 4) / 2;
		Ui.button(g, sx, y, bw, 14, Ui.tr("color.copy"), Ui.inside(mouseX, mouseY, sx, y, bw, 14), Ui.DARK, Ui.CAPS);
		Ui.button(g, sx + w - bw, y, bw, 14, Ui.tr("color.paste"), Ui.inside(mouseX, mouseY, sx + w - bw, y, bw, 14), Ui.DARK, Ui.CAPS);
		y += 20;
		for (int i = 0; i < SWATCHES.length; i++) {
			int qx = sx + (i % 6) * 15, qy = y + (i / 6) * 13;
			Ui.round(g, qx, qy, 10, 10, 5, SWATCHES[i]);
			if (Ui.inside(mouseX, mouseY, qx - 1, qy - 1, 12, 12)) Ui.ring(g, qx - 2, qy - 2, 14, 14, 7, 0x80FFFFFF);
		}
		y += 30;
		Ui.text(g, Ui.tr("color.chroma"), sx, y + 1, Ui.SOFT, Ui.UI);
		Ui.toggle(g, sx + w - 20, y, color.chroma, Ui.inside(mouseX, mouseY, sx + w - 24, y - 2, 24, 14));
		y += 14;
		if (color.chroma) {
			int tw = w;
			int knob = sx + 4 + (int) (color.chromaSpeed / 60 * (tw - 8));
			Ui.round(g, sx, y + 3, tw, 3, 1, 0x70000000);
			Ui.round(g, sx, y + 3, knob - sx, 3, 1, Ui.ACCENT);
			Ui.round(g, knob - 4, y, 8, 8, 4, Ui.ACCENT);
		}

		// Foot: reset and done.
		int fy = py + PH - PAD - 14;
		Ui.button(g, px + PAD, fy, 60, 14, Ui.tr("color.reset"), Ui.inside(mouseX, mouseY, px + PAD, fy, 60, 14), Ui.DARK, Ui.CAPS);
		Ui.button(g, px + PW - PAD - 60, fy, 60, 14, Ui.tr("color.done"), Ui.inside(mouseX, mouseY, px + PW - PAD - 60, fy, 60, 14), Ui.PRIMARY, Ui.CAPS);
	}

	private void marker(GuiGraphics g, int x, int y) {
		Ui.ring(g, x - 2, y - 2, STRIP + 4, 4, 2, 0xFFFFFFFF);
	}

	private void closeButton(GuiGraphics g, int mouseX, int mouseY) {
		int cx = px + PW - PAD - 14, cy = py + 6;
		Ui.button(g, cx, cy, 14, 14, "", Ui.I_CLOSE, Ui.inside(mouseX, mouseY, cx, cy, 14, 14) ? 1f : 0f, Ui.DARK, Ui.CAPS);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		typing = false;
		int y0 = sqY(), sx = sideX(), w = px + PW - PAD - sx;
		if (Ui.inside(mx, my, px + PW - PAD - 14, py + 6, 14, 14) || Ui.inside(mx, my, px + PW - PAD - 60, py + PH - PAD - 14, 60, 14)) {
			onClose();
			return true;
		}
		if (Ui.inside(mx, my, px + PAD, py + PH - PAD - 14, 60, 14)) {
			color.reset();
			fromValue();
			return true;
		}
		if (Ui.inside(mx, my, sqX(), y0, SQ, SQ)) dragging = "sv";
		else if (Ui.inside(mx, my, hueX() - 2, y0, STRIP + 4, SQ)) dragging = "hue";
		else if (Ui.inside(mx, my, alphaX() - 2, y0, STRIP + 4, SQ)) dragging = "alpha";
		else if (Ui.inside(mx, my, sx + 26, y0 + 4, w - 26, 14)) typing = true;
		else if (Ui.inside(mx, my, sx, y0 + 28, (w - 4) / 2, 14)) minecraft.keyboardHandler.setClipboard("#" + String.format("%08X", color.value));
		else if (Ui.inside(mx, my, sx + w - (w - 4) / 2, y0 + 28, (w - 4) / 2, 14)) paste(minecraft.keyboardHandler.getClipboard());
		else if (Ui.inside(mx, my, sx + w - 24, y0 + 76, 24, 14)) color.chroma = !color.chroma;
		else if (color.chroma && Ui.inside(mx, my, sx, y0 + 88, w, 10)) dragging = "speed";
		else {
			for (int i = 0; i < SWATCHES.length; i++) {
				if (Ui.inside(mx, my, sx + (i % 6) * 15 - 1, y0 + 48 + (i / 6) * 13 - 1, 12, 12)) {
					color.value = color.value & 0xFF000000 | SWATCHES[i] & 0xFFFFFF;
					fromValue();
					return true;
				}
			}
		}
		drag(mx, my);
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		drag(event.x(), event.y());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = "";
		return true;
	}

	private void drag(double mx, double my) {
		float fy = (float) Math.max(0, Math.min(1, (my - sqY()) / SQ));
		switch (dragging) {
			case "sv" -> {
				sat = (float) Math.max(0, Math.min(1, (mx - sqX()) / SQ));
				bri = 1 - fy;
				toValue();
			}
			case "hue" -> {
				hue = Math.min(0.999f, fy);
				toValue();
			}
			case "alpha" -> {
				alpha = Math.round((1 - fy) * 255);
				toValue();
			}
			case "speed" -> {
				int sx = sideX(), w = px + PW - PAD - sx;
				color.chromaSpeed = Math.max(1, Math.min(60, Math.round((mx - sx - 4) / (w - 8) * 60)));
			}
			default -> {
			}
		}
	}

	/** "#RRGGBB", "#AARRGGBB" or the same without "#". */
	private void paste(String text) {
		String s = text.trim().replace("#", "");
		try {
			if (s.length() == 6) color.value = 0xFF000000 | Integer.parseInt(s, 16);
			else if (s.length() == 8) color.value = (int) Long.parseLong(s, 16);
			else return;
			fromValue();
		} catch (NumberFormatException e) {
			// not a colour: ignored
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isPaste()) {
			paste(minecraft.keyboardHandler.getClipboard());
			return true;
		}
		if (event.isCopy()) {
			minecraft.keyboardHandler.setClipboard("#" + String.format("%08X", color.value));
			return true;
		}
		if (typing) {
			if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !hex.isEmpty()) hex = hex.substring(0, hex.length() - 1);
			else if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_ESCAPE) {
				typing = false;
				paste(hex);
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!typing) return false;
		String c = event.codepointAsString().toUpperCase();
		if (c.length() == 1 && Character.digit(c.charAt(0), 16) >= 0 && hex.length() < 8) {
			hex += c;
			if (hex.length() == 8 || hex.length() == 6) paste(hex);
		}
		return true;
	}

	@Override
	public void onClose() {
		Mods.save();
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

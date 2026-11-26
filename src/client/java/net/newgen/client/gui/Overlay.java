package net.newgen.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * The menus' shared overlays, all in our own style: toasts (with Undo), the one confirm dialog, hover tooltips and the
 * fades at the edges of a scrolled list. Screens call the render methods last (each starts a new stratum so it sits on
 * top) and offer their clicks and keys to the click / key methods first.
 */
public final class Overlay {
	private Overlay() {
	}

	// ---- toasts ----

	private record Toast(String text, Runnable undo, long start) {
	}

	private static final List<Toast> TOASTS = new ArrayList<>();
	private static final int LIFE = 6000, TW = 200, TH = 20, UNDO_W = 50;

	/** A short message at the bottom of the screen; undo (may be null) adds an Undo button and makes Ctrl+Z run it. */
	public static void toast(String text, Runnable undo) {
		TOASTS.add(0, new Toast(text, undo, System.currentTimeMillis()));
		while (TOASTS.size() > 3) TOASTS.remove(TOASTS.size() - 1);
	}

	private static int toastX(int width) {
		return (width - TW) / 2;
	}

	private static int toastY(int height, int i) {
		return height - 10 - (i + 1) * (TH + 4);
	}

	public static void renderToasts(GuiGraphics g, int width, int height, int mouseX, int mouseY) {
		long now = System.currentTimeMillis();
		TOASTS.removeIf(t -> now - t.start > LIFE);
		if (TOASTS.isEmpty()) return;
		g.nextStratum();
		for (int i = 0; i < TOASTS.size(); i++) {
			Toast t = TOASTS.get(i);
			int x = toastX(width), y = toastY(height, i) + Math.round((1 - Ui.ease(t.start, 220)) * 10);
			Ui.round(g, x, y, TW, TH, Ui.R_MD, Ui.GLASS);
			Ui.round(g, x, y, TW, TH, Ui.R_MD, 0x40000000);
			Ui.ring(g, x, y, TW, TH, Ui.R_MD, Ui.LINE);
			Ui.round(g, x + 6, y + 6, 2, TH - 12, 1, Ui.ACCENT);
			int textW = TW - 18 - (t.undo != null ? UNDO_W + 6 : 0);
			Ui.text(g, Ui.fit(t.text, textW, Ui.UI, false), x + 13, y + 7, Ui.TEXT, Ui.UI);
			if (t.undo != null) {
				int bx = x + TW - UNDO_W - 4;
				Ui.button(g, bx, y + 3, UNDO_W, 14, Ui.tr("toast.undo"), Ui.I_UNDO, Ui.inside(mouseX, mouseY, bx, y + 3, UNDO_W, 14) ? 1f : 0f, Ui.DARK, Ui.CAPS);
			}
			// Time left, as a thin line along the bottom.
			float left = 1 - (now - t.start) / (float) LIFE;
			g.fill(x + 6, y + TH - 2, x + 6 + Math.round((TW - 12) * left), y + TH - 1, 0x30FFFFFF);
		}
	}

	/** A click on a toast: its Undo runs it; anywhere else on it dismisses it. */
	public static boolean clickToast(double mx, double my, int width, int height) {
		for (int i = 0; i < TOASTS.size(); i++) {
			Toast t = TOASTS.get(i);
			int x = toastX(width), y = toastY(height, i);
			if (!Ui.inside(mx, my, x, y, TW, TH)) continue;
			TOASTS.remove(i);
			if (t.undo != null && mx >= x + TW - UNDO_W - 4) run(t);
			return true;
		}
		return false;
	}

	/** Ctrl+Z: undoes the newest toast that can be undone. */
	public static boolean undoLatest() {
		for (Toast t : TOASTS) {
			if (t.undo == null) continue;
			TOASTS.remove(t);
			run(t);
			return true;
		}
		return false;
	}

	private static void run(Toast t) {
		t.undo.run();
		Ui.sound("press");
	}

	// ---- the confirm dialog ----

	private static String confirmText, confirmLabel;
	private static Runnable confirmYes;
	private static final int CW = 220, CB = 70;

	/** Asks before something big (turning a whole tab off, resetting a mod); yes runs on the confirm button or Enter. */
	public static void confirm(String text, String yesLabel, Runnable yes) {
		confirmText = text;
		confirmLabel = yesLabel;
		confirmYes = yes;
	}

	public static boolean confirming() {
		return confirmYes != null;
	}

	private static int confirmH() {
		return 18 + wrap(confirmText, CW - 24).size() * 10 + 30;
	}

	public static void renderConfirm(GuiGraphics g, int width, int height, int mouseX, int mouseY) {
		if (confirmYes == null) return;
		g.nextStratum();
		g.fill(0, 0, width, height, 0x80000000);
		int h = confirmH(), x = (width - CW) / 2, y = (height - h) / 2;
		Ui.panel(g, x, y, CW, h);
		List<String> lines = wrap(confirmText, CW - 24);
		for (int i = 0; i < lines.size(); i++) Ui.text(g, lines.get(i), x + 12, y + 14 + i * 10, Ui.TEXT, Ui.UI);
		int by = y + h - 24;
		Ui.button(g, x + CW - 12 - CB * 2 - 6, by, CB, 14, Ui.tr("confirm.cancel"), Ui.inside(mouseX, mouseY, x + CW - 12 - CB * 2 - 6, by, CB, 14), Ui.DARK, Ui.CAPS);
		Ui.button(g, x + CW - 12 - CB, by, CB, 14, confirmLabel, Ui.inside(mouseX, mouseY, x + CW - 12 - CB, by, CB, 14), Ui.PRIMARY, Ui.CAPS);
	}

	/** While the dialog is open it takes every click: its two buttons, and outside it cancels. */
	public static boolean clickConfirm(double mx, double my, int width, int height) {
		if (confirmYes == null) return false;
		int h = confirmH(), x = (width - CW) / 2, y = (height - h) / 2, by = y + h - 24;
		if (Ui.inside(mx, my, x + CW - 12 - CB, by, CB, 14)) yes();
		else if (Ui.inside(mx, my, x + CW - 12 - CB * 2 - 6, by, CB, 14) || !Ui.inside(mx, my, x, y, CW, h)) confirmYes = null;
		return true;
	}

	public static boolean keyConfirm(int key) {
		if (confirmYes == null) return false;
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) yes();
		else if (key == GLFW.GLFW_KEY_ESCAPE) confirmYes = null;
		return true;
	}

	private static void yes() {
		Runnable r = confirmYes;
		confirmYes = null;
		r.run();
	}

	// ---- tooltips ----

	private static String tipText;
	private static long tipSince;

	/**
	 * Draws a tooltip once the mouse has rested on the same thing for half a second. text null = nothing under the
	 * mouse has one (the timer restarts).
	 */
	public static void tooltip(GuiGraphics g, String text, int mouseX, int mouseY, int width, int height) {
		if (text == null || !text.equals(tipText)) {
			tipText = text;
			tipSince = System.currentTimeMillis();
			return;
		}
		if (System.currentTimeMillis() - tipSince < 500) return;
		List<String> lines = wrap(text, 170);
		int w = 0;
		for (String l : lines) w = Math.max(w, Ui.width(l, Ui.UI));
		w += 12;
		int h = lines.size() * 10 + 8, x = Math.min(mouseX + 10, width - w - 2), y = mouseY + 12;
		if (y + h > height - 2) y = mouseY - h - 4;
		g.nextStratum();
		Ui.round(g, x, y, w, h, Ui.R_SM, Ui.GLASS);
		Ui.round(g, x, y, w, h, Ui.R_SM, 0x60000000);
		Ui.ring(g, x, y, w, h, Ui.R_SM, Ui.LINE);
		for (int i = 0; i < lines.size(); i++) Ui.text(g, lines.get(i), x + 6, y + 5 + i * 10, Ui.TEXT, Ui.UI);
	}

	/** Word wrap to a width in the UI face. */
	public static List<String> wrap(String text, int w) {
		List<String> out = new ArrayList<>();
		for (String para : text.split("\n")) {
			StringBuilder line = new StringBuilder();
			for (String word : para.split(" ")) {
				String next = line.isEmpty() ? word : line + " " + word;
				if (!line.isEmpty() && Ui.width(next, Ui.UI) > w) {
					out.add(line.toString());
					line = new StringBuilder(word);
				} else {
					line = new StringBuilder(next);
				}
			}
			out.add(line.toString());
		}
		return out;
	}

	// ---- scrolling ----

	/** Smooth scrolling: the shown offset runs towards the goal (straight there with reduced motion). */
	public static int approach(int shown, int goal) {
		if (Ui.reduceMotion || Math.abs(goal - shown) <= 1) return goal;
		return shown + Math.round((goal - shown) * 0.35f);
	}

	/** Soft shadows at the top and bottom edges of a scrolled view, only where there is more to scroll to. */
	public static void fades(GuiGraphics g, int x1, int top, int x2, int bottom, int scroll, int max) {
		if (scroll > 0) g.fillGradient(x1, top, x2, top + 10, 0x50000000, 0x00000000);
		if (scroll < max) g.fillGradient(x1, bottom - 10, x2, bottom, 0x00000000, 0x50000000);
	}
}

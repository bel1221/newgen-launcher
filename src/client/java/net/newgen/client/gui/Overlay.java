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

	private static final class Toast {
		final String text;
		final Runnable undo;
		long start; // moved forward while the mouse rests on the toast, so hovering pauses it

		Toast(String text, Runnable undo, long start) {
			this.text = text;
			this.undo = undo;
			this.start = start;
		}
	}

	/** One line of the notifications centre: every toast, newest first. */
	public record Note(String text, long time) {
	}

	private static final List<Toast> TOASTS = new ArrayList<>();
	public static final List<Note> HISTORY = new ArrayList<>();
	/** Toasts since the notifications centre was last opened (the dot on its bell). */
	public static int unread;
	private static final int LIFE = 5000, TW = 200, TH = 20, UNDO_W = 50;

	/** A short message at the bottom right; undo (may be null) adds an Undo button and makes Ctrl+Z run it. */
	public static void toast(String text, Runnable undo) {
		long now = System.currentTimeMillis();
		TOASTS.add(0, new Toast(text, undo, now));
		while (TOASTS.size() > 2) TOASTS.remove(TOASTS.size() - 1);
		HISTORY.add(0, new Note(text, now));
		while (HISTORY.size() > 50) HISTORY.remove(HISTORY.size() - 1);
		unread++;
	}

	private static int toastX(int width) {
		return width - TW - 10;
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
			if (Ui.inside(mouseX, mouseY, toastX(width), toastY(height, i), TW, TH) && now - t.start > 250) t.start = Math.max(t.start, now - LIFE / 2);
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

	// ---- the notifications centre: every toast of this session, newest first ----

	public static boolean notesOpen;
	private static int notesRight, notesTop;
	private static final int NW = 230, NOTE_H = 14, NOTES_MAX = 14;

	/** Opens (or closes) the list under a bell whose right edge and bottom are given. */
	public static void toggleNotes(int right, int top) {
		notesOpen = !notesOpen;
		notesRight = right;
		notesTop = top;
		unread = 0;
	}

	private static int notesH() {
		return 24 + Math.max(1, Math.min(NOTES_MAX, HISTORY.size())) * NOTE_H + 6;
	}

	private static String ago(long time) {
		long min = (System.currentTimeMillis() - time) / 60000;
		if (min < 1) return Ui.tr("notes.now");
		return min < 60 ? Ui.trf("notes.min", min) : Ui.trf("notes.hour", min / 60);
	}

	public static void renderNotes(GuiGraphics g, int mouseX, int mouseY) {
		if (!notesOpen) return;
		g.nextStratum();
		int x = notesRight - NW, y = notesTop, h = notesH();
		Ui.panel(g, x, y, NW, h);
		Ui.round(g, x, y, NW, h, Ui.R_LG, 0x30000000);
		Ui.text(g, Ui.tr("notes"), x + 10, y + 8, Ui.TEXT, Ui.TITLE);
		String clear = Ui.tr("notes.clear");
		int cw = Ui.width(clear, Ui.CAPS) + 14;
		if (!HISTORY.isEmpty()) Ui.button(g, x + NW - 8 - cw, y + 5, cw, 14, clear, Ui.inside(mouseX, mouseY, x + NW - 8 - cw, y + 5, cw, 14), Ui.DARK, Ui.CAPS);
		if (HISTORY.isEmpty()) Ui.text(g, Ui.tr("notes.none"), x + 10, y + 28, Ui.DIM, Ui.UI);
		for (int i = 0; i < Math.min(NOTES_MAX, HISTORY.size()); i++) {
			Note n = HISTORY.get(i);
			int ry = y + 24 + i * NOTE_H;
			String when = ago(n.time);
			int ww = Ui.width(when, Ui.UI);
			Ui.round(g, x + 10, ry + 5, 3, 3, 1, i < 1 ? Ui.ACCENT : 0x40FFFFFF);
			Ui.text(g, Ui.fit(n.text, NW - 34 - ww, Ui.UI, false), x + 18, ry + 3, Ui.SOFT, Ui.UI);
			Ui.text(g, when, x + NW - 10 - ww, ry + 3, Ui.DIM, Ui.UI);
		}
	}

	/** While open the list takes every click: Clear, a click inside does nothing, outside closes it. */
	public static boolean clickNotes(double mx, double my) {
		if (!notesOpen) return false;
		int x = notesRight - NW, y = notesTop;
		int cw = Ui.width(Ui.tr("notes.clear"), Ui.CAPS) + 14;
		if (Ui.inside(mx, my, x + NW - 8 - cw, y + 5, cw, 14)) HISTORY.clear();
		else if (!Ui.inside(mx, my, x, y, NW, notesH())) notesOpen = false;
		return true;
	}

	/** The bell button: the glyph, with a dot when there are toasts not seen in the list yet. */
	public static void bell(GuiGraphics g, int x, int y, int size, int mouseX, int mouseY) {
		Ui.button(g, x, y, size, size, "", Ui.I_BELL, Ui.inside(mouseX, mouseY, x, y, size, size) ? 1f : 0f, notesOpen ? Ui.SELECTED : Ui.DARK, Ui.CAPS);
		if (unread > 0) Ui.round(g, x + size - 4, y, 4, 4, 2, Ui.ACCENT);
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

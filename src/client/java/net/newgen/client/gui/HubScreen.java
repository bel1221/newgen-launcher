package net.newgen.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.newgen.client.NewgenClient;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Mods;

/**
 * What Right Shift opens, kept minimal: the live game (not blurred, so the HUD reads as it will in play), the NewGen
 * logo in the middle and one row under it: [options] [Mods] [language]. The HUD elements stay on screen where they
 * can be arranged: drag to move, mouse wheel resizes, right click puts back. One quiet hint line at the foot.
 */
public class HubScreen extends Screen {
	private static final int ROW_H = 20, MODS_W = 110, GAP = 4;

	private HudMod dragging;
	private double grabX, grabY; // where inside the element the mouse grabbed it
	private int logoW, top, hintY;
	private boolean opened; // init runs again on resize; the open sound plays once
	private static final int HINT_H = 13;

	public HubScreen() {
		super(Component.literal("Newgen Client"));
	}

	@Override
	protected void init() {
		// Vanilla's tutorial toast ("Move with W, A, S and D") sits where our panels are; it goes. Other toasts stay.
		Ui.hideTutorialToasts();
		if (!opened) {
			opened = true;
			Ui.sound("open");
		}
		HudMod.editing = true;
		// The logo shrinks with the window and goes away before it would push the row off screen.
		logoW = Math.max(0, Math.min(Math.min(150, width - 40), (height - 90) * 4));
		int logoH = logoW >= 40 ? Ui.logoHeight(logoW) + 10 : 0;
		// The cluster ends 20 above the middle, clear of the crosshair and the Attack Indicator under it.
		// The cluster (logo, row, hint under it) ends 18 above the middle, clear of the crosshair and the Attack Indicator.
		top = Math.max(4, height / 2 - 18 - HINT_H - logoH - ROW_H);
		hintY = top + logoH + ROW_H + 5;
		int y = top + logoH, x = (width - MODS_W) / 2;
		addRenderableWidget(new Ui.Btn(x - GAP - ROW_H, y, ROW_H, ROW_H, "", Ui.I_GEAR, Ui.DARK, Ui.CAPS,
				() -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))).narration(I18n.get("menu.options")));
		addRenderableWidget(new Ui.Btn(x, y, MODS_W, ROW_H, Ui.tr("mods"), Ui.DARK, Ui.CAPS, () -> minecraft.setScreen(new ModsScreen(this))));
		addRenderableWidget(new Ui.Btn(x + MODS_W + GAP, y, ROW_H, ROW_H, "", Ui.I_GLOBE, Ui.DARK, Ui.CAPS,
				() -> minecraft.setScreen(new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager())))
				.narration(I18n.get("options.language")));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// In a world: the live game with only a light shade, no blur. Without one: the panorama.
		if (minecraft.level == null && !VideoBackground.render(g, width, height)) renderPanorama(g, delta);
		else g.fill(0, 0, width, height, 0x30000000);
	}

	// ===================================================================================================================
	// HUD editor (Wave 1A step 2). Everything below works on the HUD elements drawn on this screen.
	//   click: select (a group selects together); shift-click: add/remove; drag on empty space: box select
	//   drag: move the selection, snapping to screen edges/centre, other elements and the grid (Alt: no snapping),
	//         Shift locks the axis; drag the corner handle: resize; wheel or +/-: scale
	//   arrows: nudge 1 (Shift: 10); Ctrl+Z / Ctrl+Y: undo / redo; Ctrl+C / Ctrl+V: copy / paste style;
	//   Ctrl+G / Ctrl+Shift+G: group / ungroup; L: lock; Ctrl+A: select all; G: grid; ?: shortcuts; right click: menu
	// ===================================================================================================================

	private static final int SNAP = 4, GRID = 10, HANDLE = 5;
	private static boolean grid;
	private final java.util.LinkedHashSet<HudMod> selection = new java.util.LinkedHashSet<>();
	private final java.util.Map<HudMod, int[]> dragStart = new java.util.HashMap<>();
	private double downX, downY;
	private boolean moving, boxing;
	private HudMod resizing;
	private double resizeScale;
	private int resizeW;
	private final List<int[]> guides = new java.util.ArrayList<>(); // {0, x} vertical, {1, y} horizontal
	private final java.util.ArrayDeque<java.util.Map<HudMod, float[]>> undo = new java.util.ArrayDeque<>(), redo = new java.util.ArrayDeque<>();
	private static HudMod styleClipboard;
	private HudMod menuFor;
	private int menuX, menuY;
	private boolean showShortcuts, showAdd;

	/** The elements on screen. Without a world there is nothing they could show. */
	private List<HudMod> huds() {
		return minecraft.level == null ? List.of() : Mods.huds();
	}

	// ---- undo ----

	private java.util.Map<HudMod, float[]> snapshot() {
		java.util.Map<HudMod, float[]> s = new java.util.HashMap<>();
		for (HudMod h : Mods.allHuds()) s.put(h, new float[]{h.x, h.y, (float) h.scale.get(), h.locked ? 1 : 0, owner(h).enabled ? 1 : 0});
		return s;
	}

	private void restore(java.util.Map<HudMod, float[]> s) {
		s.forEach((h, v) -> {
			h.x = v[0];
			h.y = v[1];
			h.scale.set(v[2]);
			h.locked = v[3] == 1;
			owner(h).enabled = v[4] == 1;
		});
		Mods.save();
	}

	/** Call before a change, so it can be undone. */
	private void remember() {
		undo.push(snapshot());
		if (undo.size() > 50) undo.removeLast();
		redo.clear();
	}

	/** The mod an element belongs to (itself, or the mod whose indicator it is). */
	private static Mod owner(HudMod h) {
		for (Mod m : Mods.ALL) if (m == h || m.hud == h) return m;
		return h;
	}

	// ---- geometry ----

	private void place(HudMod h, int left, int top) {
		int freeX = width - h.width, freeY = height - h.height;
		h.x = freeX <= 0 ? 0 : Mods.clamp01(left / (float) freeX);
		h.y = freeY <= 0 ? 0 : Mods.clamp01(top / (float) freeY);
		h.left = Math.max(0, Math.min(left, freeX));
		h.top = Math.max(0, Math.min(top, freeY));
	}

	private void selectWithGroup(HudMod h, boolean add) {
		if (!add) selection.clear();
		boolean on = !add || !selection.contains(h);
		for (HudMod o : huds()) if (o == h || !h.group.isEmpty() && h.group.equals(o.group)) {
			if (on) selection.add(o);
			else selection.remove(o);
		}
	}

	/** The selection's bounding box {left, top, right, bottom} with the drag offset applied. */
	private int[] box(int dx, int dy) {
		int l = Integer.MAX_VALUE, t = Integer.MAX_VALUE, r = Integer.MIN_VALUE, b = Integer.MIN_VALUE;
		for (HudMod h : selection) {
			int[] s = dragStart.get(h);
			if (s == null) continue;
			l = Math.min(l, s[0] + dx);
			t = Math.min(t, s[1] + dy);
			r = Math.max(r, s[0] + dx + h.width);
			b = Math.max(b, s[1] + dy + h.height);
		}
		return new int[]{l, t, r, b};
	}

	/** The smallest shift (within SNAP) that puts one of the box's lines on a target line; 0 = none. */
	private int snap(int[] lines, List<Integer> targets, int axis) {
		int best = SNAP + 1, line = 0;
		for (int v : lines) for (int t : targets) if (Math.abs(t - v) < Math.abs(best)) {
			best = t - v;
			line = t;
		}
		if (Math.abs(best) > SNAP) return 0;
		guides.add(new int[]{axis, line});
		return best;
	}

	private void moveSelection(double mx, double my, boolean shift, boolean alt) {
		int dx = (int) Math.round(mx - downX), dy = (int) Math.round(my - downY);
		if (shift) {
			if (Math.abs(dx) > Math.abs(dy)) dy = 0;
			else dx = 0;
		}
		guides.clear();
		if (!alt) {
			List<Integer> xs = new java.util.ArrayList<>(List.of(0, width / 2, width)), ys = new java.util.ArrayList<>(List.of(0, height / 2, height));
			for (HudMod o : huds()) {
				if (selection.contains(o)) continue;
				xs.addAll(List.of(o.left, o.left + o.width / 2, o.left + o.width));
				ys.addAll(List.of(o.top, o.top + o.height / 2, o.top + o.height));
			}
			if (grid) {
				for (int g = 0; g <= width; g += GRID) xs.add(g);
				for (int g = 0; g <= height; g += GRID) ys.add(g);
			}
			int[] b = box(dx, dy);
			if (!shift || dx != 0) dx += snap(new int[]{b[0], (b[0] + b[2]) / 2, b[2]}, xs, 0);
			if (!shift || dy != 0) dy += snap(new int[]{b[1], (b[1] + b[3]) / 2, b[3]}, ys, 1);
		}
		for (HudMod h : selection) {
			int[] s = dragStart.get(h);
			if (s != null && !h.locked) place(h, s[0] + dx, s[1] + dy);
		}
	}

	// ---- drawing ----

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// The elements, drawn here so they stay above this screen's shade.
		if (grid) {
			for (int x = 0; x < width; x += GRID) g.fill(x, 0, x + 1, height, 0x14FFFFFF);
			for (int y = 0; y < height; y += GRID) g.fill(0, y, width, y + 1, 0x14FFFFFF);
		}
		HudMod hot = moving ? null : at(mouseX, mouseY);
		for (HudMod hud : huds()) {
			hud.render(g, minecraft);
			int c = selection.contains(hud) ? Ui.ACCENT : Ui.LINE;
			g.renderOutline(hud.left - 1, hud.top - 1, hud.width + 2, hud.height + 2, c);
			if (hud.locked) Ui.glyph(g, Ui.I_CLOSE, hud.left + hud.width - 6, hud.top - 7, 6, Ui.DIM);
		}
		if (selection.size() == 1) {
			HudMod h = selection.iterator().next();
			if (!h.locked) g.fill(h.left + h.width - HANDLE + 1, h.top + h.height - HANDLE + 1, h.left + h.width + 1, h.top + h.height + 1, Ui.ACCENT);
		}
		for (int[] l : guides) {
			if (l[0] == 0) g.fill(l[1], 0, l[1] + 1, height, 0xC0FFFFFF);
			else g.fill(0, l[1], width, l[1] + 1, 0xC0FFFFFF);
		}
		if (boxing) {
			int x1 = (int) Math.min(downX, mouseX), y1 = (int) Math.min(downY, mouseY), x2 = (int) Math.max(downX, mouseX), y2 = (int) Math.max(downY, mouseY);
			g.fill(x1, y1, x2, y2, 0x18000000 | Ui.ACCENT & 0xFFFFFF);
			g.renderOutline(x1, y1, x2 - x1, y2 - y1, Ui.ACCENT);
		}
		if (hot != null && !selection.contains(hot)) frame(g, hot, Ui.SOFT);
		else if (hot != null) frame(g, hot, Ui.ACCENT);

		g.nextStratum();
		Ui.logo(g, width / 2, top, logoW);
		super.render(g, mouseX, mouseY, delta);
		if (minecraft.level != null) {
			String hint = Ui.fit(Ui.tr("hub.hint2"), width - 16, Ui.UI, false);
			int w = Ui.width(hint, Ui.UI) + 12;
			Ui.round(g, (width - w) / 2, hintY, w, 13, Ui.R_SM, Ui.GLASS);
			Ui.centered(g, hint, width / 2, hintY + 3, Ui.SOFT, Ui.UI);
			toolbar(g, mouseX, mouseY);
		}
		if (menuFor != null) menu(g, mouseX, mouseY);
		if (showAdd) addPanel(g, mouseX, mouseY);
		if (showShortcuts) shortcuts(g);
	}

	private static final String[] ALIGN = {"left", "centre", "right", "top", "middle", "bottom"};

	/** Bottom-left: [+ add HUD] [grid] [?]; with 2+ selected, the align buttons beside them. */
	private void toolbar(GuiGraphics g, int mouseX, int mouseY) {
		int y = height - 22, x = 6;
		String add = Ui.tr("hud.add");
		int aw = Ui.width(add, Ui.CAPS) + 26;
		Ui.button(g, x, y, aw, 16, add, Ui.I_GRID, Ui.inside(mouseX, mouseY, x, y, aw, 16) ? 1f : 0f, showAdd ? Ui.SELECTED : Ui.DARK, Ui.CAPS);
		x += aw + 4;
		Ui.button(g, x, y, 16, 16, "", Ui.I_LAYOUT, Ui.inside(mouseX, mouseY, x, y, 16, 16) ? 1f : 0f, grid ? Ui.SELECTED : Ui.DARK, Ui.CAPS);
		x += 20;
		Ui.button(g, x, y, 16, 16, "?", Ui.inside(mouseX, mouseY, x, y, 16, 16), showShortcuts ? Ui.SELECTED : Ui.DARK, Ui.CAPS);
		x += 24;
		if (selection.size() >= 2) {
			for (String a : ALIGN) {
				String label = Ui.tr("hud.align." + a);
				int w = Ui.width(label, Ui.CAPS) + 10;
				Ui.button(g, x, y, w, 16, label, Ui.inside(mouseX, mouseY, x, y, w, 16), Ui.DARK, Ui.CAPS);
				x += w + 3;
			}
		}
	}

	private boolean toolbarClick(double mx, double my) {
		int y = height - 22, x = 6;
		int aw = Ui.width(Ui.tr("hud.add"), Ui.CAPS) + 26;
		if (Ui.inside(mx, my, x, y, aw, 16)) {
			showAdd = !showAdd;
			return true;
		}
		x += aw + 4;
		if (Ui.inside(mx, my, x, y, 16, 16)) {
			grid = !grid;
			return true;
		}
		x += 20;
		if (Ui.inside(mx, my, x, y, 16, 16)) {
			showShortcuts = !showShortcuts;
			return true;
		}
		x += 24;
		if (selection.size() >= 2) {
			for (String a : ALIGN) {
				int w = Ui.width(Ui.tr("hud.align." + a), Ui.CAPS) + 10;
				if (Ui.inside(mx, my, x, y, w, 16)) {
					align(a);
					return true;
				}
				x += w + 3;
			}
		}
		return false;
	}

	private void align(String how) {
		remember();
		int l = Integer.MAX_VALUE, t = Integer.MAX_VALUE, r = Integer.MIN_VALUE, b = Integer.MIN_VALUE;
		for (HudMod h : selection) {
			l = Math.min(l, h.left);
			t = Math.min(t, h.top);
			r = Math.max(r, h.left + h.width);
			b = Math.max(b, h.top + h.height);
		}
		for (HudMod h : selection) {
			if (h.locked) continue;
			switch (how) {
				case "left" -> place(h, l, h.top);
				case "centre" -> place(h, (l + r) / 2 - h.width / 2, h.top);
				case "right" -> place(h, r - h.width, h.top);
				case "top" -> place(h, h.left, t);
				case "middle" -> place(h, h.left, (t + b) / 2 - h.height / 2);
				default -> place(h, h.left, b - h.height);
			}
		}
		Mods.save();
	}

	// ---- right-click menu ----

	private static final String[] MENU = {"settings", "reset", "anchor", "lock", "hide"};

	private void menu(GuiGraphics g, int mouseX, int mouseY) {
		int w = 110, h = MENU.length * 14 + 6;
		Ui.round(g, menuX, menuY, w, h, Ui.R_MD, 0xE0101215);
		Ui.ring(g, menuX, menuY, w, h, Ui.R_MD, Ui.LINE);
		for (int i = 0; i < MENU.length; i++) {
			int y = menuY + 3 + i * 14;
			if (Ui.inside(mouseX, mouseY, menuX + 2, y, w - 4, 14)) Ui.round(g, menuX + 2, y, w - 4, 14, Ui.R_SM, Ui.HOVER);
			String label = MENU[i].equals("lock") && menuFor.locked ? Ui.tr("hud.menu.unlock") : Ui.tr("hud.menu." + MENU[i]);
			Ui.text(g, label, menuX + 8, y + 3, MENU[i].equals("hide") ? Ui.SOFT : Ui.TEXT, Ui.UI);
		}
	}

	private boolean menuClick(double mx, double my) {
		int w = 110;
		for (int i = 0; i < MENU.length; i++) {
			if (!Ui.inside(mx, my, menuX + 2, menuY + 3 + i * 14, w - 4, 14)) continue;
			HudMod h = menuFor;
			menuFor = null;
			if (!MENU[i].equals("settings")) remember();
			switch (MENU[i]) {
				case "settings" -> minecraft.setScreen(new ModsScreen(this, owner(h)));
				case "reset" -> {
					h.x = h.defaultX;
					h.y = h.defaultY;
				}
				case "anchor" -> {
					// The nearest of the 9 anchor points (corners, edge middles, centre).
					h.x = Math.round(h.x * 2) / 2f;
					h.y = Math.round(h.y * 2) / 2f;
				}
				case "lock" -> h.locked = !h.locked;
				default -> {
					owner(h).enabled = false;
					selection.remove(h);
				}
			}
			Mods.save();
			return true;
		}
		menuFor = null;
		return false;
	}

	// ---- add-HUD browser and shortcuts ----

	private List<Mod> hidden() {
		List<Mod> out = new java.util.ArrayList<>();
		for (Mod m : Mods.ALL) if (m instanceof HudMod && !m.enabled) out.add(m);
		return out;
	}

	private int addTop() {
		return Math.max(36, height - 30 - Math.min(10, hidden().size()) * 16 - 26);
	}

	private void addPanel(GuiGraphics g, int mouseX, int mouseY) {
		List<Mod> list = hidden();
		int w = 170, n = Math.min(10, list.size()), h = 26 + Math.max(1, n) * 16, x = 6, y = addTop();
		Ui.panel(g, x, y, w, h);
		Ui.text(g, Ui.tr("hud.add"), x + 8, y + 8, Ui.TEXT, Ui.TITLE);
		if (list.isEmpty()) Ui.text(g, Ui.tr("hud.add.none"), x + 8, y + 26, Ui.DIM, Ui.UI);
		for (int i = 0; i < n; i++) {
			int ry = y + 22 + i * 16;
			if (Ui.inside(mouseX, mouseY, x + 4, ry, w - 8, 16)) Ui.round(g, x + 4, ry, w - 8, 16, Ui.R_SM, Ui.HOVER);
			Ui.modIcon(g, list.get(i).id, x + 8, ry + 1, 14, Ui.SOFT);
			Ui.text(g, Ui.fit(list.get(i).name, w - 40, Ui.UI, false), x + 28, ry + 4, Ui.TEXT, Ui.UI);
		}
	}

	private boolean addClick(double mx, double my) {
		List<Mod> list = hidden();
		int w = 170, n = Math.min(10, list.size()), x = 6, y = addTop();
		for (int i = 0; i < n; i++) {
			if (Ui.inside(mx, my, x + 4, y + 22 + i * 16, w - 8, 16)) {
				remember();
				list.get(i).enabled = true;
				Mods.save();
				return true;
			}
		}
		return Ui.inside(mx, my, x, y, w, 26 + Math.max(1, n) * 16);
	}

	private static final String[] KEYS = {"click", "shift_click", "box", "drag", "alt", "corner", "wheel", "arrows", "undo", "copy", "group", "lock", "all", "grid", "menu"};

	private void shortcuts(GuiGraphics g) {
		int w = 230, h = 26 + KEYS.length * 11, x = width - w - 6, y = Math.max(4, height - h - 30);
		Ui.panel(g, x, y, w, h);
		Ui.text(g, Ui.tr("hud.shortcuts"), x + 8, y + 8, Ui.TEXT, Ui.TITLE);
		for (int i = 0; i < KEYS.length; i++) Ui.text(g, Ui.fit(Ui.tr("hud.key." + KEYS[i]), w - 16, Ui.UI, false), x + 8, y + 24 + i * 11, Ui.SOFT, Ui.UI);
	}

	// ---- input ----

	private void frame(GuiGraphics g, HudMod hud, int color) {
		int w = Ui.width(hud.name, Ui.UI) + 8, x, y;
		if (hud.left + hud.width + 3 + w <= width) {
			x = hud.left + hud.width + 3;
			y = Math.min(hud.top, height - 12);
		} else if (hud.left - 3 - w >= 0) {
			x = hud.left - 3 - w;
			y = Math.min(hud.top, height - 12);
		} else {
			x = Math.max(0, Math.min(hud.left - 1, width - w));
			y = hud.top >= 13 ? hud.top - 13 : Math.min(hud.top + hud.height + 1, height - 12);
		}
		Ui.round(g, x, y, w, 12, Ui.R_SM, Ui.GLASS);
		Ui.text(g, hud.name, x + 4, y + 2, color | 0xFF000000, Ui.UI);
	}

	/** The element under the mouse. Later mods are drawn on top, so they win when elements overlap. */
	private HudMod at(double mx, double my) {
		HudMod found = null;
		for (HudMod hud : huds()) if (Ui.inside(mx, my, hud.left, hud.top, hud.width, hud.height)) found = hud;
		return found;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (menuFor != null) return menuClick(mx, my) || true;
		if (showAdd && addClick(mx, my)) return true;
		if (event.button() == 0 && minecraft.level != null && toolbarClick(mx, my)) return true;
		if (super.mouseClicked(event, doubleClick)) return true; // the hub's buttons
		HudMod hit = at(mx, my);
		boolean shift = event.hasShiftDown();
		if (event.button() == 1) {
			if (hit != null) {
				menuFor = hit;
				menuX = (int) Math.min(mx, width - 112);
				menuY = (int) Math.min(my, height - MENU.length * 14 - 8);
				if (!selection.contains(hit)) selectWithGroup(hit, false);
			}
			return true;
		}
		if (event.button() != 0) return false;
		downX = mx;
		downY = my;
		// The corner handle of a single selected element resizes it.
		if (selection.size() == 1) {
			HudMod h = selection.iterator().next();
			if (!h.locked && Ui.inside(mx, my, h.left + h.width - HANDLE, h.top + h.height - HANDLE, HANDLE + 2, HANDLE + 2)) {
				remember();
				resizing = h;
				resizeScale = h.scale.get();
				resizeW = h.width;
				return true;
			}
		}
		if (hit == null) {
			if (!shift) selection.clear();
			boxing = true;
			return true;
		}
		if (shift) selectWithGroup(hit, true);
		else if (!selection.contains(hit)) selectWithGroup(hit, false);
		remember();
		dragStart.clear();
		for (HudMod h : selection) dragStart.put(h, new int[]{h.left, h.top});
		moving = true;
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (resizing != null) {
			double w = Math.max(8, resizeW + (event.x() - downX));
			resizing.scale.set(resizeScale * w / Math.max(1, resizeW));
			return true;
		}
		if (moving) {
			moveSelection(event.x(), event.y(), event.hasShiftDown(), event.hasAltDown());
			return true;
		}
		return boxing || super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (boxing) {
			double x1 = Math.min(downX, event.x()), y1 = Math.min(downY, event.y()), x2 = Math.max(downX, event.x()), y2 = Math.max(downY, event.y());
			if (x2 - x1 > 2 || y2 - y1 > 2) for (HudMod h : huds()) {
				if (h.left < x2 && h.left + h.width > x1 && h.top < y2 && h.top + h.height > y1) selectWithGroup(h, true);
			}
			boxing = false;
		}
		if (moving || resizing != null) {
			moving = false;
			resizing = null;
			guides.clear();
			Mods.save();
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		HudMod hit = at(mouseX, mouseY);
		java.util.Collection<HudMod> targets = hit != null && selection.contains(hit) ? selection : hit != null ? List.of(hit) : List.of();
		if (targets.isEmpty() || scrollY == 0) return false;
		remember();
		for (HudMod h : targets) h.scale.set(h.scale.get() + Math.signum(scrollY) * h.scale.step);
		Mods.save();
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (NewgenClient.OPEN_MODS.matches(event)) {
			onClose();
			return true;
		}
		int key = event.key();
		boolean ctrl = event.hasControlDown(), shift = event.hasShiftDown();
		if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && (menuFor != null || showAdd || showShortcuts)) {
			menuFor = null;
			showAdd = showShortcuts = false;
			return true;
		}
		if (ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_Z && !undo.isEmpty()) {
			redo.push(snapshot());
			restore(undo.pop());
			return true;
		}
		if (ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_Y && !redo.isEmpty()) {
			undo.push(snapshot());
			restore(redo.pop());
			return true;
		}
		if (ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_A) {
			selection.clear();
			selection.addAll(huds());
			return true;
		}
		if (ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_C && !selection.isEmpty()) {
			styleClipboard = selection.iterator().next();
			return true;
		}
		if (ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_V && styleClipboard != null) {
			for (HudMod h : selection) if (h != styleClipboard) h.copyStyleFrom(styleClipboard);
			Mods.save();
			return true;
		}
		if (ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_G) {
			String id = shift ? "" : Long.toString(System.nanoTime(), 36);
			for (HudMod h : selection) h.group = id;
			Mods.save();
			return true;
		}
		if (!ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_G) {
			grid = !grid;
			return true;
		}
		if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_L && !selection.isEmpty()) {
			remember();
			boolean lock = !selection.iterator().next().locked;
			for (HudMod h : selection) h.locked = lock;
			Mods.save();
			return true;
		}
		if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_SLASH) {
			showShortcuts = !showShortcuts;
			return true;
		}
		int sx = Ui.horizontal(key), sy = Ui.vertical(key);
		if ((sx != 0 || sy != 0) && !selection.isEmpty()) {
			remember();
			int step = shift ? 10 : 1;
			for (HudMod h : selection) if (!h.locked) place(h, h.left + sx * step, h.top + sy * step);
			Mods.save();
			return true;
		}
		if ((key == org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD || key == org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS
				|| key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT) && !selection.isEmpty()) {
			remember();
			int dir = key == org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT ? -1 : 1;
			for (HudMod h : selection) h.scale.set(h.scale.get() + dir * h.scale.step);
			Mods.save();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		HudMod.editing = false;
		Mods.save();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

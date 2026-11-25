package net.newgen.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.newgen.client.NewgenClient;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Mods;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

/**
 * The "Newgen Mods" menu: a glass panel over the game (or over the panorama when opened from the title screen).
 * Two views: the grid of mod cards (category chips + search), and the options of one mod ("selected").
 * Both views scroll with the mouse wheel. Everything is drawn by hand; the small x/y helper
 * methods below are shared by drawing and clicking so the two can never disagree.
 */
public class ModsScreen extends Screen {
	/** Quick colours: a click on a colour swatch steps through these. */
	private static final int[] PALETTE = {
			0xFFFFFF, 0xAAAAAA, 0x000000, 0xFF5555, 0xFFAA00, 0xFFFF55,
			0x55FF55, 0x55FFFF, 0x5599FF, 0xAA55FF, 0xFF77CC};
	private static final String[] TABS = {"All", "HUD", "Visual", "Controls", "Utility"};

	private static final int PAD = 8, HEADER = 26, BAR_H = 18, GAP = 5, ROW_H = 20, HEAD_H = 14;
	/** A card is the name (NAME_H), the OPTIONS strip and the ENABLED/DISABLED strip. */
	private static final int NAME_H = 24, STRIP = 13, CARD_H = NAME_H + STRIP * 2;
	private static final int PILL_W = 32, PILL_H = 12, SWATCH = 10, HEX_W = 66, BUTTON_H = 14, COLUMN_GAP = 14;
	/** FAINT is for dividing lines and the scroll track, KNOB for the knob of an ON/OFF pill. */
	private static final int FAINT = 0x18FFFFFF, KNOB = 0xFFF2F3F5;

	private static int tab;           // static: coming back from the Right Shift screen lands on the same tab

	private final Screen parent;      // where Esc goes; null = back into the game
	private int px, py, pw, ph;       // panel position and size
	private int columns;
	private String search = "";
	private int gridScroll, rowScroll; // pixels scrolled in each view
	private int rowsHeight;           // height of all option rows, set by cells()
	private Mod selected;             // null = card grid, otherwise the mod whose options are shown
	private Setting.Num dragging;     // slider being dragged, if any
	private int dragX, dragW;         // where that slider is
	private Setting.Key listening;    // key setting waiting for a key press, if any
	private Setting typing;           // text or colour setting being typed into, if any
	private String hex = "";          // the hex digits typed so far, while typing is a colour

	public ModsScreen(Screen parent) {
		super(Component.literal("Newgen Mods"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		pw = Math.min(480, width - 12);
		ph = Math.min(300, height - 12);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		columns = Math.max(1, (pw - PAD * 2) / 108);
	}

	// ---- layout helpers ----

	private int right() {
		return px + pw - PAD;
	}

	/** Right edge of the scrolling content; leaves room for the scroll bar. */
	private int contentRight() {
		return right() - 7;
	}

	/** Top of the buttons in the header. */
	private int headY() {
		return py + (HEADER - BUTTON_H) / 2;
	}

	private int viewTop() {
		return py + HEADER + BAR_H + 6;
	}

	private int footY() {
		return py + ph - PAD - BUTTON_H;
	}

	private int viewBottom() {
		return footY() - 4;
	}

	private boolean inView(double my) {
		return my >= viewTop() && my < viewBottom();
	}

	private int cardW() {
		return (contentRight() - px - PAD - GAP * (columns - 1)) / columns;
	}

	private int cardX(int i) {
		return px + PAD + (i % columns) * (cardW() + GAP);
	}

	private int cardY(int i) {
		return viewTop() - gridScroll + (i / columns) * (CARD_H + GAP);
	}

	private int chipW(int i) {
		return Ui.width(Ui.spaced(TABS[i]), Ui.CAPS) + 12;
	}

	/** Left edge of chip i; chipX(TABS.length) is where the chips end. */
	private int chipX(int i) {
		int x = px + PAD;
		for (int k = 0; k < i; k++) x += chipW(k) + 4;
		return x;
	}

	private int searchX() {
		return Math.max(chipX(TABS.length) + 4, right() - 130);
	}

	private int barY() {
		return py + HEADER + 4;
	}

	private int toggleW() {
		return Ui.width(Ui.spaced("DISABLED"), Ui.CAPS) + 14;
	}

	/** Width of the bottom-left button: "Edit HUD layout" or "Reset to defaults". */
	private int footW(String label) {
		return Math.min(Ui.width(Ui.spaced(label), Ui.CAPS) + 16, pw - PAD * 2);
	}

	/** Width of the box used by choice, key and text options in a column that is w wide. */
	private static int fieldW(int w) {
		return Math.min(110, w / 2);
	}

	private static int sliderW(int w) {
		return Math.min(80, w / 3);
	}

	private int limit(int scroll, int contentHeight) {
		return Math.max(0, Math.min(scroll, contentHeight - (viewBottom() - viewTop())));
	}

	/** The mods of the current tab that match the search text. */
	private List<Mod> shown() {
		List<Mod> list = new ArrayList<>();
		String query = search.toLowerCase();
		for (Mod mod : Mods.ALL) {
			if (tab != 0 && !TABS[tab].equalsIgnoreCase(mod.category)) continue;
			if ((mod.name + " " + mod.description).toLowerCase().contains(query)) list.add(mod);
		}
		return list;
	}

	/** One thing in the options view: an option (setting) or a section heading (heading), at its place on screen. */
	private record Cell(Setting setting, String heading, int x, int y, int w) {
	}

	/**
	 * Where every option of the selected mod goes. A mod with many options gets the sections GENERAL and COLOUR,
	 * and two columns when the panel is wide enough.
	 */
	private List<Cell> cells() {
		List<Cell> out = new ArrayList<>();
		int left = px + PAD, full = contentRight() - left, top = viewTop() - rowScroll, y = top;
		boolean sections = selected.settings.size() > 6;
		int cols = sections && full >= 360 ? 2 : 1;
		int colW = (full - (cols - 1) * COLUMN_GAP) / cols;
		for (int pass = 0; pass < 2; pass++) {
			List<Setting> part = new ArrayList<>();
			for (Setting s : selected.settings) {
				if (sections ? s instanceof Setting.Color == (pass == 1) : pass == 0) part.add(s);
			}
			if (part.isEmpty()) continue;
			if (sections) {
				out.add(new Cell(null, pass == 0 ? "GENERAL" : "COLOUR", left, y, full));
				y += HEAD_H;
			}
			for (int i = 0; i < part.size(); i++) {
				out.add(new Cell(part.get(i), null, left + (i % cols) * (colW + COLUMN_GAP), y + (i / cols) * ROW_H, colW));
			}
			y += (part.size() + cols - 1) / cols * ROW_H;
		}
		rowsHeight = y - top;
		return out;
	}

	private static String keyName(Setting.Key key) {
		if (key.isMouse()) return InputConstants.Type.MOUSE.getOrCreate(key.value - Setting.Key.MOUSE).getDisplayName().getString();
		return key.value < 0 ? "None" : InputConstants.Type.KEYSYM.getOrCreate(key.value).getDisplayName().getString();
	}

	// ---- drawing ----

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// The game's own blur (strength follows Options > Accessibility > Menu Background Blur), no dark overlay.
		if (minecraft.level == null) renderPanorama(g, delta);
		renderBlurredBackground(g);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		Ui.panel(g, px, py, pw, ph);
		g.fill(px + 1, py + HEADER, px + pw - 1, py + HEADER + 1, FAINT);
		boolean hot = inView(mouseY);
		int titleY = py + (HEADER - 7) / 2;
		Ui.button(g, right() - BUTTON_H, headY(), BUTTON_H, BUTTON_H, "×", Ui.inside(mouseX, mouseY, right() - BUTTON_H, headY(), BUTTON_H, BUTTON_H), Ui.DARK, Ui.TITLE);
		int content;

		if (selected == null) {
			Ui.icon(g, px + PAD, py + (HEADER - 16) / 2, 16);
			Ui.text(g, Ui.fit("NEWGEN MODS", pw - PAD * 2 - 44, Ui.TITLE, false), px + PAD + 21, titleY, Ui.TEXT, Ui.TITLE);

			for (int i = 0; i < TABS.length; i++) button(g, chipX(i), barY(), chipW(i), TABS[i], mouseX, mouseY, i == tab ? Ui.BLUE : Ui.DARK);
			int sx = searchX(), sw = right() - sx;
			if (sw >= 30) {
				field(g, sx, barY(), sw, !search.isEmpty());
				if (search.isEmpty()) Ui.text(g, Ui.fit("Search...", sw - 12, Ui.UI, false), sx + 6, barY() + 3, Ui.DIM, Ui.UI);
				else Ui.text(g, Ui.fit(search + "_", sw - 12, Ui.UI, true), sx + 6, barY() + 3, Ui.TEXT, Ui.UI);
			}

			List<Mod> list = shown();
			content = (list.size() + columns - 1) / columns * (CARD_H + GAP) - GAP;
			gridScroll = limit(gridScroll, content);
			g.enableScissor(px, viewTop(), px + pw, viewBottom());
			for (int i = 0; i < list.size(); i++) {
				int y = cardY(i);
				if (y + CARD_H > viewTop() && y < viewBottom()) card(g, list.get(i), cardX(i), y, hot ? mouseX : -1, mouseY);
			}
			g.disableScissor();
			if (list.isEmpty()) Ui.centered(g, "No mods found", px + pw / 2, viewTop() + 8, Ui.DIM, Ui.UI);
			// Without a world there is no HUD to arrange.
			if (minecraft.level != null) button(g, px + PAD, footY(), footW("Edit HUD layout"), "Edit HUD layout", mouseX, mouseY, Ui.BLUE);
		} else {
			// The round back button.
			int bx = px + PAD, by = headY();
			Ui.round(g, bx, by, BUTTON_H, BUTTON_H, BUTTON_H / 2, Ui.DARK);
			Ui.ring(g, bx, by, BUTTON_H, BUTTON_H, BUTTON_H / 2, Ui.LINE);
			if (Ui.inside(mouseX, mouseY, bx, by, BUTTON_H, BUTTON_H)) Ui.round(g, bx, by, BUTTON_H, BUTTON_H, BUTTON_H / 2, Ui.HOVER);
			Ui.centered(g, "←", bx + BUTTON_H / 2, by + 3, Ui.TEXT, Ui.UI);
			int nameX = bx + BUTTON_H + 8, toggleX = right() - BUTTON_H - 4 - toggleW();
			Ui.text(g, Ui.fit(Ui.spaced(selected.name), toggleX - 8 - nameX, Ui.TITLE, false), nameX, titleY, Ui.TEXT, Ui.TITLE);
			button(g, toggleX, headY(), toggleW(), selected.enabled ? "Enabled" : "Disabled", mouseX, mouseY, selected.enabled ? Ui.GREEN : Ui.RED);
			Ui.text(g, Ui.fit(selected.description, pw - PAD * 2, Ui.UI, false), px + PAD, barY() + 3, Ui.DIM, Ui.UI);

			List<Cell> cells = cells();
			content = rowsHeight;
			if (rowScroll != limit(rowScroll, content)) {
				rowScroll = limit(rowScroll, content);
				cells = cells();
			}
			g.enableScissor(px, viewTop(), px + pw, viewBottom());
			for (Cell cell : cells) {
				if (cell.y + ROW_H <= viewTop() || cell.y >= viewBottom()) continue;
				if (cell.setting != null) {
					row(g, cell, hot ? mouseX : -1, mouseY);
				} else {
					Ui.text(g, cell.heading, cell.x, cell.y + 4, Ui.TEXT, Ui.CAPS);
					g.fill(cell.x + Ui.width(cell.heading, Ui.CAPS) + 6, cell.y + 7, cell.x + cell.w, cell.y + 8, FAINT);
				}
			}
			g.disableScissor();
			if (selected.settings.isEmpty()) Ui.centered(g, "No options", px + pw / 2, viewTop() + 8, Ui.DIM, Ui.UI);
			button(g, px + PAD, footY(), footW("Reset to defaults"), "Reset to defaults", mouseX, mouseY, Ui.DARK);
		}

		int viewH = viewBottom() - viewTop(), max = content - viewH;
		if (max > 0) {
			int scroll = selected == null ? gridScroll : rowScroll, h = Math.max(8, viewH * viewH / content);
			int y = viewTop() + (viewH - h) * scroll / max;
			Ui.round(g, right() - 3, viewTop(), 3, viewH, 1, FAINT);
			Ui.round(g, right() - 3, y, 3, h, 1, 0x80FFFFFF);
		}
	}

	private void card(GuiGraphics g, Mod mod, int x, int y, int mouseX, int mouseY) {
		int w = cardW(), options = y + NAME_H, status = options + STRIP;
		Ui.round(g, x, y, w, CARD_H, 5, Ui.CARD);
		Ui.centered(g, Ui.fit(mod.name, w - 8, Ui.UI, false), x + w / 2, y + (NAME_H - 7) / 2, Ui.SOFT, Ui.UI);
		g.fill(x, options, x + w, status, 0x55000000);
		g.fill(x, options, x + w, options + 1, FAINT);
		if (Ui.inside(mouseX, mouseY, x, y, w, NAME_H + STRIP)) g.fill(x, options, x + w, status, Ui.HOVER);
		Ui.centered(g, Ui.spaced("Options"), x + w / 2, options + 3, Ui.TEXT, Ui.CAPS);
		// The strip that closes the card: its bottom corners are the card's.
		Ui.roundBottom(g, x, status, w, STRIP, 5, mod.enabled ? Ui.GREEN : Ui.RED);
		if (Ui.inside(mouseX, mouseY, x, status, w, STRIP)) Ui.roundBottom(g, x, status, w, STRIP, 5, Ui.HOVER);
		Ui.centered(g, Ui.spaced(mod.enabled ? "Enabled" : "Disabled"), x + w / 2, status + 3, Ui.TEXT, Ui.CAPS);
		Ui.ring(g, x, y, w, CARD_H, 5, Ui.LINE);
	}

	private void row(GuiGraphics g, Cell cell, int mouseX, int mouseY) {
		Setting setting = cell.setting;
		int x = cell.x, y = cell.y, cr = x + cell.w, controlX = cr - fieldW(cell.w);
		if (Ui.inside(mouseX, mouseY, x - 3, y, cell.w + 6, ROW_H)) Ui.round(g, x - 3, y + 1, cell.w + 6, ROW_H - 2, 4, 0x10FFFFFF);

		if (setting instanceof Setting.Bool bool) {
			controlX = cr - PILL_W;
			// An ON/OFF pill: the word says the state, colour and knob side repeat it.
			Ui.round(g, controlX, y + 4, PILL_W, PILL_H, PILL_H / 2, bool.value ? Ui.GREEN : Ui.RED);
			Ui.round(g, bool.value ? cr - 10 : controlX + 2, y + 6, 8, 8, 4, KNOB);
			Ui.text(g, bool.value ? "ON" : "OFF", controlX + (bool.value ? 6 : 13), y + 6, Ui.TEXT, Ui.CAPS);
		} else if (setting instanceof Setting.Num num) {
			// The knob's centre runs from 4 pixels inside one end of the track to 4 inside the other (see the click).
			int sw = sliderW(cell.w), sx = cr - sw;
			int knob = sx + 4 + (int) ((num.get() - num.min) / (num.max - num.min) * (sw - 8));
			Ui.round(g, sx, y + 9, sw, 3, 1, 0x70000000);
			Ui.round(g, sx, y + 9, knob - sx, 3, 1, Ui.BLUE);
			Ui.round(g, knob - 4, y + 6, 8, 8, 4, Ui.BLUE);
			controlX = sx - 8 - Ui.width(num.text(), Ui.CAPS);
			Ui.text(g, num.text(), controlX, y + 6, Ui.SOFT, Ui.CAPS);
		} else if (setting instanceof Setting.Color color) {
			// Round swatch on a grey base, so a see-through colour looks see-through; next to it the code, which can be typed.
			int hx = cr - HEX_W, sx = hx - 5 - SWATCH;
			controlX = sx - 1;
			Ui.round(g, sx, y + 5, SWATCH, SWATCH, SWATCH / 2, 0xFF8C8C8C);
			Ui.round(g, sx, y + 5, SWATCH, SWATCH, SWATCH / 2, color.value);
			Ui.ring(g, sx - 1, y + 4, SWATCH + 2, SWATCH + 2, SWATCH / 2 + 1, 0xC0FFFFFF);
			field(g, hx, y + 3, HEX_W, color == typing);
			Ui.text(g, color == typing ? "#" + hex + "_" : String.format("#%08X", color.value), hx + 6, y + 6, Ui.SOFT, Ui.UI);
		} else {
			int w = cr - controlX;
			boolean active = setting == listening || setting == typing, over = Ui.inside(mouseX, mouseY, controlX, y + 3, w, BUTTON_H);
			if (setting instanceof Setting.Text text) {
				String value = text.value == null ? "" : text.value;
				field(g, controlX, y + 3, w, active);
				Ui.text(g, Ui.fit(active ? value + "_" : value, w - 12, Ui.UI, true), controlX + 6, y + 6, Ui.TEXT, Ui.UI);
			} else if (setting instanceof Setting.Choice choice) {
				// No box: the value between two blue arrows.
				if (over) Ui.round(g, controlX, y + 3, w, BUTTON_H, 4, Ui.HOVER);
				Ui.text(g, "‹", controlX + 5, y + 6, Ui.BLUE, Ui.TITLE);
				Ui.text(g, "›", cr - 5 - Ui.width("›", Ui.TITLE), y + 6, Ui.BLUE, Ui.TITLE);
				Ui.centered(g, Ui.fit(String.valueOf(choice.value), w - 30, Ui.UI, false), controlX + w / 2, y + 6, Ui.SOFT, Ui.UI);
			} else if (setting instanceof Setting.Key key) {
				Ui.button(g, controlX, y + 3, w, BUTTON_H, Ui.spaced(key == listening ? "Key or mouse" : keyName(key)), over, active ? Ui.BLUE : Ui.DARK, Ui.CAPS);
			}
		}
		Ui.text(g, Ui.fit(setting.name, controlX - 6 - x, Ui.UI, false), x, y + 6, Ui.SOFT, Ui.UI);
	}

	/** A rounded button of the usual height with its label in spaced capitals. */
	private void button(GuiGraphics g, int x, int y, int w, String label, int mouseX, int mouseY, int fill) {
		Ui.button(g, x, y, w, BUTTON_H, Ui.spaced(label), Ui.inside(mouseX, mouseY, x, y, w, BUTTON_H), fill, Ui.CAPS);
	}

	/** A rounded typing field; the outline turns blue while it holds text or is being typed into. */
	private void field(GuiGraphics g, int x, int y, int w, boolean active) {
		Ui.round(g, x, y, w, BUTTON_H, 4, Ui.DARK);
		Ui.ring(g, x, y, w, BUTTON_H, 4, active ? Ui.BLUE : Ui.LINE);
	}

	// ---- input ----

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		boolean left = event.button() == 0;
		if (listening != null && event.button() >= 0 && event.button() < 8) {
			listening.value = Setting.Key.MOUSE + event.button(); // a click while capturing binds that mouse button
			listening = null;
			Mods.save();
			return true;
		}
		if (!left && event.button() != 1) return false;
		// Any click ends key capture and typing; the clicked row may start it again below.
		listening = null;
		typing = null;

		if (left && Ui.inside(mx, my, right() - BUTTON_H, headY(), BUTTON_H, BUTTON_H)) {
			onClose();
			return true;
		}
		if (selected == null) {
			if (!left) return true;
			if (minecraft.level != null && Ui.inside(mx, my, px + PAD, footY(), footW("Edit HUD layout"), BUTTON_H)) {
				minecraft.setScreen(new HubScreen());
				return true;
			}
			for (int i = 0; i < TABS.length; i++) {
				if (Ui.inside(mx, my, chipX(i), barY(), chipW(i), BUTTON_H)) {
					tab = i;
					gridScroll = 0;
				}
			}
			if (!inView(my)) return true;
			List<Mod> list = shown();
			for (int i = 0; i < list.size(); i++) {
				Mod mod = list.get(i);
				int x = cardX(i), y = cardY(i);
				if (!Ui.inside(mx, my, x, y, cardW(), CARD_H)) continue;
				if (my >= y + NAME_H + STRIP) {
					mod.enabled = !mod.enabled;
					Mods.save();
				} else {
					selected = mod;
					rowScroll = 0;
				}
			}
			return true;
		}

		if (left && Ui.inside(mx, my, px + PAD, headY(), BUTTON_H, BUTTON_H)) {
			selected = null;
		} else if (left && Ui.inside(mx, my, right() - BUTTON_H - 4 - toggleW(), headY(), toggleW(), BUTTON_H)) {
			selected.enabled = !selected.enabled;
			Mods.save();
		} else if (left && Ui.inside(mx, my, px + PAD, footY(), footW("Reset to defaults"), BUTTON_H)) {
			selected.reset();
			Mods.save();
		} else if (inView(my)) {
			for (Cell cell : cells()) {
				Setting setting = cell.setting;
				if (setting == null || !Ui.inside(mx, my, cell.x, cell.y, cell.w, ROW_H)) continue;
				int cr = cell.x + cell.w, fieldX = cr - fieldW(cell.w);
				if (setting instanceof Setting.Choice choice && mx >= fieldX) {
					// The "<" end or a right click steps back, anywhere else steps forward.
					int n = choice.options.length, at = 0;
					while (at < n && !choice.options[at].equals(choice.value)) at++;
					if (n > 0) choice.value = choice.options[(at + (left && mx >= fieldX + 14 ? 1 : n - 1)) % n];
					Mods.save();
				} else if (!left) {
					continue;
				} else if (setting instanceof Setting.Bool bool && mx >= cr - PILL_W - 4) {
					bool.value = !bool.value;
					Mods.save();
				} else if (setting instanceof Setting.Num num && mx >= cr - sliderW(cell.w) - 4) {
					dragging = num;
					dragW = sliderW(cell.w) - 8;
					dragX = cr - dragW - 4;
					slide(mx);
				} else if (setting instanceof Setting.Color color && mx >= cr - HEX_W) {
					typing = color;
					hex = String.format("%08X", color.value);
				} else if (setting instanceof Setting.Color color && mx >= cr - HEX_W - 8 - SWATCH) {
					// Next quick colour; how see-through it is stays as it was.
					int at = 0;
					while (at < PALETTE.length && PALETTE[at] != (color.value & 0xFFFFFF)) at++;
					color.value = color.value & 0xFF000000 | PALETTE[(at + 1) % PALETTE.length];
					Mods.save();
				} else if (setting instanceof Setting.Key key && mx >= fieldX) {
					listening = key;
				} else if (setting instanceof Setting.Text && mx >= fieldX) {
					typing = setting;
				}
			}
		}
		return true;
	}

	private void slide(double mouseX) {
		dragging.set(dragging.min + (mouseX - dragX) / dragW * (dragging.max - dragging.min));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) return false;
		slide(event.x());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			Mods.save();
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		// render() clamps both values every frame, so no limits are needed here.
		if (selected == null) gridScroll -= (int) (scrollY * 25);
		else rowScroll -= (int) (scrollY * ROW_H);
		return true;
	}

	/** Puts typed text into the option being typed into. A colour takes hex digits: 6 (solid) or 8 (#AARRGGBB). */
	private void type(String added, boolean backspace) {
		if (typing instanceof Setting.Text text) {
			String value = text.value == null ? "" : text.value;
			if (backspace) value = dropLast(value);
			for (char c : added.toCharArray()) {
				if (c >= ' ' && c != 127 && value.length() < 64) value += c;
			}
			text.value = value;
		} else if (typing instanceof Setting.Color color) {
			if (backspace) hex = dropLast(hex);
			for (char c : added.toUpperCase().toCharArray()) {
				if (c < 128 && Character.digit(c, 16) >= 0 && hex.length() < 8) hex += c;
			}
			if (hex.length() == 8) color.value = (int) Long.parseLong(hex, 16);
			else if (hex.length() == 6) color.value = 0xFF000000 | Integer.parseInt(hex, 16);
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (listening != null) {
			listening.value = key == GLFW.GLFW_KEY_ESCAPE ? -1 : key; // Esc clears the binding
			listening = null;
			Mods.save();
			return true;
		}
		if (typing != null) {
			if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				typing = null;
				Mods.save();
			} else if (key == GLFW.GLFW_KEY_BACKSPACE) {
				type("", true);
			} else if (event.isPaste()) {
				if (typing instanceof Setting.Color) hex = ""; // a pasted code replaces the old one
				type(minecraft.keyboardHandler.getClipboard(), false);
			}
			return true; // while typing, no key may close the menu
		}
		if (key == GLFW.GLFW_KEY_ESCAPE && selected != null) {
			selected = null; // Esc in the options view goes back to the grid first
			return true;
		}
		if (selected == null && key == GLFW.GLFW_KEY_BACKSPACE) {
			search = dropLast(search);
			return true;
		}
		// In the grid, keys that type a character belong to the search box, even if one is bound to this menu.
		boolean types = selected == null && key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_GRAVE_ACCENT;
		if (!types && NewgenClient.OPEN_MODS.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!event.isAllowedChatCharacter() || listening != null) return false;
		if (typing != null) {
			type(event.codepointAsString(), false);
		} else if (selected == null && search.length() < 32) {
			search += event.codepointAsString();
			gridScroll = 0;
		}
		return true;
	}

	private static String dropLast(String s) {
		return s.isEmpty() ? s : s.substring(0, s.offsetByCodePoints(s.length(), -1));
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public void removed() {
		Mods.save();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

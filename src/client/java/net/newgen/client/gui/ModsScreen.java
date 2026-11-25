package net.newgen.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
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
 * The "Newgen Mods" menu: one centred glass panel over the game (or the panorama from the title screen).
 * <pre>
 * [icon] NewGen   [Mods] [Options]                         [x]
 * ------------------------------------------------------------
 * | All      74 |  [All] [On] [Off]   [search............]  |
 * | HUD      30 |  +--------+ +--------+ +--------+          |
 * | Visual   20 |  |  icon  | |  icon  | |  icon  |          |
 * | Controls  6 |  |  name  | |  name  | |  name  |          |
 * | Utility  18 |  |Options*| |Options*| |Options*|          |
 * |             |  |Enabled | |Disabled| |Enabled |          |
 * |[Edit HUD]   |  +--------+ +--------+ +--------+          |
 * </pre>
 * The categories on the left filter the grid, the chips filter by state, the search by name. A card's Options strip
 * (or its icon) opens the mod's options in the same area; its last strip turns it on (blue) or off (neutral).
 * Everything is drawn by hand; the x/y helpers are shared by drawing and clicking so the two never disagree.
 */
public class ModsScreen extends Screen {
	/** Quick colours: a click on a colour swatch steps through these. */
	private static final int[] PALETTE = {
			0xFFFFFF, 0xAAAAAA, 0x000000, 0xFF5555, 0xFFAA00, 0xFFFF55,
			0x55FF55, 0x55FFFF, 0x5599FF, 0xAA55FF, 0xFF77CC};
	/** The categories (Mod.category values) and their icons. */
	private static final String[] TABS = {"All", "HUD", "Visual", "Controls", "Utility"};
	private static final int[] TAB_ICONS = {Ui.I_GRID, Ui.I_LAYOUT, Ui.I_EYE, Ui.I_MOUSE, Ui.I_SLIDERS};
	/** The state chips over the grid. */
	private static final String[] STATES = {"state.all", "state.on", "state.off"};

	private static final int TOAST_H = 32, PAD = 8, HEADER = 28, SIDE_W = 92, SIDE_ROW = 16, GAP = 6, ROW_H = 20, HEAD_H = 14;
	/** A card: icon and name (TOP_H), then the Options strip and the Enabled/Disabled strip (STRIP each). */
	private static final int TOP_H = 38, STRIP = 14, CARD_H = TOP_H + STRIP * 2;
	private static final int PILL_W = 20, SWATCH = 10, HEX_W = 66, BUTTON_H = 14, COLUMN_GAP = 14;
	/** FAINT is for dividing lines and the scroll track, STRIP_BG under a card's Options strip. */
	private static final int FAINT = 0x18FFFFFF, STRIP_BG = 0x14000000;

	private static int tab;           // static: coming back from the Right Shift screen lands on the same category
	private static int state;         // 0 all, 1 on, 2 off

	private final Screen parent;      // where Esc goes; null = back into the game
	private int px, py, pw, ph;       // panel position and size
	private int columns;
	private String search = "";
	private int gridScroll, rowScroll; // pixels scrolled in each view
	private int rowsHeight;           // height of all option rows, set by cells()
	private Mod selected;             // null = card grid, otherwise the mod whose options are shown
	private int focus = -1;           // card that has the keyboard focus in the grid (Tab), -1 = none
	private int rowFocus = -1;        // option row with the keyboard focus in the options view
	private Mod rowMod;               // the mod rowFocus belongs to
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
		// Vanilla's tutorial toast ("Move with W, A, S and D") sits where our panels are; it goes. Other toasts stay.
		Ui.hideTutorialToasts();
		// 20 units of margin at every scale; the top stays below the 32 high toast column (tutorial hints) so the close
		// button is never under a toast.
		// Never below a usable minimum: on a tiny window or a huge GUI scale the panel overflows rather than turning negative.
		pw = Math.max(240, Math.min(500, width - 40));
		ph = Math.max(120, Math.min(310, height - TOAST_H - 12));
		px = (width - pw) / 2;
		py = Math.max(TOAST_H + 4, (height - ph) / 2);
		columns = Math.max(1, (contentRight() - contentX() + GAP) / (96 + GAP));
	}

	// ---- layout helpers ----

	private int right() {
		return px + pw - PAD;
	}

	/** Left edge of the grid / options area, right of the category column. */
	private int contentX() {
		return px + PAD + SIDE_W + PAD;
	}

	/** Right edge of the scrolling content; leaves room for the scroll bar. */
	private int contentRight() {
		return right() - 7;
	}

	private int headY() {
		return py + (HEADER - BUTTON_H) / 2;
	}

	/** Top of the row under the header: the chips and search, or the back button, name and switch. */
	private int barY() {
		return py + HEADER + PAD;
	}

	private int viewTop() {
		return barY() + BUTTON_H + (selected == null ? GAP : 18);
	}

	private int footY() {
		return py + ph - PAD - BUTTON_H;
	}

	private int viewBottom() {
		return selected == null ? py + ph - PAD : footY() - 4;
	}

	private boolean inView(double my) {
		return my >= viewTop() && my < viewBottom();
	}

	private int cardW() {
		return (contentRight() - contentX() - GAP * (columns - 1)) / columns;
	}

	private int cardX(int i) {
		return contentX() + (i % columns) * (cardW() + GAP);
	}

	private int cardY(int i) {
		return viewTop() - gridScroll + (i / columns) * (CARD_H + GAP);
	}

	private int sideRowY(int i) {
		return barY() + i * (SIDE_ROW + 2);
	}

	private int stateW(int i) {
		return Ui.width(Ui.tr(STATES[i]), Ui.CAPS) + 14;
	}

	private int stateX(int i) {
		int x = contentX();
		for (int k = 0; k < i; k++) x += stateW(k) + 4;
		return x;
	}

	private int searchX() {
		return stateX(STATES.length) + 2;
	}

	/** A header tab sized to its label (and its icon, 10 + 5 wide, when it has one). */
	private static int tabW(String label, boolean icon) {
		return Ui.width(label, Ui.CAPS) + 16 + (icon ? 15 : 0);
	}

	/** A mod's name and description in the game's language (newgen_client.mod.<id>.name / .description), else as written. */
	private static String modName(Mod mod) {
		return translated("newgen_client.mod." + mod.id + ".name", mod.name);
	}

	private static String modDescription(Mod mod) {
		return translated("newgen_client.mod." + mod.id + ".description", mod.description);
	}

	private static String translated(String key, String fallback) {
		return net.minecraft.client.resources.language.I18n.exists(key) ? net.minecraft.client.resources.language.I18n.get(key) : fallback;
	}

	/** An option's label in the game's language when the lang files have it (newgen_client.setting.<label>), else as written. */
	private static String settingLabel(String name) {
		String key = "newgen_client.setting." + name.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
		return net.minecraft.client.resources.language.I18n.exists(key) ? net.minecraft.client.resources.language.I18n.get(key) : name;
	}

	/** A choice's value in the game's language when the lang files have it (newgen_client.choice.<value>), else as written. */
	private static String choiceLabel(String value) {
		String key = "newgen_client.choice." + value.toLowerCase().replace(' ', '_').replace("'", "");
		return net.minecraft.client.resources.language.I18n.exists(key) ? net.minecraft.client.resources.language.I18n.get(key) : value;
	}

	/** The game's own word for its options, so the title screen and this menu spell it the same. */
	private static String optionsLabel() {
		return net.minecraft.client.resources.language.I18n.get("menu.options").replace("...", "").trim();
	}

	/** The header tabs: Mods (this screen) and Options (the game's own options), after the name. */
	private int tabX(int i) {
		int x = px + PAD + 18 + Ui.width("NewGen", Ui.TITLE) + 14;
		return i == 0 ? x : x + tabW(Ui.tr("mods"), false) + 4;
	}

	/** Width of the bottom-left button: "Reset to defaults". */
	private int footW(String label) {
		return Math.min(Ui.width(label, Ui.CAPS) + 20, right() - contentX());
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

	private static boolean inTab(Mod mod, int t) {
		return t == 0 || TABS[t].equalsIgnoreCase(mod.category);
	}

	private static int tabIcon(Mod mod) {
		for (int t = 1; t < TABS.length; t++) if (TABS[t].equalsIgnoreCase(mod.category)) return TAB_ICONS[t];
		return Ui.I_GRID;
	}

	/** The mods of the current category and state that match the search text. */
	private List<Mod> shown() {
		List<Mod> list = new ArrayList<>();
		String query = search.toLowerCase();
		for (Mod mod : Mods.ALL) {
			if (!inTab(mod, tab) || state == 1 && !mod.enabled || state == 2 && mod.enabled) continue;
			if ((mod.name + " " + mod.description + " " + modName(mod) + " " + modDescription(mod)).toLowerCase().contains(query)) list.add(mod);
		}
		return list;
	}

	/** One thing in the options view: an option (setting) or a section heading (heading), at its place on screen. */
	private record Cell(Setting setting, String heading, int x, int y, int w) {
	}

	/**
	 * Where every option of the selected mod goes. A mod with many options gets the sections General and Colour,
	 * and two columns when the area is wide enough.
	 */
	private List<Cell> cells() {
		List<Cell> out = new ArrayList<>();
		int left = contentX(), full = contentRight() - left, top = viewTop() - rowScroll, y = top;
		boolean sections = selected.settings.size() > 6;
		int cols = sections && full >= 300 ? 2 : 1;
		int colW = (full - (cols - 1) * COLUMN_GAP) / cols;
		for (int pass = 0; pass < 2; pass++) {
			List<Setting> part = new ArrayList<>();
			for (Setting s : selected.settings) {
				if (s instanceof Setting.Hidden) continue;
				if (sections ? s instanceof Setting.Color == (pass == 1) : pass == 0) part.add(s);
			}
			if (part.isEmpty()) continue;
			if (sections) {
				out.add(new Cell(null, Ui.tr(pass == 0 ? "section.general" : "section.colour"), left, y, full));
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
		return key.value < 0 ? Ui.tr("key.none") : InputConstants.Type.KEYSYM.getOrCreate(key.value).getDisplayName().getString();
	}

	// ---- drawing ----

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// The game's own blur (strength follows Options > Accessibility > Menu Background Blur), no dark overlay.
		if (minecraft.level == null && !VideoBackground.render(g, width, height)) renderPanorama(g, delta);
		renderBlurredBackground(g);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		Ui.panel(g, px, py, pw, ph);
		g.fill(px + 1, py + HEADER, px + pw - 1, py + HEADER + 1, FAINT);

		// Header: icon and name, the two tabs, the close button.
		Ui.icon(g, px + PAD, py + (HEADER - 14) / 2, 14);
		Ui.text(g, "NewGen", px + PAD + 18, py + (HEADER - 7) / 2, Ui.TEXT, Ui.TITLE);
		String mods = Ui.tr("mods"), options = optionsLabel();
		Ui.button(g, tabX(0), headY(), tabW(mods, false), BUTTON_H, mods, false, Ui.SELECTED, Ui.CAPS);
		Ui.button(g, tabX(1), headY(), tabW(options, true), BUTTON_H, options, Ui.I_GEAR,
				Ui.inside(mouseX, mouseY, tabX(1), headY(), tabW(options, true), BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);
		Ui.button(g, right() - BUTTON_H, headY(), BUTTON_H, BUTTON_H, "", Ui.I_CLOSE,
				Ui.inside(mouseX, mouseY, right() - BUTTON_H, headY(), BUTTON_H, BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);

		// Left column: the categories with their counts, and Edit HUD layout at its foot.
		int sx = px + PAD;
		Ui.round(g, sx, barY() - 2, SIDE_W, py + ph - PAD - (barY() - 2), Ui.R_MD, Ui.CARD);
		for (int t = 0; t < TABS.length; t++) {
			int y = sideRowY(t);
			boolean on = selected == null && t == tab, over = Ui.inside(mouseX, mouseY, sx + 2, y, SIDE_W - 4, SIDE_ROW);
			if (on || over) Ui.round(g, sx + 2, y, SIDE_W - 4, SIDE_ROW, Ui.R_SM, on ? Ui.SELECTED : Ui.HOVER);
			if (on) Ui.round(g, sx + 2, y + 4, 2, SIDE_ROW - 8, 1, Ui.ACCENT); // the accent as a small mark
			Ui.glyph(g, TAB_ICONS[t], sx + 7, y + 4, 8, on ? Ui.TEXT : Ui.DIM);
			Ui.text(g, Ui.tr("tab." + TABS[t].toLowerCase()), sx + 20, y + 5, on ? Ui.TEXT : Ui.SOFT, Ui.CAPS);
			int count = 0;
			for (Mod mod : Mods.ALL) if (inTab(mod, t)) count++;
			String n = String.valueOf(count);
			Ui.text(g, n, sx + SIDE_W - 7 - Ui.width(n, Ui.UI), y + 5, Ui.DIM, Ui.UI);
		}
		// Without a world there is no HUD to arrange.
		if (minecraft.level != null) {
			int by = py + ph - PAD - 4 - BUTTON_H;
			String edit = Ui.tr("hud.edit");
			Ui.button(g, sx + 4, by, SIDE_W - 8, BUTTON_H, edit, Ui.I_LAYOUT,
					Ui.inside(mouseX, mouseY, sx + 4, by, SIDE_W - 8, BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);
		}

		boolean hot = inView(mouseY);
		int content;
		if (selected == null) {
			for (int i = 0; i < STATES.length; i++) {
				String label = Ui.tr(STATES[i]);
				Ui.button(g, stateX(i), barY(), stateW(i), BUTTON_H, label, Ui.inside(mouseX, mouseY, stateX(i), barY(), stateW(i), BUTTON_H),
						i == state ? Ui.SELECTED : Ui.DARK, Ui.CAPS);
			}
			int fx = searchX(), fw = right() - fx;
			if (fw >= 40) {
				field(g, fx, barY(), fw, !search.isEmpty());
				Ui.glyph(g, Ui.I_SEARCH, fx + 5, barY() + 3, 8, Ui.DIM);
				if (search.isEmpty()) Ui.text(g, Ui.fit(Ui.tr("search"), fw - 24, Ui.UI, false), fx + 17, barY() + 3, Ui.DIM, Ui.UI);
				else Ui.text(g, Ui.fit(search + "_", fw - 24, Ui.UI, true), fx + 17, barY() + 3, Ui.TEXT, Ui.UI);
			}

			List<Mod> list = shown();
			content = (list.size() + columns - 1) / columns * (CARD_H + GAP) - GAP;
			gridScroll = limit(gridScroll, content);
			if (viewBottom() > viewTop()) g.enableScissor(contentX(), viewTop(), right(), viewBottom());
			for (int i = 0; i < list.size(); i++) {
				int y = cardY(i);
				if (y + CARD_H > viewTop() && y < viewBottom()) card(g, list.get(i), cardX(i), y, hot ? mouseX : -1, mouseY);
				if (i == focus && y + CARD_H > viewTop() && y < viewBottom()) Ui.ring(g, cardX(i) - 1, y - 1, cardW() + 2, CARD_H + 2, Ui.R_MD + 1, Ui.ACCENT);
			}
			if (viewBottom() > viewTop()) g.disableScissor();
			if (list.isEmpty()) Ui.centered(g, Ui.tr("mods.none"), (contentX() + right()) / 2, viewTop() + 12, Ui.DIM, Ui.UI);
		} else {
			// The options of one mod: back, its name and its switch; its description under them.
			int bx = contentX(), by = barY();
			Ui.button(g, bx, by, BUTTON_H, BUTTON_H, "", Ui.I_BACK, Ui.inside(mouseX, mouseY, bx, by, BUTTON_H, BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);
			int nameX = bx + BUTTON_H + 8, toggleX = right() - PILL_W;
			Ui.text(g, Ui.fit(modName(selected), toggleX - 8 - nameX, Ui.TITLE, false), nameX, by + 3, Ui.TEXT, Ui.TITLE);
			Ui.toggle(g, toggleX, by + 2, selected.enabled, Ui.inside(mouseX, mouseY, toggleX - 4, by, PILL_W + 4, BUTTON_H));
			Ui.text(g, Ui.fit(modDescription(selected), right() - bx, Ui.UI, false), bx, by + BUTTON_H + 5, Ui.DIM, Ui.UI);

			List<Cell> cells = cells();
			content = rowsHeight;
			if (rowScroll != limit(rowScroll, content)) {
				rowScroll = limit(rowScroll, content);
				cells = cells();
			}
			if (viewBottom() > viewTop()) g.enableScissor(contentX() - 4, viewTop(), right(), viewBottom());
			for (Cell cell : cells) {
				if (cell.y + ROW_H <= viewTop() || cell.y >= viewBottom()) continue;
				if (cell.setting != null) {
					row(g, cell, hot ? mouseX : -1, mouseY);
					if (rowMod == selected && rowFocus >= 0 && rowFocus < rows().size() && rows().get(rowFocus) == cell.setting)
						Ui.ring(g, cell.x - 3, cell.y + 1, cell.w + 6, ROW_H - 2, Ui.R_SM, Ui.ACCENT);
				} else {
					Ui.text(g, cell.heading, cell.x, cell.y + 4, Ui.TEXT, Ui.CAPS);
					g.fill(cell.x + Ui.width(cell.heading, Ui.CAPS) + 6, cell.y + 7, cell.x + cell.w, cell.y + 8, FAINT);
				}
			}
			if (viewBottom() > viewTop()) g.disableScissor();
			if (selected.settings.isEmpty()) Ui.centered(g, Ui.tr("options.none"), (contentX() + right()) / 2, viewTop() + 8, Ui.DIM, Ui.UI);
			button(g, contentX(), footY(), footW(Ui.tr("reset")), Ui.tr("reset"), mouseX, mouseY, Ui.DARK);
		}

		int viewH = viewBottom() - viewTop(), max = content - viewH;
		if (max > 0) {
			int scroll = selected == null ? gridScroll : rowScroll, h = Math.max(8, viewH * viewH / content);
			int y = viewTop() + (viewH - h) * scroll / max;
			Ui.round(g, right() - 3, viewTop(), 3, viewH, 1, FAINT);
			Ui.round(g, right() - 3, y, 3, h, 1, 0x80FFFFFF);
		}
	}

	/** A card: the category icon and the name, then an Options strip with a gear, then the Enabled / Disabled strip. */
	private void card(GuiGraphics g, Mod mod, int x, int y, int mouseX, int mouseY) {
		int w = cardW(), opt = y + TOP_H, status = opt + STRIP;
		Ui.round(g, x, y, w, CARD_H, Ui.R_MD, Ui.CARD);
		if (Ui.inside(mouseX, mouseY, x, y, w, TOP_H)) Ui.round(g, x, y, w, CARD_H, Ui.R_MD, Ui.HOVER);
		Ui.glyph(g, tabIcon(mod), x + (w - 14) / 2, y + 7, 14, mod.enabled ? Ui.TEXT : Ui.DIM);
		Ui.centered(g, Ui.fit(modName(mod), w - 10, Ui.CAPS, false), x + w / 2, y + 26, mod.enabled ? Ui.TEXT : Ui.SOFT, Ui.CAPS);

		g.fill(x, opt, x + w, status, STRIP_BG);
		g.fill(x, opt, x + w, opt + 1, FAINT);
		if (Ui.inside(mouseX, mouseY, x, opt, w, STRIP)) g.fill(x, opt, x + w, status, Ui.HOVER);
		String options = optionsLabel();
		int ow = Ui.width(options, Ui.CAPS) + 12, ox = x + (w - ow) / 2;
		Ui.text(g, options, ox, opt + 4, Ui.SOFT, Ui.CAPS);
		Ui.glyph(g, Ui.I_GEAR, ox + ow - 8, opt + 3, 8, Ui.SOFT);

		// The strip that closes the card: brighter glass with a small blue dot when on, plain when off; no colour block.
		if (mod.enabled) Ui.roundBottom(g, x, status, w, STRIP, Ui.R_MD, Ui.SELECTED);
		if (Ui.inside(mouseX, mouseY, x, status, w, STRIP)) Ui.roundBottom(g, x, status, w, STRIP, Ui.R_MD, Ui.HOVER);
		String state = Ui.tr(mod.enabled ? "enabled" : "disabled");
		int sw = Ui.width(state, Ui.CAPS) + (mod.enabled ? 7 : 0), sx = x + (w - sw) / 2;
		if (mod.enabled) Ui.round(g, sx, status + 5, 4, 4, 2, Ui.ACCENT);
		Ui.text(g, state, sx + (mod.enabled ? 7 : 0), status + 4, mod.enabled ? Ui.TEXT : Ui.DIM, Ui.CAPS);
		Ui.ring(g, x, y, w, CARD_H, Ui.R_MD, Ui.LINE);
	}

	private void row(GuiGraphics g, Cell cell, int mouseX, int mouseY) {
		Setting setting = cell.setting;
		int x = cell.x, y = cell.y, cr = x + cell.w, controlX = cr - fieldW(cell.w);
		if (Ui.inside(mouseX, mouseY, x - 3, y, cell.w + 6, ROW_H)) Ui.round(g, x - 3, y + 1, cell.w + 6, ROW_H - 2, 4, 0x10FFFFFF);

		if (setting instanceof Setting.Bool bool) {
			controlX = cr - PILL_W;
			// The same switch as on the cards: blue track and knob right when on.
			Ui.toggle(g, controlX, y + 5, bool.value, Ui.inside(mouseX, mouseY, controlX - 4, y, PILL_W + 4, ROW_H));
		} else if (setting instanceof Setting.Num num) {
			// The knob's centre runs from 4 pixels inside one end of the track to 4 inside the other (see the click).
			int sw = sliderW(cell.w), sx = cr - sw;
			int knob = sx + 4 + (int) ((num.get() - num.min) / (num.max - num.min) * (sw - 8));
			Ui.round(g, sx, y + 9, sw, 3, 1, 0x70000000);
			Ui.round(g, sx, y + 9, knob - sx, 3, 1, Ui.ACCENT);
			Ui.round(g, knob - 4, y + 6, 8, 8, 4, Ui.ACCENT);
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
				Ui.text(g, "‹", controlX + 5, y + 6, Ui.SOFT, Ui.TITLE);
				Ui.text(g, "›", cr - 5 - Ui.width("›", Ui.TITLE), y + 6, Ui.SOFT, Ui.TITLE);
				Ui.centered(g, Ui.fit(choiceLabel(choice.value), w - 30, Ui.UI, false), controlX + w / 2, y + 6, Ui.SOFT, Ui.UI);
			} else if (setting instanceof Setting.Action action) {
				Ui.button(g, controlX, y + 3, w, BUTTON_H, net.minecraft.client.resources.language.I18n.get(action.label), over, Ui.DARK, Ui.CAPS);
			} else if (setting instanceof Setting.Key key) {
				Ui.button(g, controlX, y + 3, w, BUTTON_H, key == listening ? Ui.tr("key.listening") : keyName(key), over, active ? Ui.SELECTED : Ui.DARK, Ui.CAPS);
			}
		}
		Ui.text(g, Ui.fit(settingLabel(setting.name), controlX - 6 - x, Ui.UI, false), x, y + 6, Ui.SOFT, Ui.UI);
	}

	/** A rounded button of the usual height with its label in the label face. */
	private void button(GuiGraphics g, int x, int y, int w, String label, int mouseX, int mouseY, int fill) {
		Ui.button(g, x, y, w, BUTTON_H, label, Ui.inside(mouseX, mouseY, x, y, w, BUTTON_H), fill, Ui.CAPS);
	}

	/** A rounded typing field; the outline turns blue while it holds text or is being typed into. */
	private void field(GuiGraphics g, int x, int y, int w, boolean active) {
		Ui.round(g, x, y, w, BUTTON_H, Ui.R_SM, Ui.DARK);
		Ui.ring(g, x, y, w, BUTTON_H, Ui.R_SM, active ? 0x80FFFFFF : Ui.LINE);
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
		if (left && Ui.inside(mx, my, px, py, pw, ph)) Ui.sound("press");
		// Any click ends key capture and typing; the clicked row may start it again below.
		listening = null;
		if (typing != null) {
			typing = null;
			Mods.save(); // clicking away from a text or colour field keeps what was typed
		}

		// Header and left column work in both views.
		if (left && Ui.inside(mx, my, right() - BUTTON_H, headY(), BUTTON_H, BUTTON_H)) {
			onClose();
			return true;
		}
		if (left && Ui.inside(mx, my, tabX(1), headY(), tabW(optionsLabel(), true), BUTTON_H)) {
			minecraft.setScreen(new OptionsScreen(this, minecraft.options));
			return true;
		}
		int sx = px + PAD;
		if (left && minecraft.level != null && Ui.inside(mx, my, sx + 4, py + ph - PAD - 4 - BUTTON_H, SIDE_W - 8, BUTTON_H)) {
			minecraft.setScreen(new HubScreen());
			return true;
		}
		for (int t = 0; t < TABS.length; t++) {
			if (left && Ui.inside(mx, my, sx + 2, sideRowY(t), SIDE_W - 4, SIDE_ROW)) {
				tab = t;
				focus = -1;
				selected = null;
				gridScroll = 0;
				return true;
			}
		}

		if (selected == null) {
			if (!left) return true;
			for (int i = 0; i < STATES.length; i++) {
				if (Ui.inside(mx, my, stateX(i), barY(), stateW(i), BUTTON_H)) {
					state = i;
					focus = -1;
					gridScroll = 0;
				}
			}
			if (!inView(my)) return true;
			List<Mod> list = shown();
			for (int i = 0; i < list.size(); i++) {
				Mod mod = list.get(i);
				int x = cardX(i), y = cardY(i);
				if (!Ui.inside(mx, my, x, y, cardW(), CARD_H)) continue;
				if (my >= y + TOP_H + STRIP) {
					mod.enabled = !mod.enabled;
					Mods.save();
				} else {
					selected = mod;
					rowScroll = 0;
				}
			}
			return true;
		}

		if (left && Ui.inside(mx, my, contentX(), barY(), BUTTON_H, BUTTON_H)) {
			selected = null;
		} else if (left && Ui.inside(mx, my, right() - PILL_W - 4, barY(), PILL_W + 4, BUTTON_H)) {
			selected.enabled = !selected.enabled;
			Mods.save();
		} else if (left && Ui.inside(mx, my, contentX(), footY(), footW(Ui.tr("reset")), BUTTON_H)) {
			selected.reset();
			Mods.save();
		} else if (inView(my)) {
			for (Cell cell : cells()) {
				Setting setting = cell.setting;
				if (setting == null || !Ui.inside(mx, my, cell.x, cell.y, cell.w, ROW_H)) continue;
				// The keyboard focus follows the mouse, so Enter acts on the row just clicked.
				rowMod = selected;
				rowFocus = rows().indexOf(setting);
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
				} else if (setting instanceof Setting.Action action && mx >= fieldX) {
					action.run.run();
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
		// Keyboard: Tab / Shift+Tab move between the cards, Enter opens the focused card's options, Shift+Enter turns it on or off.
		if (selected == null && (key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
			List<Mod> list = shown();
			if (list.isEmpty()) return true;
			if (key == GLFW.GLFW_KEY_TAB) {
				focus = Math.floorMod(focus < 0 ? (event.hasShiftDown() ? -1 : 0) : focus + (event.hasShiftDown() ? -1 : 1), list.size());
				// Scroll the focused card into view.
				int y = (focus / columns) * (CARD_H + GAP), viewH = viewBottom() - viewTop();
				if (y < gridScroll) gridScroll = y;
				else if (y + CARD_H > gridScroll + viewH) gridScroll = y + CARD_H - viewH;
			} else if (focus >= 0 && focus < list.size()) {
				Mod mod = list.get(focus);
				if (event.hasShiftDown()) {
					mod.enabled = !mod.enabled;
					Mods.save();
				} else {
					selected = mod;
					rowScroll = 0;
				}
			}
			return true;
		}
		// Keyboard in the options view: Tab / Shift+Tab or Up / Down move between options, Enter or Space uses one (switch,
		// key, button, text), Left / Right change sliders, choices (the palette too) and switches.
		if (selected != null && rowKeys(key, event.hasShiftDown())) return true;
		if (key == GLFW.GLFW_KEY_ESCAPE && selected != null) {
			selected = null; // Esc in the options view goes back to the grid first
			return true;
		}
		if (selected == null && key == GLFW.GLFW_KEY_BACKSPACE) {
			search = dropLast(search);
			focus = -1;
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

	/** The options a keyboard can reach, in screen order. */
	private List<Setting> rows() {
		List<Setting> out = new ArrayList<>();
		for (Cell c : cells()) if (c.setting != null) out.add(c.setting);
		return out;
	}

	private boolean rowKeys(int key, boolean shift) {
		List<Setting> rows = rows();
		if (rows.isEmpty()) return false;
		if (rowMod != selected) {
			rowMod = selected;
			rowFocus = -1;
		}
		if (key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) {
			int step = key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_TAB && shift ? -1 : 1;
			rowFocus = Math.floorMod(rowFocus < 0 ? (step > 0 ? -1 : 0) + step : rowFocus + step, rows.size());
			return true;
		}
		if (rowFocus < 0 || rowFocus >= rows.size()) return false;
		Setting s = rows.get(rowFocus);
		int dir = key == GLFW.GLFW_KEY_RIGHT ? 1 : key == GLFW.GLFW_KEY_LEFT ? -1 : 0;
		boolean use = key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE;
		if (dir == 0 && !use) return false;
		if (s instanceof Setting.Bool b) b.value = dir == 0 ? !b.value : dir > 0;
		else if (s instanceof Setting.Num n && dir != 0) n.set(n.get() + dir * n.step);
		else if (s instanceof Setting.Choice c) {
			int at = java.util.Arrays.asList(c.options).indexOf(c.value);
			c.value = c.options[Math.floorMod(at + (dir == 0 ? 1 : dir), c.options.length)];
		} else if (s instanceof Setting.Key k && use) listening = k;
		else if (s instanceof Setting.Action a && use) a.run.run();
		else if (s instanceof Setting.Color c && use) {
			typing = c;
			hex = String.format("%08X", c.value);
		} else if (s instanceof Setting.Text && use) typing = s;
		else return false;
		Mods.save();
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!event.isAllowedChatCharacter() || listening != null) return false;
		if (typing != null) {
			type(event.codepointAsString(), false);
		} else if (selected == null && search.length() < 32) {
			search += event.codepointAsString();
			focus = -1;
			gridScroll = 0;
		}
		return true;
	}

	private static String dropLast(String s) {
		return s.isEmpty() ? s : s.substring(0, s.offsetByCodePoints(s.length(), -1));
	}

	@Override
	public void onClose() {
		// A slider dragged while the screen closes never sees its release: keep its value.
		dragging = null;
		typing = null;
		Mods.save();
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

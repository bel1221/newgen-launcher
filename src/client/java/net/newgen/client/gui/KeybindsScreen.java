package net.newgen.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Mods;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

/**
 * Every key in one list: the keys of our mods, a key per mod that turns it on or off, and the game's own keys.
 * <pre>
 * [<] Keybinds                       [Clashes 2] [search.....]  [x]
 * ZOOM
 *   Zoom                                     [Hold|Toggle] [  C  ]
 *   Hold to zoom   clashes with Chat                       [  T  ]
 * </pre>
 * A click on a key listens for the next key or mouse button (Esc cancels), right click clears it (with Undo).
 * Hold / Toggle sits next to keys that support it. Two keys on the same button are marked "clashes with ...".
 */
public class KeybindsScreen extends Screen {
	private static final int PAD = 8, HEADER = 28, ROW_H = 18, HEAD_H = 16, BUTTON_H = 14, KEY_W = 84, MODE_W = 74;
	private static final int WARN = 0xFFE8A23A;

	/** One bindable key: code in Setting.Key form (keyboard code, Setting.Key.MOUSE + button, or -1 for none). */
	private record Bind(String group, String label, IntSupplier get, IntConsumer set, Setting.Key key, OptionInstance<Boolean> mode) {
		int code() {
			return get.getAsInt();
		}
	}

	private final Screen parent;
	private final List<Bind> binds = new ArrayList<>();
	private int px, py, pw, ph, scroll, goal, content;
	private String search = "";
	private boolean clashesOnly;
	private Bind listening;
	private String tip;

	public KeybindsScreen(Screen parent) {
		super(Component.literal("Keybinds"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		pw = Math.max(240, Math.min(500, width - 40));
		ph = Math.max(120, Math.min(310, height - 44));
		px = (width - pw) / 2;
		py = Math.max(36, (height - ph) / 2);
		binds.clear();
		// Our mods' own keys, by mod.
		for (Mod mod : Mods.ALL) {
			for (Setting s : mod.settings) {
				if (!(s instanceof Setting.Key k)) continue;
				binds.add(new Bind(name(mod), label(s.name), () -> k.value, v -> k.value = v, k, null));
			}
		}
		// A key per mod that switches it on or off in game.
		String toggles = Ui.tr("keys.toggles");
		for (Mod mod : Mods.ALL) {
			if (mod.id.equals("theme")) continue;
			binds.add(new Bind(toggles, name(mod), () -> Mods.toggleKeys.getOrDefault(mod.id, -1), v -> {
				if (v == -1) Mods.toggleKeys.remove(mod.id);
				else Mods.toggleKeys.put(mod.id, v);
			}, null, null));
		}
		// The game's keys (and other mods' keys registered with it), by their category.
		for (KeyMapping km : minecraft.options.keyMappings) {
			OptionInstance<Boolean> mode = km == minecraft.options.keySprint ? minecraft.options.toggleSprint()
					: km == minecraft.options.keyShift ? minecraft.options.toggleCrouch() : null;
			binds.add(new Bind(km.getCategory().label().getString(), I18n.get(km.getName()), () -> vanillaCode(km), v -> {
				km.setKey(v == -1 ? InputConstants.UNKNOWN : v >= Setting.Key.MOUSE && v < Setting.Key.MOUSE + 8
						? InputConstants.Type.MOUSE.getOrCreate(v - Setting.Key.MOUSE) : InputConstants.Type.KEYSYM.getOrCreate(v));
				KeyMapping.resetMapping();
				minecraft.options.save();
			}, null, mode));
		}
	}

	private static int vanillaCode(KeyMapping km) {
		InputConstants.Key k = KeyBindingHelper.getBoundKeyOf(km);
		if (k == InputConstants.UNKNOWN || k.getValue() < 0) return -1;
		return k.getType() == InputConstants.Type.MOUSE ? Setting.Key.MOUSE + k.getValue() : k.getValue();
	}

	private static String name(Mod mod) {
		String key = "newgen_client.mod." + mod.id + ".name";
		return I18n.exists(key) ? I18n.get(key) : mod.name;
	}

	private static String label(String name) {
		String key = "newgen_client.setting." + name.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
		return I18n.exists(key) ? I18n.get(key) : name;
	}

	static String keyName(int code) {
		if (code == -1) return Ui.tr("key.none");
		if (code >= Setting.Key.MOUSE && code < Setting.Key.MOUSE + 8)
			return InputConstants.Type.MOUSE.getOrCreate(code - Setting.Key.MOUSE).getDisplayName().getString();
		return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
	}

	/** The other keys on the same button. */
	private List<Bind> clashes(Bind b) {
		List<Bind> out = new ArrayList<>();
		int code = b.code();
		if (code == -1) return out;
		for (Bind o : binds) if (o != b && o.code() == code) out.add(o);
		return out;
	}

	private List<Bind> shown() {
		List<Bind> out = new ArrayList<>();
		String q = search.toLowerCase();
		for (Bind b : binds) {
			if (clashesOnly && clashes(b).isEmpty()) continue;
			if (!q.isEmpty() && !(b.group + " " + b.label + " " + keyName(b.code())).toLowerCase().contains(q)) continue;
			out.add(b);
		}
		return out;
	}

	private int clashCount() {
		int n = 0;
		for (Bind b : binds) if (!clashes(b).isEmpty()) n++;
		return n;
	}

	// ---- layout ----

	private int right() {
		return px + pw - PAD;
	}

	private int headY() {
		return py + (HEADER - BUTTON_H) / 2;
	}

	private int viewTop() {
		return py + HEADER + 4;
	}

	private int viewBottom() {
		return py + ph - PAD;
	}

	private int clashW() {
		return Ui.width(Ui.trf("keys.clashes", clashCount()), Ui.CAPS) + 16;
	}

	private int clashX() {
		return searchX() - clashW() - 4;
	}

	private int searchX() {
		return right() - BUTTON_H - 6 - searchW();
	}

	private int searchW() {
		return Math.min(130, pw / 3);
	}

	/** Where each row is: headings when the group changes. Returns rows paired with their y (heading rows have b == null). */
	private record Row(Bind b, String heading, int y) {
	}

	private List<Row> rows() {
		List<Row> out = new ArrayList<>();
		int y = viewTop() - scroll;
		String group = null;
		for (Bind b : shown()) {
			if (!b.group.equals(group)) {
				group = b.group;
				out.add(new Row(null, group, y));
				y += HEAD_H;
			}
			out.add(new Row(b, null, y));
			y += ROW_H;
		}
		content = y + scroll - viewTop();
		return out;
	}

	private int keyX() {
		return right() - 7 - KEY_W;
	}

	private int modeX() {
		return keyX() - 6 - MODE_W;
	}

	// ---- drawing ----

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (minecraft.level == null && !VideoBackground.render(g, width, height)) renderPanorama(g, delta);
		renderBlurredBackground(g);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		tip = null;
		Ui.panel(g, px, py, pw, ph);
		g.fill(px + 1, py + HEADER, px + pw - 1, py + HEADER + 1, 0x18FFFFFF);
		int x = px + PAD;
		// The same header as the Mods menu: the NewGen mark, back, the page name, its tools, close.
		Ui.icon(g, x, py + (HEADER - 14) / 2, 14);
		int hx = x + 18;
		Ui.button(g, hx, headY(), BUTTON_H, BUTTON_H, "", Ui.I_BACK, Ui.inside(mouseX, mouseY, hx, headY(), BUTTON_H, BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);
		Ui.glyph(g, Ui.I_KEYBOARD, hx + BUTTON_H + 8, headY() + 2, 10, Ui.TEXT);
		Ui.text(g, Ui.tr("keys"), hx + BUTTON_H + 22, py + (HEADER - 7) / 2, Ui.TEXT, Ui.TITLE);
		int clashes = clashCount();
		if (clashX() > hx + BUTTON_H + 22 + Ui.width(Ui.tr("keys"), Ui.TITLE) + 8) {
			Ui.button(g, clashX(), headY(), clashW(), BUTTON_H, Ui.trf("keys.clashes", clashes),
					Ui.inside(mouseX, mouseY, clashX(), headY(), clashW(), BUTTON_H), clashesOnly ? Ui.SELECTED : Ui.DARK, Ui.CAPS);
			if (Ui.inside(mouseX, mouseY, clashX(), headY(), clashW(), BUTTON_H)) tip = Ui.tr("keys.clashes.tip");
		}
		int sx = searchX(), sw = searchW();
		Ui.round(g, sx, headY(), sw, BUTTON_H, Ui.R_SM, Ui.DARK);
		Ui.ring(g, sx, headY(), sw, BUTTON_H, Ui.R_SM, search.isEmpty() ? Ui.LINE : 0x80FFFFFF);
		Ui.glyph(g, Ui.I_SEARCH, sx + 5, headY() + 3, 8, Ui.DIM);
		Ui.text(g, Ui.fit(search.isEmpty() ? Ui.tr("keys.search") : search + "_", sw - 24, Ui.UI, !search.isEmpty()), sx + 17, headY() + 3,
				search.isEmpty() ? Ui.DIM : Ui.TEXT, Ui.UI);
		Ui.button(g, right() - BUTTON_H, headY(), BUTTON_H, BUTTON_H, "", Ui.I_CLOSE,
				Ui.inside(mouseX, mouseY, right() - BUTTON_H, headY(), BUTTON_H, BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);

		List<Row> rows = rows();
		int max = Math.max(0, content - (viewBottom() - viewTop()));
		goal = Math.max(0, Math.min(goal, max));
		int next = Math.max(0, Math.min(Overlay.approach(scroll, goal), max));
		if (next != scroll) {
			scroll = next;
			rows = rows();
		}
		boolean hot = mouseY >= viewTop() && mouseY < viewBottom() && !Overlay.confirming();
		g.enableScissor(px + PAD - 2, viewTop(), right(), viewBottom());
		for (Row r : rows) {
			if (r.y + ROW_H <= viewTop() || r.y >= viewBottom()) continue;
			if (r.b == null) {
				Ui.text(g, Ui.fit(r.heading.toUpperCase(), pw / 2, Ui.CAPS, false), x, r.y + 6, Ui.TEXT, Ui.CAPS);
				continue;
			}
			row(g, r.b, r.y, hot ? mouseX : -1, mouseY);
		}
		Overlay.fades(g, px + PAD - 2, viewTop(), right(), viewBottom(), scroll, max);
		g.disableScissor();
		if (rows.isEmpty()) Ui.centered(g, Ui.tr("keys.none"), px + pw / 2, viewTop() + 12, Ui.DIM, Ui.UI);
		if (max > 0) {
			int viewH = viewBottom() - viewTop(), h = Math.max(8, viewH * viewH / content), y = viewTop() + (viewH - h) * scroll / max;
			Ui.round(g, right() - 3, viewTop(), 3, viewH, 1, 0x18FFFFFF);
			Ui.round(g, right() - 3, y, 3, h, 1, 0x80FFFFFF);
		}
		Overlay.tooltip(g, Overlay.confirming() ? null : tip, mouseX, mouseY, width, height);
		Overlay.renderToasts(g, width, height, mouseX, mouseY);
		Overlay.renderConfirm(g, width, height, mouseX, mouseY);
	}

	private void row(GuiGraphics g, Bind b, int y, int mouseX, int mouseY) {
		int x = px + PAD + 6;
		if (Ui.inside(mouseX, mouseY, px + PAD - 2, y, right() - 7 - (px + PAD - 2), ROW_H)) Ui.round(g, px + PAD - 2, y + 1, right() - 7 - (px + PAD - 2), ROW_H - 2, 4, 0x10FFFFFF);
		List<Bind> clashes = clashes(b);
		boolean canMode = b.key != null || b.mode != null;
		int labelEnd = (canMode ? modeX() : keyX()) - 8;
		String label = Ui.fit(b.label, Math.max(20, labelEnd - x), Ui.UI, false);
		Ui.text(g, label, x, y + 5, Ui.SOFT, Ui.UI);
		if (!clashes.isEmpty()) {
			int cx = x + Ui.width(label, Ui.UI) + 8;
			String warn = Ui.trf("keys.clash", clashes.get(0).label) + (clashes.size() > 1 ? " +" + (clashes.size() - 1) : "");
			if (labelEnd - cx > 30) Ui.text(g, Ui.fit(warn, labelEnd - cx, Ui.UI, false), cx, y + 5, WARN, Ui.UI);
			if (Ui.inside(mouseX, mouseY, x, y, labelEnd - x, ROW_H)) {
				StringBuilder all = new StringBuilder(Ui.tr("keys.clash.tip"));
				for (Bind c : clashes) all.append("\n").append(c.group).append(" › ").append(c.label);
				tip = all.toString();
			}
		}
		if (canMode) {
			// Hold | Toggle, the chosen half filled.
			boolean toggle = b.key != null ? b.key.toggle : b.mode.get();
			int half = MODE_W / 2, mx = modeX();
			Ui.round(g, mx, y + 2, MODE_W, BUTTON_H, Ui.R_SM, Ui.DARK);
			Ui.round(g, toggle ? mx + half : mx, y + 2, half, BUTTON_H, Ui.R_SM, Ui.SELECTED);
			Ui.centered(g, Ui.tr("keys.hold"), mx + half / 2, y + 6, toggle ? Ui.DIM : Ui.TEXT, Ui.CAPS);
			Ui.centered(g, Ui.tr("keys.toggle"), mx + half + half / 2, y + 6, toggle ? Ui.TEXT : Ui.DIM, Ui.CAPS);
		}
		boolean over = Ui.inside(mouseX, mouseY, keyX(), y + 2, KEY_W, BUTTON_H);
		String key = b == listening ? Ui.tr("key.listening") : keyName(b.code());
		Ui.button(g, keyX(), y + 2, KEY_W, BUTTON_H, key, over, b == listening ? Ui.SELECTED : Ui.DARK, Ui.CAPS);
		if (!clashes.isEmpty()) Ui.ring(g, keyX(), y + 2, KEY_W, BUTTON_H, Ui.R_SM, WARN);
		if (over && b != listening) tip = Ui.tr("keys.key.tip");
	}

	// ---- input ----

	private void bind(Bind b, int code) {
		int before = b.code();
		b.set.accept(code);
		Mods.save();
		if (code == -1 && before != -1)
			Overlay.toast(Ui.trf("toast.key_cleared", b.label), () -> {
				b.set.accept(before);
				Mods.save();
			});
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (Overlay.clickConfirm(mx, my, width, height)) return true;
		if (listening != null && event.button() >= 0 && event.button() < 8) {
			bind(listening, Setting.Key.MOUSE + event.button());
			listening = null;
			return true;
		}
		boolean left = event.button() == 0;
		if (left && Overlay.clickToast(mx, my, width, height)) return true;
		if (left && Ui.inside(mx, my, right() - BUTTON_H, headY(), BUTTON_H, BUTTON_H)) {
			onClose();
			return true;
		}
		if (left && Ui.inside(mx, my, px + PAD + 18, headY(), BUTTON_H, BUTTON_H)) {
			minecraft.setScreen(parent);
			return true;
		}
		if (left && Ui.inside(mx, my, clashX(), headY(), clashW(), BUTTON_H)) {
			clashesOnly = !clashesOnly;
			goal = scroll = 0;
			return true;
		}
		if (my < viewTop() || my >= viewBottom()) return true;
		for (Row r : rows()) {
			if (r.b == null || !Ui.inside(mx, my, px + PAD, r.y, pw, ROW_H)) continue;
			Bind b = r.b;
			if (Ui.inside(mx, my, keyX(), r.y + 2, KEY_W, BUTTON_H)) {
				if (left) listening = b;
				else if (event.button() == 1) bind(b, -1);
			} else if (left && (b.key != null || b.mode != null) && Ui.inside(mx, my, modeX(), r.y + 2, MODE_W, BUTTON_H)) {
				boolean toggle = mx >= modeX() + MODE_W / 2;
				if (b.key != null) b.key.toggle = toggle;
				else {
					b.mode.set(toggle);
					minecraft.options.save();
				}
				Mods.save();
			}
			if (left) Ui.sound("press");
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		goal -= (int) (scrollY * ROW_H * 1.5);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (Overlay.keyConfirm(key)) return true;
		if (listening != null) {
			// Esc cancels; any other key is the new binding (right click on a key clears it).
			if (key != GLFW.GLFW_KEY_ESCAPE) bind(listening, key);
			listening = null;
			return true;
		}
		if (key == GLFW.GLFW_KEY_Z && event.hasControlDown()) {
			Overlay.undoLatest();
			return true;
		}
		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			if (!search.isEmpty()) search = search.substring(0, search.offsetByCodePoints(search.length(), -1));
			goal = 0;
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			minecraft.setScreen(parent);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (listening != null || !event.isAllowedChatCharacter()) return false;
		if (search.length() < 32) search += event.codepointAsString();
		goal = 0;
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

package net.newgen.client.gui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.Profiles;
import net.newgen.client.core.Profiles.Profile;
import org.lwjgl.glfw.GLFW;

/**
 * Profiles: whole sets of settings and keys to switch between.
 * <pre>
 * [<] Profiles                       [+ New] [Paste] [Open file]  [x]
 *  (icon) PvP            * active            [star]  [Use]  [...]
 *  (icon) Building                           [star]  [Use]  [...]
 * </pre>
 * The icon cycles on click; a double click on the name (or Rename in ...) edits it in place. ... holds Duplicate,
 * Rename, Copy as text, Save as file and Delete (asks first, then Undo). Starred profiles come first.
 */
public class ProfilesScreen extends Screen {
	private static final int PAD = 8, HEADER = 28, ROW_H = 22, BUTTON_H = 14, USE_W = 54;
	/** The icons a profile can wear (menu glyphs). */
	private static final int[] ICONS = {Ui.I_USER, Ui.I_STAR, Ui.I_PLAY, Ui.I_MOUSE, Ui.I_EYE, Ui.I_LAYOUT, Ui.I_GLOBE, Ui.I_PALETTE, Ui.I_KEYBOARD, Ui.I_SLIDERS};
	private static final String[] MENU = {"profiles.duplicate", "profiles.rename", "profiles.copy", "profiles.save_file", "profiles.delete"};
	private static final int MENU_W = 130, MENU_ROW = 14;

	private final Screen parent;
	private int px, py, pw, ph, scroll, goal;
	private Profile renaming, menuFor;
	private String nameText = "";
	private int menuX, menuY;
	private String tip;

	public ProfilesScreen(Screen parent) {
		super(Component.literal("Profiles"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		pw = Math.max(240, Math.min(420, width - 40));
		ph = Math.max(120, Math.min(280, height - 44));
		px = (width - pw) / 2;
		py = Math.max(36, (height - ph) / 2);
	}

	private List<Profile> sorted() {
		List<Profile> out = new ArrayList<>(Profiles.ALL);
		out.sort((a, b) -> Boolean.compare(b.favourite, a.favourite));
		return out;
	}

	private int right() {
		return px + pw - PAD;
	}

	private int headY() {
		return py + (HEADER - BUTTON_H) / 2;
	}

	private int top() {
		return py + HEADER + 6;
	}

	private int bottom() {
		return py + ph - PAD;
	}

	private int rowY(int i) {
		return top() - scroll + i * (ROW_H + 4);
	}

	/** Header buttons from the right: close, Open file, Paste, New. */
	private int[] headButtons() {
		int close = right() - BUTTON_H;
		int fileW = Ui.width(Ui.tr("profiles.open_file"), Ui.CAPS) + 30, pasteW = Ui.width(Ui.tr("profiles.paste"), Ui.CAPS) + 30,
				newW = Ui.width(Ui.tr("profiles.new"), Ui.CAPS) + 30;
		int file = close - 6 - fileW, paste = file - 4 - pasteW, add = paste - 4 - newW;
		return new int[]{add, newW, paste, pasteW, file, fileW, close};
	}

	private int useX() {
		return right() - 7 - 18 - 4 - USE_W;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (minecraft.level == null && !VideoBackground.render(g, width, height)) renderPanorama(g, delta);
		renderBlurredBackground(g);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		tip = null;
		boolean blocked = Overlay.confirming() || menuFor != null;
		int mx = blocked ? -1 : mouseX;
		Ui.panel(g, px, py, pw, ph);
		g.fill(px + 1, py + HEADER, px + pw - 1, py + HEADER + 1, 0x18FFFFFF);
		int x = px + PAD;
		Ui.button(g, x, headY(), BUTTON_H, BUTTON_H, "", Ui.I_BACK, Ui.inside(mx, mouseY, x, headY(), BUTTON_H, BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);
		Ui.text(g, Ui.tr("profiles"), x + BUTTON_H + 8, py + (HEADER - 7) / 2, Ui.TEXT, Ui.TITLE);
		int[] b = headButtons();
		if (b[0] > x + BUTTON_H + 8 + Ui.width(Ui.tr("profiles"), Ui.TITLE) + 6) {
			Ui.button(g, b[0], headY(), b[1], BUTTON_H, Ui.tr("profiles.new"), Ui.I_PLUS, Ui.inside(mx, mouseY, b[0], headY(), b[1], BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);
			Ui.button(g, b[2], headY(), b[3], BUTTON_H, Ui.tr("profiles.paste"), Ui.I_COPY, Ui.inside(mx, mouseY, b[2], headY(), b[3], BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);
			Ui.button(g, b[4], headY(), b[5], BUTTON_H, Ui.tr("profiles.open_file"), Ui.I_UPLOAD, Ui.inside(mx, mouseY, b[4], headY(), b[5], BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);
			if (Ui.inside(mx, mouseY, b[2], headY(), b[3], BUTTON_H)) tip = Ui.tr("profiles.paste.tip");
		}
		Ui.button(g, b[6], headY(), BUTTON_H, BUTTON_H, "", Ui.I_CLOSE, Ui.inside(mx, mouseY, b[6], headY(), BUTTON_H, BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);

		List<Profile> list = sorted();
		int content = list.size() * (ROW_H + 4) - 4, max = Math.max(0, content - (bottom() - top()));
		goal = Math.max(0, Math.min(goal, max));
		scroll = Math.max(0, Math.min(Overlay.approach(scroll, goal), max));
		g.enableScissor(px + PAD, top(), right(), bottom());
		Profile active = Profiles.active();
		for (int i = 0; i < list.size(); i++) {
			Profile p = list.get(i);
			int y = rowY(i);
			if (y + ROW_H <= top() || y >= bottom()) continue;
			boolean on = p == active, over = Ui.inside(mx, mouseY, x, y, right() - 7 - x, ROW_H);
			Ui.round(g, x, y, right() - 7 - x, ROW_H, Ui.R_MD, on ? Ui.SELECTED : Ui.CARD);
			if (over) Ui.round(g, x, y, right() - 7 - x, ROW_H, Ui.R_MD, Ui.HOVER);
			if (on) Ui.round(g, x + 2, y + 6, 2, ROW_H - 12, 1, Ui.ACCENT);
			boolean iconHot = Ui.inside(mx, mouseY, x + 8, y + 3, 16, 16);
			Ui.round(g, x + 8, y + 3, 16, 16, Ui.R_SM, iconHot ? Ui.HOVER : 0x14FFFFFF);
			Ui.glyph(g, ICONS[Math.floorMod(p.icon, ICONS.length)], x + 11, y + 6, 10, Ui.TEXT);
			if (iconHot) tip = Ui.tr("profiles.icon.tip");
			int nx = x + 32, nameEnd = useX() - 8 - 18;
			if (p == renaming) {
				Ui.round(g, nx - 4, y + 4, nameEnd - nx + 4, BUTTON_H, Ui.R_SM, Ui.DARK);
				Ui.ring(g, nx - 4, y + 4, nameEnd - nx + 4, BUTTON_H, Ui.R_SM, 0x80FFFFFF);
				Ui.text(g, Ui.fit(nameText + "_", nameEnd - nx - 4, Ui.UI, true), nx, y + 7, Ui.TEXT, Ui.UI);
			} else {
				String name = Ui.fit(p.name, nameEnd - nx, Ui.CAPS, false);
				Ui.text(g, name, nx, y + 7, on ? Ui.TEXT : Ui.SOFT, Ui.CAPS);
				if (on) Ui.text(g, Ui.tr("profiles.active"), nx + Ui.width(name, Ui.CAPS) + 8, y + 7, Ui.DIM, Ui.UI);
			}
			int sx = useX() - 4 - 16;
			boolean starHot = Ui.inside(mx, mouseY, sx, y + 3, 16, 16);
			Ui.glyph(g, p.favourite ? Ui.I_STAR_ON : Ui.I_STAR, sx + 3, y + 6, 10, p.favourite ? Ui.ACCENT : starHot ? Ui.TEXT : Ui.DIM);
			if (!on) Ui.button(g, useX(), y + 4, USE_W, BUTTON_H, Ui.tr("profiles.use"), Ui.inside(mx, mouseY, useX(), y + 4, USE_W, BUTTON_H), Ui.DARK, Ui.CAPS);
			int dx = right() - 7 - 4 - 18;
			Ui.button(g, dx, y + 4, 18, BUTTON_H, "", Ui.I_MORE, Ui.inside(mx, mouseY, dx, y + 4, 18, BUTTON_H) ? 1f : 0f, Ui.DARK, Ui.CAPS);
		}
		Overlay.fades(g, px + PAD, top(), right(), bottom(), scroll, max);
		g.disableScissor();
		if (menuFor != null) menu(g, mouseX, mouseY);
		Overlay.tooltip(g, blocked ? null : tip, mouseX, mouseY, width, height);
		Overlay.renderToasts(g, width, height, mouseX, mouseY);
		Overlay.renderConfirm(g, width, height, mouseX, mouseY);
	}

	private int menuLeft() {
		return Math.min(menuX, width - MENU_W - 2);
	}

	private int menuTop() {
		return Math.min(menuY, height - MENU.length * MENU_ROW - 6);
	}

	private void menu(GuiGraphics g, int mouseX, int mouseY) {
		g.nextStratum();
		int x = menuLeft(), y = menuTop(), h = MENU.length * MENU_ROW + 4;
		Ui.round(g, x, y, MENU_W, h, Ui.R_SM, Ui.GLASS);
		Ui.round(g, x, y, MENU_W, h, Ui.R_SM, 0x60000000);
		Ui.ring(g, x, y, MENU_W, h, Ui.R_SM, Ui.LINE);
		int[] icons = {Ui.I_PLUS, Ui.I_SLIDERS, Ui.I_COPY, Ui.I_DOWNLOAD, Ui.I_CLOSE};
		for (int i = 0; i < MENU.length; i++) {
			int ry = y + 2 + i * MENU_ROW;
			boolean off = i == 4 && (menuFor == Profiles.active() || Profiles.ALL.size() <= 1);
			if (!off && Ui.inside(mouseX, mouseY, x + 2, ry, MENU_W - 4, MENU_ROW)) Ui.round(g, x + 2, ry, MENU_W - 4, MENU_ROW, Ui.R_SM, Ui.HOVER);
			Ui.glyph(g, icons[i], x + 7, ry + 3, 8, off ? Ui.DIM : Ui.SOFT);
			Ui.text(g, Ui.tr(MENU[i]), x + 20, ry + 4, off ? Ui.DIM : Ui.TEXT, Ui.UI);
		}
	}

	private void menuClick(double mx, double my) {
		Profile p = menuFor;
		menuFor = null;
		int x = menuLeft(), y = menuTop();
		for (int i = 0; i < MENU.length; i++) {
			if (!Ui.inside(mx, my, x + 2, y + 2 + i * MENU_ROW, MENU_W - 4, MENU_ROW)) continue;
			switch (i) {
				case 0 -> {
					Profile copy = Profiles.duplicate(p);
					Overlay.toast(Ui.trf("toast.profile_added", copy.name), null);
				}
				case 1 -> startRename(p);
				case 2 -> {
					minecraft.keyboardHandler.setClipboard(Profiles.toText(p));
					Overlay.toast(Ui.trf("toast.profile_copied", p.name), null);
				}
				case 3 -> saveFile(p);
				default -> {
					if (p == Profiles.active() || Profiles.ALL.size() <= 1) return;
					Overlay.confirm(Ui.trf("confirm.profile_delete", p.name), Ui.tr("profiles.delete"), () -> {
						com.google.gson.JsonObject data = Profiles.snapshot(p);
						int at = Profiles.ALL.indexOf(p);
						Profiles.delete(p);
						Overlay.toast(Ui.trf("toast.profile_deleted", p.name), () -> Profiles.restore(p, data, at));
					});
				}
			}
		}
	}

	private void startRename(Profile p) {
		renaming = p;
		nameText = p.name;
	}

	private void finishRename() {
		if (renaming == null) return;
		String name = nameText.trim();
		if (!name.isEmpty() && !name.equals(renaming.name)) {
			renaming.name = name.equalsIgnoreCase(renaming.name) ? name : Profiles.freeName(name);
			Profiles.changed();
		}
		renaming = null;
	}

	/** The system's save / open dialogs run on their own thread (they block); the result comes back on the game thread. */
	private static void saveFile(Profile p) {
		Thread t = new Thread(() -> {
			String path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_saveFileDialog(Ui.tr("profiles.save_file"), p.name + ".zip", null, null);
			if (path == null) return;
			Minecraft.getInstance().execute(() -> {
				try {
					Profiles.toZip(p, Path.of(path.endsWith(".zip") ? path : path + ".zip"));
					Overlay.toast(Ui.trf("toast.profile_saved", p.name), null);
				} catch (Exception e) {
					Overlay.toast(Ui.tr("toast.profile_failed"), null);
				}
			});
		}, "Newgen profile export");
		t.setDaemon(true);
		t.start();
	}

	private static void openFile() {
		Thread t = new Thread(() -> {
			String path;
			try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
				org.lwjgl.PointerBuffer patterns = stack.mallocPointer(1);
				patterns.put(stack.UTF8("*.zip"));
				patterns.flip();
				path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(Ui.tr("profiles.open_file"), null, patterns, null, false);
			}
			if (path == null) return;
			String chosen = path;
			Minecraft.getInstance().execute(() -> {
				try {
					Profile p = Profiles.fromZip(Path.of(chosen));
					Overlay.toast(p != null ? Ui.trf("toast.profile_added", p.name) : Ui.tr("toast.profile_failed"), null);
				} catch (Exception e) {
					Overlay.toast(Ui.tr("toast.profile_failed"), null);
				}
			});
		}, "Newgen profile import");
		t.setDaemon(true);
		t.start();
	}

	private void use(Profile p) {
		Profile before = Profiles.active();
		Profiles.switchTo(p);
		Overlay.toast(Ui.trf("toast.profile_switched", p.name), before == null ? null : () -> Profiles.switchTo(before));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (Overlay.clickConfirm(mx, my, width, height)) return true;
		if (menuFor != null) {
			if (event.button() == 0) menuClick(mx, my);
			else menuFor = null;
			return true;
		}
		boolean left = event.button() == 0;
		if (left && Overlay.clickToast(mx, my, width, height)) return true;
		finishRename();
		int[] b = headButtons();
		if (left && Ui.inside(mx, my, px + PAD, headY(), BUTTON_H, BUTTON_H)) {
			minecraft.setScreen(parent);
			return true;
		}
		if (left && Ui.inside(mx, my, b[6], headY(), BUTTON_H, BUTTON_H)) {
			onClose();
			return true;
		}
		if (left && Ui.inside(mx, my, b[0], headY(), b[1], BUTTON_H)) {
			Profile p = Profiles.create(Ui.tr("profiles.new_name"));
			startRename(p);
			return true;
		}
		if (left && Ui.inside(mx, my, b[2], headY(), b[3], BUTTON_H)) {
			Profile p = Profiles.fromText(minecraft.keyboardHandler.getClipboard());
			Overlay.toast(p != null ? Ui.trf("toast.profile_added", p.name) : Ui.tr("toast.profile_paste_none"), null);
			return true;
		}
		if (left && Ui.inside(mx, my, b[4], headY(), b[5], BUTTON_H)) {
			openFile();
			return true;
		}
		if (my < top() || my >= bottom()) return true;
		List<Profile> list = sorted();
		int x = px + PAD;
		for (int i = 0; i < list.size(); i++) {
			Profile p = list.get(i);
			int y = rowY(i);
			if (!Ui.inside(mx, my, x, y, right() - 7 - x, ROW_H)) continue;
			int dx = right() - 7 - 4 - 18, sx = useX() - 4 - 16;
			if (!left || Ui.inside(mx, my, dx, y + 4, 18, BUTTON_H)) {
				menuFor = p;
				menuX = (int) mx;
				menuY = (int) my;
			} else if (Ui.inside(mx, my, x + 8, y + 3, 16, 16)) {
				p.icon = Math.floorMod(p.icon + 1, ICONS.length);
				Profiles.changed();
			} else if (Ui.inside(mx, my, sx, y + 3, 16, 16)) {
				p.favourite = !p.favourite;
				Profiles.changed();
			} else if (p != Profiles.active() && Ui.inside(mx, my, useX(), y + 4, USE_W, BUTTON_H)) {
				use(p);
			} else if (doubleClick) {
				startRename(p);
			}
			Ui.sound("press");
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
		if (renaming != null) {
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) finishRename();
			else if (key == GLFW.GLFW_KEY_ESCAPE) renaming = null;
			else if (key == GLFW.GLFW_KEY_BACKSPACE && !nameText.isEmpty()) nameText = nameText.substring(0, nameText.offsetByCodePoints(nameText.length(), -1));
			return true;
		}
		if (menuFor != null && key == GLFW.GLFW_KEY_ESCAPE) {
			menuFor = null;
			return true;
		}
		if (key == GLFW.GLFW_KEY_Z && event.hasControlDown()) {
			Overlay.undoLatest();
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
		if (renaming == null || !event.isAllowedChatCharacter()) return false;
		if (nameText.length() < 24) nameText += event.codepointAsString();
		return true;
	}

	@Override
	public void onClose() {
		finishRename();
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

package net.newgen.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.newgen.client.core.Mods;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

/**
 * Picks an item (a grid of icons) or a status effect (a list of names) for a Pick option, with a search box.
 * Typing filters at once; a click picks and goes back.
 */
public class RegistryPickerScreen extends Screen {
	private static final int PW = 300, PH = 220, PAD = 10, CELL = 20, ROW = 16;
	private final Screen parent;
	private final Setting.Pick pick;
	private final List<String> all = new ArrayList<>();
	private List<String> shown = new ArrayList<>();
	private String search = "";
	private int px, py, scroll;

	public RegistryPickerScreen(Screen parent, Setting.Pick pick) {
		super(Component.literal(pick.name));
		this.parent = parent;
		this.pick = pick;
		if (pick.kind == Setting.Pick.Kind.ITEM) for (Identifier id : BuiltInRegistries.ITEM.keySet()) all.add(id.toString());
		else for (Identifier id : BuiltInRegistries.MOB_EFFECT.keySet()) all.add(id.toString());
		all.sort(null);
		filter();
	}

	private boolean items() {
		return pick.kind == Setting.Pick.Kind.ITEM;
	}

	private String label(String id) {
		Identifier key = Identifier.parse(id);
		if (items()) return BuiltInRegistries.ITEM.getValue(key).getName().getString();
		var effect = BuiltInRegistries.MOB_EFFECT.getValue(key);
		return effect == null ? id : effect.getDisplayName().getString();
	}

	private void filter() {
		String q = search.toLowerCase();
		shown = new ArrayList<>();
		for (String id : all) if (q.isEmpty() || id.contains(q) || label(id).toLowerCase().contains(q)) shown.add(id);
		scroll = 0;
	}

	@Override
	protected void init() {
		px = (width - PW) / 2;
		py = Math.max(36, (height - PH) / 2);
	}

	private int cols() {
		return (PW - PAD * 2) / CELL;
	}

	private int top() {
		return py + 48;
	}

	private int bottom() {
		return py + PH - PAD;
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
		Ui.text(g, Ui.fit(pick.name, PW - 40, Ui.TITLE, false), px + PAD, py + 8, Ui.TEXT, Ui.TITLE);
		int cx = px + PW - PAD - 14;
		Ui.button(g, cx, py + 6, 14, 14, "", Ui.I_CLOSE, Ui.inside(mouseX, mouseY, cx, py + 6, 14, 14) ? 1f : 0f, Ui.DARK, Ui.CAPS);
		int fy = py + 26, fw = PW - PAD * 2;
		Ui.round(g, px + PAD, fy, fw, 14, Ui.R_SM, Ui.DARK);
		Ui.ring(g, px + PAD, fy, fw, 14, Ui.R_SM, 0x80FFFFFF);
		Ui.glyph(g, Ui.I_SEARCH, px + PAD + 5, fy + 3, 8, Ui.DIM);
		Ui.text(g, search.isEmpty() ? Ui.tr("search") : search + "_", px + PAD + 17, fy + 3, search.isEmpty() ? Ui.DIM : Ui.TEXT, Ui.UI);

		g.enableScissor(px + PAD, top(), px + PW - PAD, bottom());
		String hover = null;
		for (int i = 0; i < shown.size(); i++) {
			String id = shown.get(i);
			if (items()) {
				int x = px + PAD + (i % cols()) * CELL, y = top() - scroll + (i / cols()) * CELL;
				if (y + CELL < top() || y > bottom()) continue;
				boolean over = Ui.inside(mouseX, mouseY, x, y, CELL, CELL) && mouseY >= top() && mouseY < bottom();
				if (id.equals(pick.value) || over) Ui.round(g, x, y, CELL - 2, CELL - 2, Ui.R_SM, id.equals(pick.value) ? Ui.SELECTED : Ui.HOVER);
				Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
				g.renderItem(new ItemStack(item), x + 1, y + 1);
				if (over) hover = label(id);
			} else {
				int y = top() - scroll + i * ROW;
				if (y + ROW < top() || y > bottom()) continue;
				boolean over = Ui.inside(mouseX, mouseY, px + PAD, y, PW - PAD * 2, ROW) && mouseY >= top() && mouseY < bottom();
				if (id.equals(pick.value) || over) Ui.round(g, px + PAD, y, PW - PAD * 2, ROW - 2, Ui.R_SM, id.equals(pick.value) ? Ui.SELECTED : Ui.HOVER);
				Ui.text(g, label(id), px + PAD + 6, y + 3, Ui.TEXT, Ui.UI);
			}
		}
		g.disableScissor();
		if (hover != null) g.setTooltipForNextFrame(font, Component.literal(hover), mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (Ui.inside(mx, my, px + PW - PAD - 14, py + 6, 14, 14)) {
			onClose();
			return true;
		}
		if (my < top() || my >= bottom()) return true;
		for (int i = 0; i < shown.size(); i++) {
			boolean hit = items()
					? Ui.inside(mx, my, px + PAD + (i % cols()) * CELL, top() - scroll + (i / cols()) * CELL, CELL, CELL)
					: Ui.inside(mx, my, px + PAD, top() - scroll + i * ROW, PW - PAD * 2, ROW);
			if (hit) {
				pick.value = shown.get(i);
				onClose();
				return true;
			}
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int rows = items() ? (shown.size() + cols() - 1) / cols() * CELL : shown.size() * ROW;
		scroll = (int) Math.max(0, Math.min(Math.max(0, rows - (bottom() - top())), scroll - scrollY * 20));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
			search = search.substring(0, search.length() - 1);
			filter();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (search.length() < 32) {
			search += event.codepointAsString();
			filter();
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

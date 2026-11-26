package net.newgen.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Mods;

/** "Copy style from...": a list of every other HUD element; a click copies its style onto this one. */
public class CopyStyleScreen extends Screen {
	private static final int PW = 220, PAD = 10, ROW = 16;
	private final Screen parent;
	private final HudMod target;
	private final List<HudMod> sources = new ArrayList<>();
	private int px, py, ph, scroll;

	public CopyStyleScreen(Screen parent, HudMod target) {
		super(Component.literal(target.name));
		this.parent = parent;
		this.target = target;
		for (HudMod h : Mods.allHuds()) if (h != target) sources.add(h);
	}

	@Override
	protected void init() {
		ph = Math.min(260, height - 48);
		px = (width - PW) / 2;
		py = Math.max(36, (height - ph) / 2);
	}

	private int top() {
		return py + 28;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (parent != null) parent.renderBackground(g, mouseX, mouseY, delta);
		g.fill(0, 0, width, height, 0x40000000);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		Ui.panel(g, px, py, PW, ph);
		Ui.text(g, Ui.fit(Ui.tr("hud.copy_from"), PW - 40, Ui.TITLE, false), px + PAD, py + 9, Ui.TEXT, Ui.TITLE);
		g.enableScissor(px, top(), px + PW, py + ph - PAD);
		for (int i = 0; i < sources.size(); i++) {
			int y = top() - scroll + i * ROW;
			boolean over = Ui.inside(mouseX, mouseY, px + PAD, y, PW - PAD * 2, ROW) && mouseY >= top();
			if (over) Ui.round(g, px + PAD, y, PW - PAD * 2, ROW - 2, Ui.R_SM, Ui.HOVER);
			Ui.modIcon(g, sources.get(i).id, px + PAD + 3, y + 1, 14, Ui.SOFT);
			Ui.text(g, Ui.fit(sources.get(i).name, PW - PAD * 2 - 26, Ui.UI, false), px + PAD + 22, y + 4, Ui.TEXT, Ui.UI);
		}
		g.disableScissor();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int i = 0; i < sources.size(); i++) {
			if (event.y() >= top() && Ui.inside(event.x(), event.y(), px + PAD, top() - scroll + i * ROW, PW - PAD * 2, ROW)) {
				target.copyStyleFrom(sources.get(i));
				Mods.save();
				onClose();
				return true;
			}
		}
		if (!Ui.inside(event.x(), event.y(), px, py, PW, ph)) onClose();
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll = (int) Math.max(0, Math.min(Math.max(0, sources.size() * ROW - (ph - 40)), scroll - scrollY * 16));
		return true;
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

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
	private int logoW, top;

	public HubScreen() {
		super(Component.literal("Newgen Client"));
	}

	@Override
	protected void init() {
		HudMod.editing = true;
		// The logo shrinks with the window and goes away before it would push the row off screen.
		logoW = Math.max(0, Math.min(Math.min(150, width - 40), (height - 90) * 4));
		int logoH = logoW >= 40 ? Ui.logoHeight(logoW) + 10 : 0;
		top = (height - logoH - ROW_H) / 2;
		int y = top + logoH, x = (width - MODS_W) / 2;
		addRenderableWidget(new Ui.Btn(x - GAP - ROW_H, y, ROW_H, ROW_H, "", Ui.I_GEAR, Ui.DARK, Ui.CAPS,
				() -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))).narration(I18n.get("menu.options")));
		addRenderableWidget(new Ui.Btn(x, y, MODS_W, ROW_H, Ui.tr("mods"), Ui.DARK, Ui.CAPS, () -> minecraft.setScreen(new ModsScreen(null))));
		addRenderableWidget(new Ui.Btn(x + MODS_W + GAP, y, ROW_H, ROW_H, "", Ui.I_GLOBE, Ui.DARK, Ui.CAPS,
				() -> minecraft.setScreen(new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager())))
				.narration(I18n.get("options.language")));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// In a world: the live game with only a light shade, no blur. Without one: the panorama.
		if (minecraft.level == null) renderPanorama(g, delta);
		else g.fill(0, 0, width, height, 0x30000000);
	}

	/** The elements on screen. Without a world there is nothing they could show. */
	private List<HudMod> huds() {
		return minecraft.level == null ? List.of() : Mods.huds();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// Drawn here and not by the game's HUD layer, so they stay above this screen's shade.
		HudMod hot = dragging != null ? dragging : at(mouseX, mouseY);
		for (HudMod hud : huds()) {
			hud.render(g, minecraft);
			if (hud != hot) frame(g, hud, Ui.LINE);
		}
		if (hot != null) frame(g, hot, Ui.BLUE); // last, so its name is never hidden by a neighbour

		Ui.logo(g, width / 2, top, logoW);
		super.render(g, mouseX, mouseY, delta);
		if (minecraft.level != null) Ui.centered(g, Ui.fit(Ui.tr("hub.hint"), width - 8, Ui.UI, false), width / 2, height - 16, Ui.SOFT, Ui.UI);
	}

	private void frame(GuiGraphics g, HudMod hud, int color) {
		g.renderOutline(hud.left - 1, hud.top - 1, hud.width + 2, hud.height + 2, color);
		// Name tag above the element, or below it when there is no room; always fully on screen.
		int w = Ui.width(hud.name, Ui.UI) + 8;
		int x = Math.max(0, Math.min(hud.left - 1, width - w));
		int y = hud.top >= 13 ? hud.top - 13 : Math.min(hud.top + hud.height + 1, height - 12);
		Ui.round(g, x, y, w, 12, Ui.R_SM, Ui.GLASS);
		Ui.text(g, hud.name, x + 4, y + 2, color | 0xFF000000, Ui.UI);
	}

	/** The element under the mouse. Later mods are drawn on top, so they win when elements overlap. */
	private HudMod at(double mx, double my) {
		HudMod found = null;
		for (HudMod hud : huds()) {
			if (Ui.inside(mx, my, hud.left, hud.top, hud.width, hud.height)) found = hud;
		}
		return found;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true; // the MODS button
		HudMod hit = at(event.x(), event.y());
		if (event.button() == 1 && hit != null) {
			hit.x = hit.defaultX;
			hit.y = hit.defaultY;
			Mods.save();
			return true;
		}
		dragging = event.button() == 0 ? hit : null;
		if (dragging == null) return false;
		grabX = event.x() - dragging.left;
		grabY = event.y() - dragging.top;
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) return super.mouseDragged(event, dx, dy);
		// x and y are fractions of the free space, so an element can never leave the screen.
		int freeX = width - dragging.width, freeY = height - dragging.height;
		dragging.x = freeX <= 0 ? 0 : Mods.clamp01((float) ((event.x() - grabX) / freeX));
		dragging.y = freeY <= 0 ? 0 : Mods.clamp01((float) ((event.y() - grabY) / freeY));
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			Mods.save();
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		HudMod hit = dragging != null ? dragging : at(mouseX, mouseY);
		if (hit == null || scrollY == 0) return false;
		hit.scale.set(hit.scale.get() + Math.signum(scrollY) * hit.scale.step);
		Mods.save();
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (NewgenClient.OPEN_MODS.matches(event)) {
			onClose();
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

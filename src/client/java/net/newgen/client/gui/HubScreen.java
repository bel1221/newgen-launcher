package net.newgen.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.newgen.client.NewgenClient;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Mods;

/**
 * What Right Shift opens: the game stays visible (blurred), the NewGen logo with a MODS button sits in the middle,
 * and the HUD elements stay on screen where they can be arranged: drag to move, mouse wheel resizes, right click puts back.
 */
public class HubScreen extends Screen {
	private static final String[] HINT = {"hub.hint.move", "hub.hint.resize"};

	private HudMod dragging;
	private double grabX, grabY; // where inside the element the mouse grabbed it
	private int logoW, top;

	public HubScreen() {
		super(Component.literal("Newgen Client"));
	}

	@Override
	protected void init() {
		HudMod.editing = true;
		int buttonW = Math.min(160, width - 40);
		// The logo shrinks with the window and goes away before it would push the button off screen.
		logoW = Math.max(0, Math.min(Math.min(190, width - 40), (height - 90) * 4));
		int logoH = logoW >= 40 ? Ui.logoHeight(logoW) + 6 : 0;
		top = (height - logoH - 20) / 2;
		addRenderableWidget(new Ui.Btn((width - buttonW) / 2, top + logoH, buttonW, 20, Ui.tr("mods"), Ui.DARK, Ui.CAPS, () -> minecraft.setScreen(new ModsScreen(null))));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// The game's own blur (strength follows Options > Accessibility > Menu Background Blur), no dark overlay.
		if (minecraft.level == null) renderPanorama(g, delta);
		renderBlurredBackground(g);
	}

	/** The elements on screen. Without a world there is nothing they could show. */
	private List<HudMod> huds() {
		return minecraft.level == null ? List.of() : Mods.huds();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// Drawn here and not by the game's HUD layer, so the elements stay sharp on top of the blur.
		HudMod hot = dragging != null ? dragging : at(mouseX, mouseY);
		for (HudMod hud : huds()) {
			hud.render(g, minecraft);
			if (hud != hot) frame(g, hud, Ui.LINE);
		}
		if (hot != null) frame(g, hot, Ui.BLUE); // last, so its name is never hidden by a neighbour

		Ui.logo(g, width / 2, top, logoW);
		super.render(g, mouseX, mouseY, delta);
		int y = top + (logoW >= 40 ? Ui.logoHeight(logoW) + 6 : 0) + 26;
		if (minecraft.level != null && y + 20 < height) {
			for (int i = 0; i < HINT.length; i++) Ui.centered(g, Ui.fit(Ui.tr(HINT[i]), width - 8, Ui.UI, false), width / 2, y + i * 11, Ui.SOFT, Ui.UI);
		}
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

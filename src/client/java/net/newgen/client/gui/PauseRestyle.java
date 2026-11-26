package net.newgen.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * The pause menu in our system (over the game's own blur): every vanilla button drawn as ours (the vanilla
 * one stays, hidden, and does the work), and two of ours on top: NewGen mods and Edit HUD.
 */
public final class PauseRestyle {
	private PauseRestyle() {
	}

	public static void attach(Minecraft mc, Screen screen, int width, int height) {
		if (!(screen instanceof PauseScreen)) return;
		List<AbstractWidget> widgets = Screens.getButtons(screen);
		List<Button> vanilla = new ArrayList<>();
		// Vanilla builds its buttons as Button.Plain (a subclass): every Button that is not ours is redrawn as ours.
		for (AbstractWidget w : widgets) if (w instanceof Button && !(w instanceof Ui.Btn) && w.visible) vanilla.add((Button) w);
		if (vanilla.isEmpty()) return; // the menu without buttons (F3+Esc)
		int left = Integer.MAX_VALUE, right = Integer.MIN_VALUE, top = Integer.MAX_VALUE, bottom = Integer.MIN_VALUE;
		for (Button b : vanilla) {
			left = Math.min(left, b.getX());
			right = Math.max(right, b.getX() + b.getWidth());
			top = Math.min(top, b.getY());
			bottom = Math.max(bottom, b.getY() + b.getHeight());
		}
		List<AbstractWidget> ours = new ArrayList<>();
		for (Button b : vanilla) {
			Ui.Btn btn = new Ui.Btn(b.getX(), b.getY(), b.getWidth(), b.getHeight(), b.getMessage().getString(), Ui.DARK, Ui.CAPS,
					() -> b.onPress(new net.minecraft.client.input.KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0)));
			btn.active = b.active;
			b.visible = false;
			ours.add(btn);
		}
		// Ours under the column (above it is the menu title): NewGen mods and Edit HUD, side by side.
		int half = (right - left - 4) / 2, y = Math.min(bottom + 4, height - 24);
		ours.add(new Ui.Btn(left, y, half, 20, Ui.tr("pause.mods"), Ui.I_GRID, Ui.PRIMARY, Ui.CAPS, () -> mc.setScreen(new ModsScreen(screen))));
		ours.add(new Ui.Btn(right - half, y, half, 20, Ui.tr("pause.edit_hud"), Ui.I_LAYOUT, Ui.DARK, Ui.CAPS, () -> mc.setScreen(new HubScreen())));
		widgets.addAll(ours);
		// The glass panel behind the column: index 0 of Fabric's button list is placed before the first button in
		// the screen's draw order, so it is drawn after the blur and under every button.
		widgets.add(0, new Panel(left - 10, top - 10, right - left + 20, y + 20 + 10 - (top - 10)));
	}

	/** The glass panel behind the buttons: takes no clicks, no focus, no narration. */
	private static final class Panel extends AbstractWidget {
		Panel(int x, int y, int w, int h) {
			super(x, y, w, h, net.minecraft.network.chat.Component.empty());
			active = false;
		}

		@Override
		protected void renderWidget(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float delta) {
			Ui.panel(g, getX(), getY(), getWidth(), getHeight());
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return false;
		}

		@Override
		protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
		}
	}
}

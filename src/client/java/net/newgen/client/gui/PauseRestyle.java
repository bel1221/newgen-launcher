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
		for (AbstractWidget w : widgets) if (w.getClass() == Button.class && w.visible) vanilla.add((Button) w);
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
		ours.add(new Ui.Btn(left, y, half, 20, Ui.tr("mods"), Ui.I_GRID, Ui.PRIMARY, Ui.CAPS, () -> mc.setScreen(new ModsScreen(screen))));
		ours.add(new Ui.Btn(right - half, y, half, 20, Ui.tr("hud.edit"), Ui.I_LAYOUT, Ui.DARK, Ui.CAPS, () -> mc.setScreen(new HubScreen())));
		widgets.addAll(ours);
	}

}

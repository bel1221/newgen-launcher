package net.newgen.client.mods.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

/** Asks before quitting the game or leaving a server. Hook: MinecraftConfirmQuitMixin. */
public class ConfirmQuitMod extends Mod {
	public static ConfirmQuitMod I;

	private final Setting.Bool quit = bool("confirm_quit", "Confirm quitting the game", true);
	private final Setting.Bool disconnect = bool("confirm_disconnect", "Confirm leaving a server", true);
	private final Setting.Bool singleplayer = bool("confirm_singleplayer", "Also when leaving a single-player world", false);

	private boolean quitConfirmed, disconnectConfirmed, asking;

	public ConfirmQuitMod() {
		super("confirm_quit", "Confirm Quit", "Asks before you quit the game or disconnect from a server");
		I = this;
	}

	/** Called at the start of Minecraft.stop(); true = stop was turned into a question. */
	public static boolean interceptQuit(Minecraft mc) {
		ConfirmQuitMod m = I;
		if (m == null || !m.enabled || !m.quit.value || m.quitConfirmed) return false;
		GLFW.glfwSetWindowShouldClose(mc.getWindow().handle(), false); // the close button would ask again every tick
		if (m.asking) return true;
		m.asking = true;
		Screen previous = mc.screen;
		mc.setScreen(new ConfirmScreen(yes -> {
			m.asking = false;
			if (yes) {
				m.quitConfirmed = true;
				mc.stop();
			} else {
				mc.setScreen(previous);
			}
		}, Component.translatable("newgen_client.confirm_quit.quit_title"), Component.translatable("newgen_client.confirm_quit.quit_message")));
		return true;
	}

	/** Called at the start of Minecraft.disconnectFromWorld (the pause menu button); true = turned into a question. */
	public static boolean interceptDisconnect(Minecraft mc, Component reason) {
		ConfirmQuitMod m = I;
		if (m == null) return false;
		if (m.disconnectConfirmed) {
			m.disconnectConfirmed = false;
			return false;
		}
		if (!m.enabled || !m.disconnect.value || mc.level == null) return false;
		if (mc.isLocalServer() && !m.singleplayer.value) return false;
		Screen previous = mc.screen;
		mc.setScreen(new ConfirmScreen(yes -> {
			if (yes) {
				m.disconnectConfirmed = true;
				mc.disconnectFromWorld(reason);
			} else {
				// The pause menu greys its button out once clicked: open a fresh one.
				mc.setScreen(previous instanceof PauseScreen || previous == null ? new PauseScreen(true) : previous);
			}
		}, Component.translatable("newgen_client.confirm_quit.disconnect_title"), Component.translatable("newgen_client.confirm_quit.disconnect_message")));
		return true;
	}
}

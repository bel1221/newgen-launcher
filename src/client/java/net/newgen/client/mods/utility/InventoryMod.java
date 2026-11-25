package net.newgen.client.mods.utility;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

/** Keeps the mouse where it was when one inventory screen closes and the next opens soon after. */
public class InventoryMod extends Mod {
	private final Setting.Num timeout = num("timeout", "Keep cursor for (seconds)", 2.0, 0.5, 10.0, 0.5);
	private final Setting.Bool chestsOnly = bool("chests_only", "Containers only (not own inventory)", false);

	private double savedX, savedY;
	private long savedAt;

	public InventoryMod() {
		super("inventory_mod", "Keep Cursor", "Do not re-centre the cursor between inventory screens");
		ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
			if (!(screen instanceof AbstractContainerScreen<?>)) return;
			if (chestsOnly.value && (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen)) return;
			ScreenEvents.remove(screen).register(s -> {
				savedX = mc.mouseHandler.xpos();
				savedY = mc.mouseHandler.ypos();
				savedAt = System.currentTimeMillis();
			});
			// savedAt is cleared after use, so a window resize (which also re-inits the screen) does nothing.
			if (enabled && savedAt != 0 && System.currentTimeMillis() - savedAt <= timeout.get() * 1000) {
				GLFW.glfwSetCursorPos(mc.getWindow().handle(), savedX, savedY);
			}
			savedAt = 0;
		});
	}
}

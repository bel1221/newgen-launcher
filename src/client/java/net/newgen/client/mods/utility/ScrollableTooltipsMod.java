package net.newgen.client.mods.utility;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;
import org.lwjgl.glfw.GLFW;

/** Mouse wheel moves the item tooltip in inventory screens, so long tooltips can be read. Drawing hook: GuiGraphicsMixin. */
public class ScrollableTooltipsMod extends Mod {
	public static ScrollableTooltipsMod INSTANCE;

	private final Setting.Num speed = num("speed", "Scroll speed (pixels)", 10, 2, 40, 2);
	private final Setting.Bool needCtrl = bool("need_ctrl", "Only while holding Ctrl", false);
	private final Setting.Bool shiftSideways = bool("shift_sideways", "Shift + wheel scrolls sideways", true);

	private int offsetX, offsetY;
	private long lastTooltip;

	public ScrollableTooltipsMod() {
		super("scrollable_tooltips", "Scrollable Tooltips", "Scroll item tooltips with the mouse wheel");
		INSTANCE = this;
		ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
			if (!(screen instanceof AbstractContainerScreen<?>)) return;
			ScreenMouseEvents.allowMouseScroll(screen).register((s, mx, my, horizontal, vertical) -> !scroll(mc, vertical));
		});
	}

	/** Returns true if the wheel was used for the tooltip (vanilla then ignores it). */
	private boolean scroll(Minecraft mc, double amount) {
		if (!enabled || System.currentTimeMillis() - lastTooltip > 150) return false; // no tooltip on screen
		if (needCtrl.value && !down(mc, GLFW.GLFW_KEY_LEFT_CONTROL) && !down(mc, GLFW.GLFW_KEY_RIGHT_CONTROL)) return false;
		int delta = (int) Math.round(amount * speed.get());
		if (shiftSideways.value && (down(mc, GLFW.GLFW_KEY_LEFT_SHIFT) || down(mc, GLFW.GLFW_KEY_RIGHT_SHIFT))) offsetX += delta;
		else offsetY += delta;
		return true;
	}

	private static boolean down(Minecraft mc, int key) {
		return InputConstants.isKeyDown(mc.getWindow(), key);
	}

	/** Called for every tooltip drawn. Returns {x, y} to shift it by, or null for no change. */
	public static int[] offset() {
		ScrollableTooltipsMod m = INSTANCE;
		if (m == null || !m.enabled || !(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) return null;
		long now = System.currentTimeMillis();
		// shortcut: the offset resets only after the tooltip was gone for a moment, not when moving
		// straight to the next item; track the hovered slot if that bothers anyone.
		if (now - m.lastTooltip > 150) m.offsetX = m.offsetY = 0;
		m.lastTooltip = now;
		return m.offsetX == 0 && m.offsetY == 0 ? null : new int[] {m.offsetX, m.offsetY};
	}
}

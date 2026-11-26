package net.newgen.client.mods.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

// Wave 3 utility 1
/** Inventory screens: own GUI scale, click outside to close, background dim. Hooks: InventoryScaleMixin,
 * ContainerDimMixin, AbstractContainerScreenMixin. */
public class InventoryTweaksMod extends Mod {
	public static InventoryTweaksMod INSTANCE;

	private final Setting.Choice scale = choice("gui_scale", "Inventory GUI scale", "Unchanged", "Unchanged", "Auto", "1", "2", "3", "4", "5", "6");
	final Setting.Bool clickOutside = bool("click_outside", "Click outside to close", false);
	private final Setting.Num dim = num("dim", "Background opacity (%)", 75, 0, 100, 5);

	/** True while the window runs at the inventory scale. */
	private boolean scaled;

	public InventoryTweaksMod() {
		super("inventory_tweaks", "Inventory Tweaks", "GUI scale, click-outside closing and background for inventories");
		INSTANCE = this;
	}

	private static boolean wantsScale(Screen screen) {
		InventoryTweaksMod m = INSTANCE;
		return m != null && m.enabled && !m.scale.value.equals("Unchanged") && screen instanceof AbstractContainerScreen<?>;
	}

	/** The GUI scale resizeDisplay should use, given the one vanilla picked. */
	public static int guiScale(Minecraft mc, int vanilla) {
		if (!wantsScale(mc.screen)) return vanilla;
		int asked = INSTANCE.scale.value.equals("Auto") ? 0 : Integer.parseInt(INSTANCE.scale.value);
		return mc.getWindow().calculateScale(asked, mc.isEnforceUnicode());
	}

	/** After a screen opens or closes: switch the window to or from the inventory scale. */
	public static void afterSetScreen(Minecraft mc) {
		InventoryTweaksMod m = INSTANCE;
		if (m == null) return;
		boolean want = wantsScale(mc.screen);
		if (want == m.scaled) return; // one container to the next keeps the scale it already has
		m.scaled = want;
		mc.resizeDisplay();
	}

	public static boolean clickOutside() {
		return INSTANCE != null && INSTANCE.enabled && INSTANCE.clickOutside.value;
	}

	/** Background dim colours (top, bottom) for container screens, or null for vanilla. */
	public static int[] dim() {
		InventoryTweaksMod m = INSTANCE;
		if (m == null || !m.enabled || m.dim.get() == 75) return null;
		double f = m.dim.get() / 75.0; // 75 % is vanilla's look
		int top = (int) Math.min(255, Math.round(0xC0 * f)), bottom = (int) Math.min(255, Math.round(0xD0 * f));
		return new int[] {top << 24 | 0x101010, bottom << 24 | 0x101010};
	}
}

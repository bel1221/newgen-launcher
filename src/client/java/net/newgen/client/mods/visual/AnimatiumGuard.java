package net.newgen.client.mods.visual;


import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Keeps players inside our Legacy Animations page: Animatium's own config screen (opened by its keybind, its Mod Menu
 * entry, its command or any YACL screen) is refused, and its open-config key is removed from Controls and unbound,
 * even when options.txt binds it. Our page writes its values itself; no Animatium code is copied.
 */
public final class AnimatiumGuard {
	private static final String KEYBINDS = "org.visuals.legacy.animatium.handler.AnimatiumKeybinds";
	private static KeyMapping configKey;
	private static boolean lookedUp;

	private AnimatiumGuard() {
	}

	/** Whether a screen would open Animatium's config (its own package or a YACL screen). */
	public static boolean blocked(Screen screen) {
		if (screen == null) return false;
		String name = screen.getClass().getName();
		return name.startsWith("org.visuals.legacy.animatium") || name.startsWith("dev.isxander.yacl3");
	}

	/** Each tick: the open-config key is unbound and taken out of the Controls list (also when options.txt binds it). */
	public static void strip(Minecraft mc) {
		if (!lookedUp) {
			lookedUp = true;
			try {
				Class<?> k = Class.forName(KEYBINDS);
				configKey = (KeyMapping) k.getMethod("getCONFIG_SCREEN").invoke(k.getField("INSTANCE").get(null));
			} catch (Throwable e) {
				configKey = null; // Animatium absent or changed: nothing to remove
			}
		}
		if (configKey == null || mc.options == null) return;
		configKey.setKey(InputConstants.UNKNOWN);
	}
}

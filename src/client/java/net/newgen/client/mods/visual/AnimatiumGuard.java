package net.newgen.client.mods.visual;

import java.util.Arrays;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.newgen.client.mixin.visual.OptionsAccessor;

/**
 * Keeps players inside our Legacy Animations page: Animatium's own config screen (its keybind, Mod Menu entry, command
 * or YACL screen) is refused, and its open-config key is taken out of Controls and unbound, even when options.txt binds
 * it. No Animatium code is copied.
 */
public final class AnimatiumGuard {
	private static final String KEYBINDS = "org.visuals.legacy.animatium.handler.AnimatiumKeybinds";
	private static KeyMapping configKey;
	private static boolean lookedUp;

	private AnimatiumGuard() {
	}

	/** Whether a screen would open Animatium's config: its own package, or a YACL screen titled as Animatium's config. */
	public static boolean blocked(Screen screen) {
		if (screen == null) return false;
		String name = screen.getClass().getName();
		if (name.startsWith("org.visuals.legacy.animatium")) return true;
		// YACL is also the settings library of other mods: only a screen titled as Animatium's config is refused
		return name.startsWith("dev.isxander.yacl3") && screen.getTitle().getString().contains("Animatium");
	}

	/** Each tick: the open-config key is unbound and removed from the Controls list (also when options.txt binds it). */
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
		OptionsAccessor options = (OptionsAccessor) (Object) mc.options;
		KeyMapping[] all = options.newgen$keyMappings();
		if (Arrays.asList(all).contains(configKey)) {
			options.newgen$setKeyMappings(Arrays.stream(all).filter(m -> m != configKey).toArray(KeyMapping[]::new));
		}
	}
}

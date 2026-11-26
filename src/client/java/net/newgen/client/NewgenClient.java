package net.newgen.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Mods;
import net.newgen.client.gui.HubScreen;
import net.newgen.client.gui.NewgenTitleScreen;
import net.newgen.client.mods.CoordinatesMod;
import net.newgen.client.mods.CpsMod;
import net.newgen.client.mods.FpsMod;
import org.lwjgl.glfw.GLFW;

/** Entry point: registers the mods, the menu key and the HUD drawing. */
public class NewgenClient implements ClientModInitializer {
	public static final String ID = "newgen_client";

	/** Opens the Newgen Mods menu. Rebindable in Options > Controls > Key Binds. */
	public static final KeyMapping OPEN_MODS = new KeyMapping(
			"key.newgen_client.open_mods", GLFW.GLFW_KEY_RIGHT_SHIFT,
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main")));

	@Override
	public void onInitializeClient() {
		// To add a mod: write a class in the "mods" package and register it here.
		Mods.register(new CpsMod());
		Mods.register(new FpsMod());
		Mods.register(new CoordinatesMod());
		net.newgen.client.mods.ThemeMod theme = new net.newgen.client.mods.ThemeMod();
		Mods.register("Visual", theme);
		// Legacy animations: a curated page over Animatium (a separate bundled mod), only when it is installed.
		net.newgen.client.mods.visual.AnimatiumMod animatium = net.newgen.client.mods.visual.AnimatiumMod.available()
				? new net.newgen.client.mods.visual.AnimatiumMod() : null;
		if (animatium != null) Mods.register("Visual", animatium);
		net.newgen.client.mods.hud.HudMods.register();
		net.newgen.client.mods.visual.VisualMods.register();
		net.newgen.client.mods.controls.ControlsMods.register();
		net.newgen.client.mods.utility.UtilityMods.register();
		Mods.load();

		KeyBindingHelper.registerKeyBinding(OPEN_MODS);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			theme.tick();
			if (animatium != null) animatium.tick();
			while (OPEN_MODS.consumeClick()) {
				if (mc.screen == null) mc.setScreen(new HubScreen());
			}
		});

		// NewGen's title screen replaces the vanilla one whenever that is shown, also when another screen goes "back" to it.
		ScreenEvents.AFTER_INIT.register((mc, screen, width, height) -> {
			if (screen instanceof TitleScreen) mc.setScreen(new NewgenTitleScreen());
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(ID, "hud"), (graphics, delta) -> {
			Minecraft mc = Minecraft.getInstance();
			// The Right Shift screen draws the elements itself, on top of its blur.
			if (mc.options.hideGui || mc.screen instanceof HubScreen) return;
			for (HudMod hud : Mods.huds()) hud.render(graphics, mc);
		});
	}
}

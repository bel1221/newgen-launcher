package net.newgen.client.mods.controls;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.newgen.client.core.Mods;

/** Registers every mod of the "Controls" group. Add one line per mod: Mods.register(CATEGORY, new XMod()); */
public final class ControlsMods {
	public static final String CATEGORY = "Controls";

	private ControlsMods() {
	}

	public static void register() {
		FreelookMod freelook = new FreelookMod();
		SnaplookMod snaplook = new SnaplookMod();
		ToggleSneakMod toggleSneak = new ToggleSneakMod();
		AutoTextHotkeyMod hotkeys = new AutoTextHotkeyMod();
		AutoTextActionsMod actions = new AutoTextActionsMod();
		Mods.register(CATEGORY, freelook);
		Mods.register(CATEGORY, snaplook);
		Mods.register(CATEGORY, toggleSneak);
		Mods.register(CATEGORY, hotkeys);
		Mods.register(CATEGORY, actions);

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			freelook.tick(mc);
			snaplook.tick(mc);
			toggleSneak.tick(mc);
			hotkeys.tick(mc);
			actions.tick(mc);
		});
	}

	/** Sends a chat message, or a command if the text starts with "/". */
	static void send(Minecraft mc, String text) {
		text = text.trim();
		if (text.isEmpty() || mc.player == null) return;
		if (text.length() > 256) text = text.substring(0, 256); // server chat limit
		if (text.startsWith("/")) mc.player.connection.sendCommand(text.substring(1));
		else mc.player.connection.sendChat(text);
	}
}

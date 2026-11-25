package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;

public class ServerAddressMod extends HudMod {
	public ServerAddressMod() {
		super("server_address", "Server Address", "Address of the server you are on", 0f, 0.16f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.getCurrentServer() != null) return List.of(mc.getCurrentServer().ip);
		return mc.hasSingleplayerServer() ? List.of("Singleplayer") : List.of();
	}
}

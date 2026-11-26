package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.newgen.client.core.HudMod;

/** The world seed in singleplayer (read from the built-in server); hidden on servers, which never send it. */
public class SeedMod extends HudMod {
	public SeedMod() {
		super("seed", "Seed", "The world seed, in singleplayer only", 0f, 0.42f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null || mc.level == null) return List.of();
		return List.of(I18n.get("newgen_client.hud.seed.text", server.overworld().getSeed()));
	}
}

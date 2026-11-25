package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.newgen.client.core.HudMod;

public class PingMod extends HudMod {
	public PingMod() {
		super("ping", "Ping", "Your latency to the server (as the server reports it)", 0f, 0.12f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null || mc.getConnection() == null) return List.of();
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		return info == null ? List.of() : List.of(info.getLatency() + " ms");
	}
}

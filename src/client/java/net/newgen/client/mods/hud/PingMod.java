package net.newgen.client.mods.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.newgen.client.core.Setting;

public class PingMod extends ValueHud {
	private final Setting.Choice mode = choice("display_mode", "Display", "Number and unit", "Number and unit", "Number only", "Bars");

	public PingMod() {
		super("ping", "Ping", "Your latency to the server (as the server reports it)", 0f, 0.18f, 80, 150, 1000);
	}

	@Override
	protected double sample(Minecraft mc) {
		if (mc.player == null || mc.getConnection() == null) return Double.NaN;
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		return info == null ? Double.NaN : info.getLatency();
	}

	@Override
	protected String text(double value, Minecraft mc) {
		int ms = (int) value;
		return switch (mode.value) {
			case "Number only" -> String.valueOf(ms);
			case "Bars" -> bars(ms);
			default -> ms + " ms";
		};
	}

	/** Five bars like the player list: full under 150 ms, one fewer per step up. */
	static String bars(int ms) {
		int n = ms < 0 ? 0 : ms < 150 ? 5 : ms < 300 ? 4 : ms < 600 ? 3 : ms < 1000 ? 2 : 1;
		return "|".repeat(n) + ".".repeat(5 - n);
	}
}

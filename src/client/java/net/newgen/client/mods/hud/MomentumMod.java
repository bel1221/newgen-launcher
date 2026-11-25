package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class MomentumMod extends HudMod {
	private final Setting.Bool groundSpeed = bool("ground_speed", "Ignore vertical speed", true);

	public MomentumMod() {
		super("momentum", "Momentum", "Your speed in blocks per second", 0f, 0.3f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null) return List.of();
		// Distance moved in the last game tick; 20 ticks = 1 second.
		double dx = mc.player.getX() - mc.player.xo, dz = mc.player.getZ() - mc.player.zo;
		double dy = groundSpeed.value ? 0 : mc.player.getY() - mc.player.yo;
		return List.of(String.format("%.2f m/s", Math.sqrt(dx * dx + dy * dy + dz * dz) * 20));
	}
}

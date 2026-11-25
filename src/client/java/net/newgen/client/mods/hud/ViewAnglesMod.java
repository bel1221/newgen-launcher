package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class ViewAnglesMod extends HudMod {
	private final Setting.Bool singleLine = bool("single_line", "Single line", true);

	public ViewAnglesMod() {
		super("view_angles", "View Angles", "Yaw and pitch of your view", 0f, 0.36f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null) return List.of();
		String yaw = String.format("Yaw: %.1f", Mth.wrapDegrees(mc.player.getYRot()));
		String pitch = String.format("Pitch: %.1f", mc.player.getXRot());
		return singleLine.value ? List.of(yaw + "  " + pitch) : List.of(yaw, pitch);
	}
}

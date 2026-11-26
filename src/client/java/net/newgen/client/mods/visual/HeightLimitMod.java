package net.newgen.client.mods.visual;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class HeightLimitMod extends HudMod {
	public static final HeightLimitMod I = new HeightLimitMod();
	private final Setting.Bool world = bool("world", "Use the build limit of the world", true);
	private final Setting.Num limit = num("limit", "Custom height limit (Y)", 100, -64, 320, 1);
	private final Setting.Bool overlay = bool("overlay", "Show a grid at the limit when close", true);
	private final Setting.Color overlayColor = color("overlay_color", "Grid colour", 0xFFFF3030);
	private final Setting.Num near = num("near", "Show grid within (blocks)", 8, 2, 32, 1);
	private final Setting.Num gridSize = num("grid_size", "Grid size (blocks each way)", 6, 2, 16, 1);

	private HeightLimitMod() {
		super("height_limit", "Height Limit", "Blocks left until the height limit", 0f, 0.3f);
	}

	private int limit(Minecraft mc) {
		return world.value ? mc.level.getMaxY() + 1 : (int) limit.get();
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null || mc.level == null) return List.of();
		return List.of(net.minecraft.client.resources.language.I18n.get("newgen_client.hud.height_limit.text", limit(mc) - mc.player.getBlockY()));
	}

	/** Called once per frame while the level is drawn. */
	public void emit(Minecraft mc, Entity camera) {
		if (!enabled || !overlay.value) return;
		int y = limit(mc);
		if (Math.abs(y - camera.getY()) > near.get()) return;
		int RANGE = (int) gridSize.get();
		int cx = Mth.floor(camera.getX()), cz = Mth.floor(camera.getZ());
		for (int i = -RANGE; i <= RANGE + 1; i++) {
			Gizmos.line(new Vec3(cx + i, y, cz - RANGE), new Vec3(cx + i, y, cz + RANGE + 1), overlayColor.value, 1.5f);
			Gizmos.line(new Vec3(cx - RANGE, y, cz + i), new Vec3(cx + RANGE + 1, y, cz + i), overlayColor.value, 1.5f);
		}
	}
}

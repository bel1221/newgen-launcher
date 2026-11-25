package net.newgen.client.mods.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class ChunkBordersMod extends Mod {
	public static final ChunkBordersMod I = new ChunkBordersMod();
	private final Setting.Color color = color("color", "Colour", 0xFFFFE000);
	private final Setting.Num width = num("width", "Line width", 2, 1, 5, 0.5);
	private final Setting.Bool grid = bool("grid", "Lines every 4 blocks on the walls", false);

	private ChunkBordersMod() {
		super("chunk_borders", "Chunk Borders", "Outlines the chunk you stand in");
	}

	/** Called once per frame while the level is drawn. */
	public void emit(Minecraft mc, Entity camera) {
		if (!enabled) return;
		int x0 = camera.chunkPosition().getMinBlockX(), z0 = camera.chunkPosition().getMinBlockZ();
		int minY = mc.level.getMinY(), maxY = mc.level.getMaxY() + 1;
		int c = color.value;
		float w = (float) width.get();
		int step = grid.value ? 4 : 16;
		for (int i = 0; i <= 16; i += step) {
			vertical(x0 + i, z0, minY, maxY, c, w);
			vertical(x0 + i, z0 + 16, minY, maxY, c, w);
			if (i == 0 || i == 16) continue;
			vertical(x0, z0 + i, minY, maxY, c, w);
			vertical(x0 + 16, z0 + i, minY, maxY, c, w);
		}
		// Rings every 16 blocks, only near the player so the view stays clear.
		int base = Mth.floor(camera.getY() / 16) * 16;
		for (int y = Math.max(minY, base - 32); y <= Math.min(maxY, base + 48); y += 16) {
			Gizmos.line(new Vec3(x0, y, z0), new Vec3(x0 + 16, y, z0), c, w);
			Gizmos.line(new Vec3(x0 + 16, y, z0), new Vec3(x0 + 16, y, z0 + 16), c, w);
			Gizmos.line(new Vec3(x0 + 16, y, z0 + 16), new Vec3(x0, y, z0 + 16), c, w);
			Gizmos.line(new Vec3(x0, y, z0 + 16), new Vec3(x0, y, z0), c, w);
		}
	}

	private static void vertical(int x, int z, int minY, int maxY, int color, float width) {
		Gizmos.line(new Vec3(x, minY, z), new Vec3(x, maxY, z), color, width);
	}
}

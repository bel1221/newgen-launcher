package net.newgen.client.mods.visual;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class LightOverlayMod extends Mod {
	public static final LightOverlayMod I = new LightOverlayMod();
	private static final int MAX_MARKS = 1000;
	private final Setting.Num range = num("range", "Range (blocks)", 12, 4, 24, 1);
	private final Setting.Color always = color("always", "Mobs can spawn any time", 0xFFFF3030);
	private final Setting.Color night = color("night", "Mobs can spawn at night", 0xFFFFE000);

	/** Marked blocks from the last scan: position and whether mobs can spawn there in daylight too. */
	private final List<BlockPos> marks = new ArrayList<>();
	private final List<Boolean> dark = new ArrayList<>();
	private long lastScan = Long.MIN_VALUE;

	private LightOverlayMod() {
		super("light_overlay", "Light Overlay", "Marks the blocks that are dark enough for monsters to spawn");
	}

	/** Called once per frame while the level is drawn. */
	public void emit(Minecraft mc, Entity camera) {
		if (!enabled) return;
		// Scanning every frame would be slow; twice a second is enough.
		long time = mc.level.getGameTime();
		if (Math.abs(time - lastScan) >= 10) {
			lastScan = time;
			scan(mc.level, camera.blockPosition());
		}
		for (int i = 0; i < marks.size(); i++) {
			BlockPos p = marks.get(i);
			int c = dark.get(i) ? always.value : night.value;
			double y = p.getY() + 0.03;
			Gizmos.line(new Vec3(p.getX() + 0.2, y, p.getZ() + 0.2), new Vec3(p.getX() + 0.8, y, p.getZ() + 0.8), c, 2f);
			Gizmos.line(new Vec3(p.getX() + 0.8, y, p.getZ() + 0.2), new Vec3(p.getX() + 0.2, y, p.getZ() + 0.8), c, 2f);
		}
	}

	private void scan(Level level, BlockPos center) {
		marks.clear();
		dark.clear();
		int r = (int) range.get();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(), below = new BlockPos.MutableBlockPos();
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				for (int y = -6; y <= 4; y++) {
					pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
					below.set(pos.getX(), pos.getY() - 1, pos.getZ());
					BlockState state = level.getBlockState(pos);
					// shortcut: "solid top below, free space, no block light" instead of the full per-mob spawn rules.
					if (!state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty()) continue;
					if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
					if (level.getBrightness(LightLayer.BLOCK, pos) > 0) continue;
					marks.add(pos.immutable());
					dark.add(level.getBrightness(LightLayer.SKY, pos) == 0);
					if (marks.size() >= MAX_MARKS) return;
				}
			}
		}
	}
}

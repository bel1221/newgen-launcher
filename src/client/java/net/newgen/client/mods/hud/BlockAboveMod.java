package net.newgen.client.mods.hud;

import java.util.List;
import java.util.Locale;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** Block Above: warns when a solid block is close above your head (low ceilings when jumping), with an optional ping. */
public class BlockAboveMod extends HudMod {
	private final Setting.Num height = num("check_height", "Check height (blocks)", 3, 1, 10, 1);
	private final Setting.Bool sound = bool("warn_sound", "Sound when it appears", false);
	private final Setting.Num volume = num("volume", "Volume", 0.5, 0.1, 1, 0.1);

	/** Gap in blocks between your head and the block above; NaN = none in range. Updated each tick. */
	private double gap = Double.NaN;

	public BlockAboveMod() {
		super("block_above", "Block Above", "Warns when a block is just above your head", 0.5f, 0.42f);
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	/** The gap between the head (headY) and a block's underside at blockY + shapeMinY, if within range; else NaN. */
	static double gapTo(double headY, int blockY, double shapeMinY, double range) {
		double g = blockY + shapeMinY - headY;
		return g >= -0.01 && g <= range ? Math.max(0, g) : Double.NaN;
	}

	private void tick(Minecraft mc) {
		if (!enabled || mc.player == null || mc.level == null) {
			gap = Double.NaN;
			return;
		}
		double before = gap;
		gap = find(mc.player, height.get());
		if (sound.value && Double.isNaN(before) && !Double.isNaN(gap))
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.5f, (float) volume.get()));
	}

	private static double find(Player player, double range) {
		double head = player.getBoundingBox().maxY;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int x = player.getBlockX(), z = player.getBlockZ();
		for (int y = (int) Math.floor(head); y <= (int) Math.floor(head + range); y++) {
			pos.set(x, y, z);
			BlockState state = player.level().getBlockState(pos);
			if (state.isAir()) continue;
			VoxelShape shape = state.getCollisionShape(player.level(), pos);
			if (shape.isEmpty()) continue;
			double g = gapTo(head, y, shape.min(Direction.Axis.Y), range);
			if (!Double.isNaN(g)) return g;
		}
		return Double.NaN;
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (Double.isNaN(gap)) return List.of();
		return List.of(I18n.get("newgen_client.block_above.text", String.format(Locale.ROOT, "%.1f", gap)));
	}
}

package net.newgen.client.mods.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.newgen.client.core.Mod;
import net.newgen.client.core.Setting;

public class BlockOutlineMod extends Mod {
	public static final BlockOutlineMod I = new BlockOutlineMod();

	public final Setting.Color color = color("color", "Colour", 0xFF00AAFF);
	public final Setting.Num width = num("width", "Line width", 2, 0.5, 5, 0.5);
	public final Setting.Bool fill = bool("fill", "Translucent fill", false);
	public final Setting.Color fillColor = color("fill_color", "Fill colour", 0x3000AAFF);

	private BlockOutlineMod() {
		super("block_outline", "Block Outline", "Colour, width and fill of the outline around the block you look at");
	}

	/** Called once per frame while the level is drawn: the see-through fill of the block looked at. */
	public void emit(Minecraft mc) {
		if (!enabled || !fill.value || mc.options.hideGui || !(mc.hitResult instanceof BlockHitResult hit)
				|| hit.getType() != HitResult.Type.BLOCK) return;
		BlockPos pos = hit.getBlockPos();
		VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
		if (shape.isEmpty()) return;
		int c = fillColor.current();
		// A little larger than the block so the fill does not flicker against its faces.
		for (AABB box : shape.toAabbs()) Gizmos.cuboid(box.move(pos).inflate(0.002), GizmoStyle.fill(c));
	}
}

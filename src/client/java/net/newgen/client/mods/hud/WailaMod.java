package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class WailaMod extends HudMod {
	private final Setting.Bool entities = bool("entities", "Show entities", true);

	private ItemStack icon = ItemStack.EMPTY;

	public WailaMod() {
		super("waila", "WAILA", "Name of the block or entity you look at", 0.5f, 0f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		icon = ItemStack.EMPTY;
		HitResult hit = mc.hitResult;
		if (mc.level == null || hit == null) return List.of();
		if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) {
			Block block = mc.level.getBlockState(b.getBlockPos()).getBlock();
			icon = new ItemStack(block);
			return List.of(block.getName().getString());
		}
		if (entities.value && hit instanceof EntityHitResult e) {
			String name = e.getEntity().getName().getString();
			return List.of(name);
		}
		return List.of();
	}

	@Override
	protected List<ItemStack> icons(Minecraft mc) {
		return icon.isEmpty() ? List.of() : List.of(icon);
	}
}

package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.newgen.client.core.HudMod;

public class CooldownsMod extends HudMod {
	private final List<ItemStack> stacks = new ArrayList<>();

	public CooldownsMod() {
		super("cooldowns", "Cooldowns", "Items in your inventory that are on cooldown", 0.6f, 0.5f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		stacks.clear();
		if (mc.player == null) return List.of();
		List<String> out = new ArrayList<>();
		Set<Item> seen = new HashSet<>();
		Inventory inv = mc.player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (s.isEmpty() || !seen.add(s.getItem())) continue;
			float left = mc.player.getCooldowns().getCooldownPercent(s, 0f);
			if (left <= 0) continue;
			stacks.add(s);
			out.add(Math.round(left * 100) + "%");
		}
		return out;
	}

	@Override
	protected List<ItemStack> icons(Minecraft mc) {
		return stacks;
	}
}

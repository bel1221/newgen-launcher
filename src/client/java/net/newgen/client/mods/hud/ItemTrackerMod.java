package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class ItemTrackerMod extends HudMod {
	private final Setting.Num duration = num("duration", "Popup seconds", 3, 1, 10, 1);

	private final Map<Item, Integer> before = new HashMap<>();
	/** Item -> {amount gained, time of the last gain}. */
	private final Map<Item, long[]> popups = new LinkedHashMap<>();
	private final List<ItemStack> stacks = new ArrayList<>();
	private Object lastPlayer;

	public ItemTrackerMod() {
		super("item_tracker", "Item Tracker", "Shows items you just picked up", 1f, 0.85f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		stacks.clear();
		if (mc.player == null) return List.of();
		Map<Item, Integer> now = new HashMap<>();
		Inventory inv = mc.player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty()) now.merge(s.getItem(), s.getCount(), Integer::sum);
		}
		long time = System.currentTimeMillis();
		// While a screen is open the player moves items around by hand; that is not a pickup.
		if (mc.player == lastPlayer && mc.screen == null) {
			now.forEach((item, n) -> {
				int gained = n - before.getOrDefault(item, 0);
				if (gained <= 0) return;
				long[] p = popups.computeIfAbsent(item, k -> new long[2]);
				p[0] += gained;
				p[1] = time;
			});
		}
		lastPlayer = mc.player;
		before.clear();
		before.putAll(now);

		popups.values().removeIf(p -> time - p[1] > duration.get() * 1000);
		List<String> out = new ArrayList<>();
		popups.forEach((item, p) -> {
			ItemStack s = new ItemStack(item);
			stacks.add(s);
			out.add("+" + p[0] + " " + s.getHoverName().getString());
		});
		return out;
	}

	@Override
	protected List<ItemStack> icons(Minecraft mc) {
		return stacks;
	}
}

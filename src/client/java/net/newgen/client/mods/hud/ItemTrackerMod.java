package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.HashMap;
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
	private final Setting.Num maxEntries = num("max_entries", "Max entries", 5, 1, 20, 1);
	private final Setting.Bool newestFirst = bool("newest_first", "Newest first", false);
	private final Setting.Bool combine = bool("combine", "Combine same item", true);

	private final Map<Item, Integer> before = new HashMap<>();

	/** One popup: amount gained and the time of the last gain. */
	private static final class Popup {
		final Item item;
		long amount, time;

		Popup(Item item) {
			this.item = item;
		}
	}

	/** Oldest gain first. */
	private final List<Popup> popups = new ArrayList<>();
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
				if (gained > 0) gain(item, gained, time);
			});
		}
		lastPlayer = mc.player;
		before.clear();
		before.putAll(now);

		popups.removeIf(p -> time - p.time > duration.get() * 1000);
		int max = (int) maxEntries.get();
		while (popups.size() > max) popups.remove(0);
		List<String> out = new ArrayList<>();
		for (int i = 0; i < popups.size(); i++) {
			Popup p = popups.get(newestFirst.value ? popups.size() - 1 - i : i);
			ItemStack s = new ItemStack(p.item);
			stacks.add(s);
			out.add("+" + p.amount + " " + s.getHoverName().getString());
		}
		return out;
	}

	private void gain(Item item, int amount, long time) {
		Popup p = null;
		if (combine.value) {
			for (Popup q : popups) if (q.item == item) p = q;
			if (p != null) popups.remove(p); // moves to the newest place
		}
		if (p == null) p = new Popup(item);
		p.amount += amount;
		p.time = time;
		popups.add(p);
	}

	@Override
	protected List<ItemStack> icons(Minecraft mc) {
		return stacks;
	}
}

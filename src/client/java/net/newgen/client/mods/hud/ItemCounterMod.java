package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class ItemCounterMod extends HudMod {
	private final Setting.Text items = text("items", "Items (ids, comma separated)", "arrow,ender_pearl,golden_apple");
	private final Setting.Bool held = bool("held", "Count held item", true);
	private final Setting.Bool hideZero = bool("hide_zero", "Hide items you have none of", true);

	private final List<ItemStack> stacks = new ArrayList<>();

	public ItemCounterMod() {
		super("item_counter", "Item Counter", "How many of chosen items you carry", 1f, 0.7f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		stacks.clear();
		if (mc.player == null) return List.of();
		Set<Item> wanted = new LinkedHashSet<>();
		if (held.value && !mc.player.getMainHandItem().isEmpty()) wanted.add(mc.player.getMainHandItem().getItem());
		for (String id : items.value.split(",")) {
			Identifier key = Identifier.tryParse(id.trim());
			if (key != null) BuiltInRegistries.ITEM.getOptional(key).ifPresent(wanted::add);
		}
		List<String> out = new ArrayList<>();
		for (Item item : wanted) {
			int n = count(mc.player, s -> s.is(item));
			if (n == 0 && hideZero.value) continue;
			stacks.add(new ItemStack(item));
			out.add("x" + n);
		}
		return out;
	}

	@Override
	protected List<ItemStack> icons(Minecraft mc) {
		return stacks;
	}

	/** Total count of the matching items in the whole inventory (armour and offhand included). */
	static int count(Player player, Predicate<ItemStack> test) {
		Inventory inv = player.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty() && test.test(s)) n += s.getCount();
		}
		return n;
	}
}

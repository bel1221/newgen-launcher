package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

/** The lore (or the whole tooltip) of the item in your hand. */
public class ItemLoreMod extends HudMod {
	private final Setting.Choice source = radio("source", "Show", "Lore only", "Lore only", "Full tooltip");
	private final Setting.Bool name = bool("item_name", "Item name first", false);
	private final Setting.Num maxLines = num("max_lines", "Max lines", 6, 1, 20, 1);
	private final Setting.Bool skipEmpty = bool("skip_empty", "Skip empty lines", true);

	private ItemStack cachedStack = ItemStack.EMPTY;
	private long cachedAt;
	private List<String> cached = List.of();

	public ItemLoreMod() {
		super("item_lore", "Item Lore", "The lore or tooltip lines of the item in your hand", 1f, 0.5f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null || mc.level == null) return List.of();
		ItemStack stack = mc.player.getMainHandItem();
		if (stack.isEmpty()) return List.of();
		long now = System.currentTimeMillis();
		if (stack == cachedStack && now - cachedAt < 250) return cached;
		cachedStack = stack;
		cachedAt = now;
		List<Component> raw;
		if (source.value.equals("Full tooltip")) {
			raw = stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);
			if (!raw.isEmpty()) raw = raw.subList(1, raw.size()); // the first line is the item name
		} else {
			ItemLore lore = stack.get(DataComponents.LORE);
			raw = lore == null ? List.of() : lore.lines();
		}
		List<String> out = new ArrayList<>();
		for (Component line : raw) {
			if (out.size() >= maxLines.get()) break;
			String s = line.getString();
			if (skipEmpty.value && s.isBlank()) continue;
			out.add(s);
		}
		// No lore: nothing to show, not even the name.
		if (!out.isEmpty() && name.value) out.add(0, stack.getHoverName().getString());
		cached = out;
		return out;
	}
}

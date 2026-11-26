package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class ArmorStatusMod extends HudMod {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
			EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
	private static final String[] SLOT_NAMES = {"Helmet", "Chestplate", "Leggings", "Boots", "Main hand", "Off hand"};

	private final Setting.Choice display = choice("display", "Durability as", "Remaining", "Remaining", "Percent");
	private final Setting.Bool hands = bool("hands", "Show held items", true);
	private final Setting.Choice layout = radio("layout", "Layout", "Vertical", "Vertical", "Horizontal");
	private final Setting.Multi slots = multi("slots", "Slots", SLOT_NAMES, SLOT_NAMES);
	private final Setting.Bool colorDurability = bool("color_durability", "Colour by durability", true);
	private final Setting.Color fullColor = color("full_color", "Full durability colour", 0xFF55FF55);
	private final Setting.Color emptyColor = color("empty_color", "Low durability colour", 0xFFFF5555);
	private final Setting.Bool arrows = bool("arrows", "Arrow count on bows", true);
	private final Setting.Bool repairCost = bool("repair_cost", "Anvil uses", false);

	private record Entry(ItemStack stack, String text, int color) {
	}

	private final List<Entry> entries = new ArrayList<>();

	public ArmorStatusMod() {
		super("armor_status", "Armor Status", "Armour and held items with their durability", 1f, 0.5f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		return List.of();
	}

	private void collect(Minecraft mc) {
		entries.clear();
		if (mc.player == null) return;
		int ink = textColor.current();
		for (int i = 0; i < SLOTS.length; i++) {
			EquipmentSlot slot = SLOTS[i];
			boolean hand = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
			if (hand && !hands.value || !slots.has(SLOT_NAMES[i])) continue;
			ItemStack s = mc.player.getItemBySlot(slot);
			if (s.isEmpty()) continue;
			String text;
			int color = ink;
			if (s.isDamageableItem()) {
				int left = s.getMaxDamage() - s.getDamageValue();
				text = display.value.equals("Percent") ? left * 100 / s.getMaxDamage() + "%" : String.valueOf(left);
				if (colorDurability.value)
					color = HudValues.blend(emptyColor.current(), fullColor.current(), (double) left / s.getMaxDamage());
			} else {
				text = s.getCount() > 1 ? "x" + s.getCount() : "";
			}
			if (arrows.value && (s.getItem() instanceof BowItem || s.getItem() instanceof CrossbowItem)) {
				text += " (" + ItemCounterMod.count(mc.player, st -> st.getItem() instanceof ArrowItem) + ")";
			}
			if (repairCost.value) {
				// Each anvil use doubles the cost: 0, 1, 3, 7... -> uses = log2(cost + 1).
				int cost = s.getOrDefault(DataComponents.REPAIR_COST, 0);
				if (cost > 0) text += " [" + (31 - Integer.numberOfLeadingZeros(cost + 1)) + "]";
			}
			entries.add(new Entry(s, text.trim(), color));
		}
	}

	@Override
	protected int[] size(Minecraft mc) {
		collect(mc);
		if (entries.isEmpty()) return null;
		if (layout.value.equals("Horizontal")) {
			int w = 0;
			for (Entry e : entries) w += cell(mc, e) + 4;
			return new int[] {w - 4, 16};
		}
		int w = 0;
		for (Entry e : entries) w = Math.max(w, cell(mc, e));
		return new int[] {w, entries.size() * 18 - 2};
	}

	private static int cell(Minecraft mc, Entry e) {
		return 16 + (e.text().isEmpty() ? 0 : 2 + mc.font.width(e.text()));
	}

	@Override
	protected void draw(GuiGraphics g, Minecraft mc) {
		boolean horizontal = layout.value.equals("Horizontal");
		int x = 0, y = 0;
		for (Entry e : entries) {
			g.renderItem(e.stack(), x, y);
			if (!e.text().isEmpty()) g.drawString(mc.font, e.text(), x + 18, y + 4, e.color(), textShadow.value);
			if (horizontal) x += cell(mc, e) + 4;
			else y += 18;
		}
	}
}

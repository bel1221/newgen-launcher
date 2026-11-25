package net.newgen.client.mods.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class ArmorStatusMod extends HudMod {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
			EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

	private final Setting.Choice display = choice("display", "Durability as", "Remaining", "Remaining", "Percent");
	private final Setting.Bool hands = bool("hands", "Show held items", true);

	private final List<ItemStack> stacks = new ArrayList<>();

	public ArmorStatusMod() {
		super("armor_status", "Armor Status", "Armour and held items with their durability", 1f, 0.5f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		stacks.clear();
		if (mc.player == null) return List.of();
		List<String> out = new ArrayList<>();
		for (EquipmentSlot slot : SLOTS) {
			boolean hand = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
			if (hand && !hands.value) continue;
			ItemStack s = mc.player.getItemBySlot(slot);
			if (s.isEmpty()) continue;
			stacks.add(s);
			if (s.isDamageableItem()) {
				int left = s.getMaxDamage() - s.getDamageValue();
				out.add(display.value.equals("Percent") ? left * 100 / s.getMaxDamage() + "%" : String.valueOf(left));
			} else {
				out.add(s.getCount() > 1 ? "x" + s.getCount() : "");
			}
		}
		return out;
	}

	@Override
	protected List<ItemStack> icons(Minecraft mc) {
		return stacks;
	}
}

package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class PotionCounterMod extends HudMod {
	private final Setting.Choice type = choice("type", "Potion type", "Splash", "Splash", "Drinkable", "All");
	private final Setting.Bool healingOnly = bool("healing_only", "Healing potions only", true);
	private final Setting.Bool hideZero = bool("hide_zero", "Hide when you have none", false);

	private final List<ItemStack> icon = List.of(new ItemStack(Items.SPLASH_POTION));

	public PotionCounterMod() {
		super("potion_counter", "Potion Counter", "Potions you carry", 0.6f, 0.9f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null) return List.of();
		int n = ItemCounterMod.count(mc.player, this::counts);
		return n == 0 && hideZero.value ? List.of() : List.of("x" + n);
	}

	private boolean counts(ItemStack s) {
		boolean splash = s.is(Items.SPLASH_POTION) || s.is(Items.LINGERING_POTION), drink = s.is(Items.POTION);
		if (!(type.value.equals("Splash") ? splash : type.value.equals("Drinkable") ? drink : splash || drink)) return false;
		if (!healingOnly.value) return true;
		PotionContents c = s.get(DataComponents.POTION_CONTENTS);
		return c != null && (c.is(Potions.HEALING) || c.is(Potions.STRONG_HEALING));
	}

	@Override
	protected List<ItemStack> icons(Minecraft mc) {
		return icon;
	}
}

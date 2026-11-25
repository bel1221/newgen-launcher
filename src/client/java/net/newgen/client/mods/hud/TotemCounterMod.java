package net.newgen.client.mods.hud;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class TotemCounterMod extends HudMod {
	private final Setting.Bool hideZero = bool("hide_zero", "Hide when you have none", false);

	private final List<ItemStack> icon = List.of(new ItemStack(Items.TOTEM_OF_UNDYING));

	public TotemCounterMod() {
		super("totem_counter", "Totem Counter", "Totems of Undying you carry", 0.4f, 0.9f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		if (mc.player == null) return List.of();
		int n = ItemCounterMod.count(mc.player, s -> s.is(Items.TOTEM_OF_UNDYING));
		return n == 0 && hideZero.value ? List.of() : List.of("x" + n);
	}

	@Override
	protected List<ItemStack> icons(Minecraft mc) {
		return icon;
	}
}

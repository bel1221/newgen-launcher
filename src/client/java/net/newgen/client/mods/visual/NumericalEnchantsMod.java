package net.newgen.client.mods.visual;

import net.newgen.client.core.Mod;

/** Enchantment levels as numbers: Sharpness 5 instead of Sharpness V. */
public class NumericalEnchantsMod extends Mod {
	public static final NumericalEnchantsMod I = new NumericalEnchantsMod();

	private NumericalEnchantsMod() {
		super("numerical_enchants", "Numerical Enchantments", "Shows enchantment levels as numbers (5) instead of Roman numerals (V)");
	}
}

package net.newgen.client.mixin.visual2;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import net.newgen.client.mods.visual.NumericalEnchantsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Numerical Enchantments: the level ("enchantment.level.N", a Roman numeral) becomes the plain number. */
@Mixin(Enchantment.class)
public class EnchantmentNameMixin {
	@ModifyExpressionValue(method = "getFullname", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
	private static MutableComponent newgen$number(MutableComponent original, Holder<Enchantment> enchantment, int level) {
		return NumericalEnchantsMod.I.enabled ? Component.literal(String.valueOf(level)) : original;
	}
}

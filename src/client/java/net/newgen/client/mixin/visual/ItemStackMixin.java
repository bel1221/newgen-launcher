package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.newgen.client.mods.visual.ShinyPotsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shiny Pots: potions (drinkable, splash and lingering) count as glinting. Only drawing code asks this. */
@Mixin(ItemStack.class)
public class ItemStackMixin {
	@ModifyReturnValue(method = "hasFoil", at = @At("RETURN"))
	private boolean newgen$shinyPots(boolean original) {
		return original || ShinyPotsMod.I.enabled && ((ItemStack) (Object) this).getItem() instanceof PotionItem;
	}
}

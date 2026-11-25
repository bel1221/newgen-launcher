package net.newgen.client.mixin.utility;

import java.util.Optional;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.newgen.client.mods.utility.ShulkerPreviewMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
	/** Only fills in a tooltip image where vanilla has none (bundles keep their own). */
	@Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
	private void newgen$shulkerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
		if (cir.getReturnValue().isPresent()) return;
		Optional<TooltipComponent> preview = ShulkerPreviewMod.preview((ItemStack) (Object) this);
		if (preview.isPresent()) cir.setReturnValue(preview);
	}
}

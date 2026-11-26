package net.newgen.client.mixin.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.newgen.client.mods.hud.BlockPlaceCountMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block Place Count: counts each block you place, on the client's own (predicted) placement. */
@Mixin(BlockItem.class)
public class BlockItemPlaceMixin {
	@Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;", at = @At("RETURN"))
	private void newgen$placed(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (context.getLevel().isClientSide() && cir.getReturnValue().consumesAction()
				&& context.getPlayer() != null && context.getPlayer() == Minecraft.getInstance().player)
			BlockPlaceCountMod.placed();
	}
}

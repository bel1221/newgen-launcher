package net.newgen.client.mixin.utility;

import net.minecraft.client.player.LocalPlayer;
import net.newgen.client.mods.utility.InventoryGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Wave 3 utility 1
/** Q in the world: Drop Protection and locked hotbar slots. */
@Mixin(LocalPlayer.class)
public class LocalPlayerDropMixin {
	@Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
	private void newgen$guardDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
		if (InventoryGuard.onWorldDrop((LocalPlayer) (Object) this)) cir.setReturnValue(false);
	}
}

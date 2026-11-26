package net.newgen.client.mixin.utility;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.newgen.client.mods.utility.InventoryGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Wave 3 utility 1
/** Every container click passes here before it is predicted and sent: Slot Locking, Drop Protection, Slot Binding. */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Inject(method = "handleInventoryMouseClick(IIILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
			at = @At("HEAD"), cancellable = true)
	private void newgen$guardClick(int containerId, int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
		if (InventoryGuard.onClick((MultiPlayerGameMode) (Object) this, containerId, slotId, button, type, player)) ci.cancel();
	}
}

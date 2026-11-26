package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.newgen.client.mods.visual.NametagMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nametag: the player model in the inventory shows your own name tag. */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
	@ModifyReturnValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
	private static EntityRenderState newgen$ownNametag(EntityRenderState state, LivingEntity entity) {
		if (NametagMod.I.enabled && NametagMod.I.inventory.value && entity instanceof LocalPlayer player) {
			state.nameTag = player.getDisplayName();
			state.nameTagAttachment = player.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, player.getYRot());
		}
		return state;
	}
}

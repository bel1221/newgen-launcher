package net.newgen.client.mixin.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.newgen.client.emotes.EmotesMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Emotes: only the main-hand item is drawn while the local player plays an emote (the main hand as the player set it,
 * so the left one for left-handers); the off-hand item is back the frame the emote ends.
 */
@Mixin(ItemInHandLayer.class)
public class EmoteItemInHandLayerMixin {
	@Inject(method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
			at = @At("HEAD"), cancellable = true)
	private void newgen$onlyMainHandWhileEmoting(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack stack, HumanoidArm arm,
			PoseStack pose, SubmitNodeCollector nodes, int light, CallbackInfo ci) {
		if (arm != state.mainArm && EmotesMod.hidesGear(state)) ci.cancel();
	}
}

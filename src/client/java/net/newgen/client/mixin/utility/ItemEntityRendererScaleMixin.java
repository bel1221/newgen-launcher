package net.newgen.client.mixin.utility;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.newgen.client.mods.utility.UhcOverlayMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** UHC Overlay: listed dropped items drawn larger, still resting on the ground. */
@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererScaleMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
			at = @At("TAIL"))
	private void newgen$extractScale(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci) {
		((UhcOverlayMod.Scaled) state).newgen$setUhcScale(UhcOverlayMod.scale(entity.getItem()));
	}

	// Right after the spin: grow around the model's bottom so the item does not sink into the ground.
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V", shift = At.Shift.AFTER))
	private void newgen$scale(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		float s = ((UhcOverlayMod.Scaled) state).newgen$uhcScale();
		if (s == 1f) return;
		float minY = (float) state.item.getModelBoundingBox().minY;
		pose.translate(0f, -minY * (s - 1f), 0f);
		pose.scale(s, s, s);
	}
}

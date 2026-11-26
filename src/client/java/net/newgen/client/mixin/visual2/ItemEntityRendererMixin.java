package net.newgen.client.mixin.visual2;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.newgen.client.mods.visual.DroppedItemsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dropped Items: still (no spin, no bob), flat on the ground, sized, one model per stack. */
@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("RETURN"))
	private void newgen$state(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci) {
		DroppedItemsMod mod = DroppedItemsMod.I;
		if (!mod.enabled) return;
		// Spin and bob both come from the age and the bob offset: zero them and the item rests.
		if (mod.still.value || mod.physics.value) {
			state.ageInTicks = 0;
			state.bobOffset = 0;
		}
		if (mod.single.value) state.count = Math.min(state.count, 1);
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"))
	private void newgen$push(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector nodes, CameraRenderState camera, CallbackInfo ci) {
		pose.pushPose();
		DroppedItemsMod mod = DroppedItemsMod.I;
		if (!mod.enabled) return;
		float s = (float) mod.size.get();
		if (mod.physics.value) {
			// On its side, flat on the ground (vanilla lifts it 0.1 by the resting bob, which now points sideways).
			pose.translate(0, 0.02 * s, 0);
			pose.mulPose(Axis.XP.rotationDegrees(90));
			pose.translate(0, -0.1 * s, 0);
		}
		if (s != 1f) pose.scale(s, s, s);
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("RETURN"))
	private void newgen$pop(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector nodes, CameraRenderState camera, CallbackInfo ci) {
		pose.popPose();
	}
}

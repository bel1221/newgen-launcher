package net.newgen.client.mixin.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.newgen.client.emotes.EmotesMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotes: the local player's armor (helmet, chestplate, leggings, boots and their trims) is not drawn while an emote plays; it is back the frame the emote ends. */
@Mixin(HumanoidArmorLayer.class)
public class EmoteHumanoidArmorLayerMixin {
	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
			at = @At("HEAD"), cancellable = true)
	private void newgen$hideWhileEmoting(PoseStack pose, SubmitNodeCollector nodes, int light, HumanoidRenderState state, float yRot, float xRot, CallbackInfo ci) {
		if (EmotesMod.hidesGear(state)) ci.cancel();
	}
}

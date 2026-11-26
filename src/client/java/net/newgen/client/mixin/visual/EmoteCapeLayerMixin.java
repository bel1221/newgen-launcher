package net.newgen.client.mixin.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.newgen.client.emotes.EmotesMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotes: the local player's cape is not drawn while an emote plays; it is back the frame the emote ends. */
@Mixin(CapeLayer.class)
public class EmoteCapeLayerMixin {
	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
			at = @At("HEAD"), cancellable = true)
	private void newgen$hideWhileEmoting(PoseStack pose, SubmitNodeCollector nodes, int light, AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
		if (EmotesMod.hidesGear(state)) ci.cancel();
	}
}

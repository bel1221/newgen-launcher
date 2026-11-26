package net.newgen.client.mixin.visual2;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.newgen.client.mods.visual.CleanViewMod;
import net.newgen.client.mods.visual.NoBobbingMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** No Bobbing (camera and hand apart) and Clean View's nausea: the screen wobble reads a blend factor of 0. */
@Mixin(GameRenderer.class)
public class GameRendererViewMixin {
	@WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
	private boolean newgen$cameraBob(GameRenderer self, PoseStack pose, float partialTick) {
		return !(NoBobbingMod.I.enabled && NoBobbingMod.I.camera.value);
	}

	@WrapWithCondition(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
	private boolean newgen$handBob(GameRenderer self, PoseStack pose, float partialTick) {
		return !(NoBobbingMod.I.enabled && NoBobbingMod.I.hand.value);
	}

	@ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"))
	private float newgen$nausea(float original) {
		return CleanViewMod.I.enabled && CleanViewMod.I.nausea.value ? 0 : original;
	}

	@ModifyExpressionValue(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;portalEffectIntensity:F"))
	private float newgen$portal(float original) {
		return CleanViewMod.I.enabled && CleanViewMod.I.nausea.value ? 0 : original;
	}
}

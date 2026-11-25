package net.newgen.client.mixin.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.newgen.client.mods.visual.OverlayMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Overlays: hides or lowers the fire shown when you burn. */
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
	@Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
	private static void newgen$fire(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
		if (OverlayMod.I.enabled && OverlayMod.I.hideFire.value) {
			ci.cancel();
			return;
		}
		pose.pushPose();
		if (OverlayMod.I.enabled) pose.translate(0, -(float) OverlayMod.I.lowerFire.get(), 0);
	}

	// Not reached when the HEAD hook cancelled, so this always matches the push above.
	@Inject(method = "renderFire", at = @At("RETURN"))
	private static void newgen$fireDone(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
		pose.popPose();
	}
}

package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.newgen.client.mods.visual.FogMod;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fog: pushes the fog distances far away, right after vanilla has worked them out. */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Inject(method = "setupFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getDevice()Lcom/mojang/blaze3d/systems/GpuDevice;"))
	private void newgen$removeFog(CallbackInfoReturnable<Vector4f> cir, @Local FogData fog) {
		if (!FogMod.I.enabled) return;
		if (FogMod.I.distance.value) {
			fog.renderDistanceStart = 1.0E7f;
			fog.renderDistanceEnd = 2.0E7f;
		}
		if (FogMod.I.environment.value) {
			fog.environmentalStart = 1.0E7f;
			fog.environmentalEnd = 2.0E7f;
		}
	}
}

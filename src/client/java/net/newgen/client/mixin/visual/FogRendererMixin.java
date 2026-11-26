package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.newgen.client.mods.visual.FogMod;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fog: changes the fog distances and colour right after vanilla has worked them out, before they are sent to the GPU. */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Inject(method = "setupFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getDevice()Lcom/mojang/blaze3d/systems/GpuDevice;"))
	private void newgen$removeFog(CallbackInfoReturnable<Vector4f> cir, @Local FogData fog, @Local Vector4f color,
			@Local(argsOnly = true) Camera camera, @Local(argsOnly = true) ClientLevel level) {
		if (!FogMod.I.enabled) return;
		FogMod.I.apply(fog, color, camera, level);
	}
}

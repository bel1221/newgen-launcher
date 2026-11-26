package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightTexture;
import net.newgen.client.mods.visual.LightingMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/** Lighting: feeds the light map a brightness above the 100% the vanilla slider allows. */
@Mixin(LightTexture.class)
public class LightTextureMixin {
	// The slice starts at the brightness ("gamma") option, so this is the float read from that option and no other.
	@ModifyExpressionValue(method = "updateLightTexture",
			slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;")),
			at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0, remap = false))
	private float newgen$brightness(float original) {
		return LightingMod.I.enabled ? (float) LightingMod.I.brightness.get() : original;
	}
}

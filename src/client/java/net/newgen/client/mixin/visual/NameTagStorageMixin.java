package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.newgen.client.mods.visual.NametagMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nametag: opacity of the dark box behind name tags. */
@Mixin(NameTagFeatureRenderer.Storage.class)
public class NameTagStorageMixin {
	@ModifyExpressionValue(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getBackgroundOpacity(F)F"))
	private float newgen$background(float original) {
		return NametagMod.I.enabled ? (float) NametagMod.I.background.get() : original;
	}
}

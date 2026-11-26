package net.newgen.client.mixin.visual2;

import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.layers.WitherArmorLayer;
import net.newgen.client.mods.visual.CleanViewMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clean View: the wither's armour glow is not drawn (the charged creeper's, the same layer, still is). */
@Mixin(EnergySwirlLayer.class)
public class EnergySwirlLayerMixin {
	@Inject(method = "submit", at = @At("HEAD"), cancellable = true)
	private void newgen$witherAura(CallbackInfo ci) {
		if ((Object) this instanceof WitherArmorLayer && CleanViewMod.I.enabled && CleanViewMod.I.witherAura.value) ci.cancel();
	}
}

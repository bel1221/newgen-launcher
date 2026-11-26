package net.newgen.client.mixin.visual2;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.newgen.client.mods.visual.NaturalCapesMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Natural Capes: the cape's swing (capeLean) stays between hanging straight and the chosen highest angle, so it never
 * swings into the back or over the head, and its side sway (capeLean2) is scaled down.
 */
@Mixin(AvatarRenderer.class)
public class AvatarCapeMixin {
	@Inject(method = "extractCapeState", at = @At("RETURN"))
	private void newgen$naturalCape(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		NaturalCapesMod mod = NaturalCapesMod.I;
		if (!mod.enabled) return;
		state.capeLean = Math.max(0f, Math.min((float) mod.maxSwing.get(), state.capeLean));
		state.capeLean2 *= (float) (mod.sideways.get() / 100);
	}
}

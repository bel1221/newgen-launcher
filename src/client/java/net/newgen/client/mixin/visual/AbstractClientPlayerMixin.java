package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.AbstractClientPlayer;
import net.newgen.client.mods.visual.FovMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** FOV: switches off the sprint, speed and bow FOV changes. The spyglass keeps working. */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
	@ModifyReturnValue(method = "getFieldOfViewModifier", at = @At("RETURN"))
	private float newgen$staticFov(float original) {
		boolean off = FovMod.I.enabled && !FovMod.I.dynamic.value && !((AbstractClientPlayer) (Object) this).isScoping();
		return off ? 1f : original;
	}
}

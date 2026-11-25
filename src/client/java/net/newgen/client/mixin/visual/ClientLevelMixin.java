package net.newgen.client.mixin.visual;

import net.minecraft.client.multiplayer.ClientLevel;
import net.newgen.client.mods.visual.ParticleChangerMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Particle Changer: hides the particles of blocks being mined and broken. */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {
	@Inject(method = {"addDestroyBlockEffect", "addBreakingBlockEffect"}, at = @At("HEAD"), cancellable = true)
	private void newgen$hideBlockParticles(CallbackInfo ci) {
		if (ParticleChangerMod.I.enabled && ParticleChangerMod.I.hideBlockBreak.value) ci.cancel();
	}
}

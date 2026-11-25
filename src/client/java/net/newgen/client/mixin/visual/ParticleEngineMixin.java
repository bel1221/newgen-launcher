package net.newgen.client.mixin.visual;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.newgen.client.mods.visual.ParticleChangerMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Particle Changer: repeats the crit and sharpness particle bursts. */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
	@Unique
	private boolean newgen$repeating;

	@Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V", at = @At("HEAD"))
	private void newgen$moreHitParticles(Entity entity, ParticleOptions particle, CallbackInfo ci) {
		if (newgen$repeating || !ParticleChangerMod.I.enabled) return;
		if (particle != ParticleTypes.CRIT && particle != ParticleTypes.ENCHANTED_HIT) return;
		// The flag stops the extra calls below from repeating themselves.
		newgen$repeating = true;
		try {
			for (int i = 1; i < (int) ParticleChangerMod.I.multiplier.get(); i++) {
				((ParticleEngine) (Object) this).createTrackingEmitter(entity, particle);
			}
		} finally {
			newgen$repeating = false;
		}
	}
}

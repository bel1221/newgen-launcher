package net.newgen.client.mixin.visual2;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.newgen.client.mods.visual.CleanViewMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Clean View: the swirls of your own potion effects are not spawned (other players' still are). */
@Mixin(LivingEntity.class)
public class LivingEntityEffectsMixin {
	@WrapWithCondition(method = "tickEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private boolean newgen$ownParticles(Level level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) {
		return !(CleanViewMod.I.enabled && CleanViewMod.I.ownParticles.value && level.isClientSide()
				&& (Object) this == Minecraft.getInstance().player);
	}
}

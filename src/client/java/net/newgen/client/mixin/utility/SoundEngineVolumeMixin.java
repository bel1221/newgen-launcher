package net.newgen.client.mixin.utility;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.newgen.client.mods.utility.SoundVolumesMod;
import net.newgen.client.mods.utility.UnfocusedLimitsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Sound Volumes (per sound) and Unfocused Limits (all sounds): scale the final volume of a sound, after vanilla's clamp. */
@Mixin(SoundEngine.class)
public class SoundEngineVolumeMixin {
	@WrapOperation(method = {
			"play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
			"calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
	private float newgen$volume(SoundEngine engine, float volume, SoundSource source, Operation<Float> original,
			@Local(argsOnly = true) SoundInstance sound) {
		float v = original.call(engine, volume, source);
		return v * SoundVolumesMod.factor(sound.getIdentifier()) * UnfocusedLimitsMod.volumeFactor();
	}
}

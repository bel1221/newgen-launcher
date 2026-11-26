package net.newgen.client.mixin.utility;

import net.minecraft.world.entity.LivingEntity;
import net.newgen.client.mods.utility.KillSoundsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Kill Sounds: entity event 3 is the server telling the client this entity died. */
@Mixin(LivingEntity.class)
public class LivingEntityKillSoundMixin {
	@Inject(method = "handleEntityEvent(B)V", at = @At("HEAD"))
	private void newgen$death(byte event, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (event == 3 && self.level().isClientSide()) KillSoundsMod.onDeath(self);
	}
}

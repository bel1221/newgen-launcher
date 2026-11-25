package net.newgen.client.mixin.controls;

import net.minecraft.client.Camera;
import net.newgen.client.mods.controls.FreelookMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freelook: points the camera at the freelook angles before it is moved back from the player. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	// Ordinals 0 and 1 are the two "follow the entity view" calls (minecart branch and normal branch).
	@Inject(method = "setup", at = {
			@At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0, shift = At.Shift.AFTER),
			@At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1, shift = At.Shift.AFTER)
	})
	private void newgen$freelook(CallbackInfo ci) {
		if (FreelookMod.active) setRotation(FreelookMod.yaw, FreelookMod.pitch);
	}
}

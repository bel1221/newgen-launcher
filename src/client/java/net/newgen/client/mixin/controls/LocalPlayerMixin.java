package net.newgen.client.mixin.controls;

import net.minecraft.client.player.LocalPlayer;
import net.newgen.client.mods.controls.ToggleSneakMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toggle Sneak/Sprint: "disable double-tap W" keeps the double-tap window closed. */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	@Shadow protected int sprintTriggerTime;

	@Inject(method = "aiStep", at = @At("HEAD"))
	private void newgen$noDoubleTap(CallbackInfo ci) {
		if (ToggleSneakMod.noDoubleTap) sprintTriggerTime = 0;
	}
}

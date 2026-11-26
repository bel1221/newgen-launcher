package net.newgen.client.mixin.hud;

import net.minecraft.client.multiplayer.ClientLevel;
import net.newgen.client.mods.hud.TpsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the TPS HUD when a world time update arrives (the server sends one every 20 ticks). Only reads; changes nothing. */
@Mixin(ClientLevel.class)
public class ClientLevelTimeMixin {
	@Inject(method = "setTimeFromServer(JJZ)V", at = @At("HEAD"))
	private void newgen$timeUpdate(long gameTime, long dayTime, boolean tickDayTime, CallbackInfo ci) {
		TpsMod.onTimePacket((ClientLevel) (Object) this);
	}
}

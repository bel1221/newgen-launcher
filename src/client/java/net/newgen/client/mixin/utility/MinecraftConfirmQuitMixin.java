package net.newgen.client.mixin.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.newgen.client.mods.utility.ConfirmQuitMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Confirm Quit: the quit button / window close (stop) and the pause menu's disconnect button ask first. */
@Mixin(Minecraft.class)
public class MinecraftConfirmQuitMixin {
	@Inject(method = "stop()V", at = @At("HEAD"), cancellable = true)
	private void newgen$confirmQuit(CallbackInfo ci) {
		if (ConfirmQuitMod.interceptQuit((Minecraft) (Object) this)) ci.cancel();
	}

	@Inject(method = "disconnectFromWorld(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
	private void newgen$confirmDisconnect(Component reason, CallbackInfo ci) {
		if (ConfirmQuitMod.interceptDisconnect((Minecraft) (Object) this, reason)) ci.cancel();
	}
}

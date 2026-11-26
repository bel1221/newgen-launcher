package net.newgen.client.mixin.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.newgen.client.mods.visual.AnimatiumGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Animatium's config screens (its keybind, command, Mod Menu entry, YACL) never open: see AnimatiumGuard. */
@Mixin(Minecraft.class)
public class AnimatiumScreenMixin {
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void newgen$noAnimatiumConfig(Screen screen, CallbackInfo ci) {
		if (AnimatiumGuard.blocked(screen)) ci.cancel();
	}
}

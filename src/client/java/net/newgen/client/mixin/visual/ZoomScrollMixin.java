package net.newgen.client.mixin.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.newgen.client.mods.visual.ZoomMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zoom: the mouse wheel changes the zoom while zooming (in game only, never in a screen). */
@Mixin(MouseHandler.class)
public class ZoomScrollMixin {
	@Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
	private void newgen$zoomScroll(long window, double dx, double dy, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (window != mc.getWindow().handle() || mc.screen != null) return;
		if (ZoomMod.I.onScroll(dy)) ci.cancel();
	}
}

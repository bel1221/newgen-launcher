package net.newgen.client.mixin.utility;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.newgen.client.gui.VideoBackground;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every screen that would draw the vanilla panorama (only screens outside a world: Options and its sub-screens,
 * Language, the world and server lists, Realms, Credits...) draws the NewGen video background instead; vanilla
 * then adds its own blur and dark menu background on top as usual. Screens in a world never reach this.
 */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
	@Shadow
	public int width;
	@Shadow
	public int height;

	@Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
	private void newgen$videoInsteadOfPanorama(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
		if (VideoBackground.render(graphics, width, height)) ci.cancel();
	}
}

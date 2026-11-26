package net.newgen.client.mixin.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import net.newgen.client.mods.hud.HudMods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Wave 3 vanilla HUD
/** Lets the XP Bar mod hide, scale or move the experience bar (the level number: GuiMixin). */
@Mixin(ExperienceBarRenderer.class)
public class ExperienceBarRendererMixin {
	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void newgen$xpBarBegin(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		if (HudMods.XP_BAR.begin(g)) ci.cancel();
	}

	@Inject(method = "renderBackground", at = @At("RETURN"))
	private void newgen$xpBarEnd(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		HudMods.XP_BAR.end(g);
	}
}

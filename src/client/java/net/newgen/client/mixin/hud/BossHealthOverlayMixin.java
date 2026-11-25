package net.newgen.client.mixin.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.newgen.client.mods.hud.HudMods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the Bossbar mod hide, scale or move the boss bars. */
@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void newgen$begin(GuiGraphics g, CallbackInfo ci) {
		if (HudMods.BOSSBAR.begin(g)) ci.cancel();
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void newgen$end(GuiGraphics g, CallbackInfo ci) {
		HudMods.BOSSBAR.end(g);
	}
}

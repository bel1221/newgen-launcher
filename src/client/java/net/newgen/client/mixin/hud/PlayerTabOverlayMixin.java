package net.newgen.client.mixin.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.newgen.client.mods.hud.HudMods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the Tab mod hide, scale or move the player list. */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void newgen$begin(GuiGraphics g, int width, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
		if (HudMods.TAB.begin(g)) ci.cancel();
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void newgen$end(GuiGraphics g, int width, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
		HudMods.TAB.end(g);
	}
}

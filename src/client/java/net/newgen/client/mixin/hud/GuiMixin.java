package net.newgen.client.mixin.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.world.scores.Objective;
import net.newgen.client.mods.hud.HudMods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the HUD mods hide, scale or move the action bar, titles, scoreboard, effect icons and locator bar. */
@Mixin(Gui.class)
public class GuiMixin {
	@Inject(method = "renderOverlayMessage", at = @At("HEAD"), cancellable = true)
	private void newgen$actionBarBegin(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		if (HudMods.ACTION_BAR.begin(g)) ci.cancel();
	}

	@Inject(method = "renderOverlayMessage", at = @At("RETURN"))
	private void newgen$actionBarEnd(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		HudMods.ACTION_BAR.end(g);
	}

	@Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true)
	private void newgen$titlesBegin(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		if (HudMods.TITLES.begin(g)) ci.cancel();
	}

	@Inject(method = "renderTitle", at = @At("RETURN"))
	private void newgen$titlesEnd(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		HudMods.TITLES.end(g);
	}

	@Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
	private void newgen$scoreboardBegin(GuiGraphics g, Objective objective, CallbackInfo ci) {
		if (HudMods.SCOREBOARD.begin(g)) ci.cancel();
	}

	@Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
	private void newgen$scoreboardEnd(GuiGraphics g, Objective objective, CallbackInfo ci) {
		HudMods.SCOREBOARD.end(g);
	}

	@Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
	private void newgen$hideVanillaEffects(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		if (HudMods.POTION_EFFECTS.enabled && HudMods.POTION_EFFECTS.hideVanilla.value) ci.cancel();
	}

	// With no waypoints the game falls back to the XP bar by itself.
	@Redirect(method = "nextContextualInfoState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/waypoints/ClientWaypointManager;hasWaypoints()Z"))
	private boolean newgen$hideLocatorBar(ClientWaypointManager waypoints) {
		return !HudMods.LOCATOR_BAR.enabled && waypoints.hasWaypoints();
	}
}

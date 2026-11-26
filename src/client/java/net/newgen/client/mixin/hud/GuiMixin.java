package net.newgen.client.mixin.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.Objective;
import net.newgen.client.mods.hud.HudMods;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

	// Vanilla fades the action bar over its last 20 ticks.
	@ModifyConstant(method = "renderOverlayMessage", constant = @Constant(floatValue = 20.0F))
	private float newgen$actionBarFade(float ticks) {
		return HudMods.ACTION_BAR.fadeTicks(ticks);
	}

	@ModifyArg(method = "renderOverlayMessage", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"),
			index = 2)
	private int newgen$actionBarAlign(Font font, Component text, int x, int y, int width, int color) {
		return HudMods.ACTION_BAR.alignX(x, width, 1f);
	}

	@Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true)
	private void newgen$titlesBegin(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		if (HudMods.TITLES.begin(g)) ci.cancel();
	}

	@Inject(method = "renderTitle", at = @At("RETURN"))
	private void newgen$titlesEnd(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		HudMods.TITLES.end(g);
	}

	@ModifyArg(method = "renderTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"), index = 0)
	private int newgen$titleAlpha(int alpha) {
		return HudMods.TITLES.titleAlpha(alpha);
	}

	@ModifyArg(method = "renderTitle", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"),
			index = 2)
	private int newgen$titleAlign(Font font, Component text, int x, int y, int width, int color) {
		return HudMods.TITLES.alignX(x, width, 4f);
	}

	@ModifyArg(method = "renderTitle", at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"),
			index = 2)
	private int newgen$subtitleAlign(Font font, Component text, int x, int y, int width, int color) {
		return HudMods.TITLES.alignX(x, width, 2f);
	}

	@Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
	private void newgen$scoreboardBegin(GuiGraphics g, Objective objective, CallbackInfo ci) {
		if (HudMods.SCOREBOARD.begin(g)) ci.cancel();
	}

	@Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
	private void newgen$scoreboardEnd(GuiGraphics g, Objective objective, CallbackInfo ci) {
		HudMods.SCOREBOARD.end(g);
	}

	// A blank number format leaves the score column empty and narrows the box.
	@Redirect(method = "displayScoreboardSidebar", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/scores/Objective;numberFormatOrDefault(Lnet/minecraft/network/chat/numbers/NumberFormat;)Lnet/minecraft/network/chat/numbers/NumberFormat;"))
	private NumberFormat newgen$scoreboardNumbers(Objective objective, NumberFormat fallback) {
		if (HudMods.SCOREBOARD.enabled && HudMods.SCOREBOARD.hideNumbers.value) return BlankFormat.INSTANCE;
		return objective.numberFormatOrDefault(fallback);
	}

	// Vanilla fills the title row first (ordinal 0), then the body.
	@Redirect(method = "displayScoreboardSidebar", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void newgen$scoreboardTitleFill(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y2, HudMods.SCOREBOARD.fill(x1, y1, x2, y2, color, true));
	}

	@Redirect(method = "displayScoreboardSidebar", at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void newgen$scoreboardBodyFill(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y2, HudMods.SCOREBOARD.fill(x1, y1, x2, y2, color, false));
	}

	// Tab toggle: the list stays open after one press of the player list key.
	@Redirect(method = "renderTabList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
	private boolean newgen$tabToggle(KeyMapping key) {
		return HudMods.TAB.open(key.isDown());
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

	// Wave 3 vanilla HUD: hotbar, hearts, hunger, armor, air, XP level, held item name, mount health, effect icons.
	// Each part is wrapped at the head/return of its own vanilla method, so the call sites stay free for Fabric's HUD API.
	@Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
	private void newgen$hotbarBegin(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		if (HudMods.HOTBAR.begin(g)) ci.cancel();
	}

	@Inject(method = "renderItemHotbar", at = @At("RETURN"))
	private void newgen$hotbarEnd(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		HudMods.HOTBAR.end(g);
	}

	@Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
	private void newgen$heartsBegin(GuiGraphics g, Player player, int x, int y, int rowHeight, int regenIndex, float maxHealth,
			int health, int displayHealth, int absorption, boolean blink, CallbackInfo ci) {
		if (HudMods.HEALTH_BAR.begin(g)) ci.cancel();
	}

	@Inject(method = "renderHearts", at = @At("RETURN"))
	private void newgen$heartsEnd(GuiGraphics g, Player player, int x, int y, int rowHeight, int regenIndex, float maxHealth,
			int health, int displayHealth, int absorption, boolean blink, CallbackInfo ci) {
		HudMods.HEALTH_BAR.end(g);
	}

	// Smooth health: the health argument (index 8 counting this) eases toward the real value.
	@ModifyVariable(method = "renderHearts", at = @At("HEAD"), argsOnly = true, index = 8)
	private int newgen$smoothHealth(int health) {
		return HudMods.HEALTH_BAR.shownHealth(health);
	}

	@Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
	private void newgen$foodBegin(GuiGraphics g, Player player, int y, int right, CallbackInfo ci) {
		if (HudMods.HUNGER_BAR.begin(g)) ci.cancel();
	}

	@Inject(method = "renderFood", at = @At("RETURN"))
	private void newgen$foodEnd(GuiGraphics g, Player player, int y, int right, CallbackInfo ci) {
		HudMods.HUNGER_BAR.end(g);
	}

	// No hunger shake: vanilla jiggles the drumsticks only when saturation is zero.
	@ModifyExpressionValue(method = "renderFood", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;getSaturationLevel()F"))
	private float newgen$noHungerShake(float saturation) {
		return HudMods.HUNGER_BAR.saturation(saturation);
	}

	@Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
	private static void newgen$armorBegin(GuiGraphics g, Player player, int y, int rows, int rowHeight, int x, CallbackInfo ci) {
		if (HudMods.ARMOR_BAR.begin(g)) ci.cancel();
	}

	@Inject(method = "renderArmor", at = @At("RETURN"))
	private static void newgen$armorEnd(GuiGraphics g, Player player, int y, int rows, int rowHeight, int x, CallbackInfo ci) {
		HudMods.ARMOR_BAR.end(g);
	}

	@Inject(method = "renderAirBubbles", at = @At("HEAD"), cancellable = true)
	private void newgen$airBegin(GuiGraphics g, Player player, int vehicleHearts, int y, int right, CallbackInfo ci) {
		if (HudMods.AIR_BAR.begin(g)) ci.cancel();
	}

	@Inject(method = "renderAirBubbles", at = @At("RETURN"))
	private void newgen$airEnd(GuiGraphics g, Player player, int vehicleHearts, int y, int right, CallbackInfo ci) {
		HudMods.AIR_BAR.end(g);
	}

	// The level number is a static interface method, so it is wrapped at its call site (the bar itself: ExperienceBarRendererMixin).
	@WrapOperation(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"))
	private void newgen$xpLevel(GuiGraphics g, Font font, int level, Operation<Void> original) {
		HudMods.XP_BAR.wrap(g, () -> original.call(g, font, level));
	}

	@Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
	private void newgen$itemNameBegin(GuiGraphics g, CallbackInfo ci) {
		if (HudMods.ITEM_NAME.begin(g)) ci.cancel();
	}

	@Inject(method = "renderSelectedItemName", at = @At("RETURN"))
	private void newgen$itemNameEnd(GuiGraphics g, CallbackInfo ci) {
		HudMods.ITEM_NAME.end(g);
	}

	@Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
	private void newgen$mountBegin(GuiGraphics g, CallbackInfo ci) {
		if (HudMods.MOUNT_HEALTH.begin(g)) ci.cancel();
	}

	@Inject(method = "renderVehicleHealth", at = @At("RETURN"))
	private void newgen$mountEnd(GuiGraphics g, CallbackInfo ci) {
		HudMods.MOUNT_HEALTH.end(g);
	}

	@Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
	private void newgen$effectsBegin(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		// Potion Effects hiding the icons cancels too; push nothing then, whichever runs first.
		if (HudMods.POTION_EFFECTS.enabled && HudMods.POTION_EFFECTS.hideVanilla.value) return;
		if (HudMods.EFFECT_ICONS.begin(g)) ci.cancel();
	}

	@Inject(method = "renderEffects", at = @At("RETURN"))
	private void newgen$effectsEnd(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
		HudMods.EFFECT_ICONS.end(g);
	}
}

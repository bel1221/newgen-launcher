package net.newgen.client.mixin.hud;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.newgen.client.mods.hud.HudMods;
import net.newgen.client.mods.hud.TabLogic;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the Tab mod hide, scale or move the player list, and its list options. */
@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
	@Shadow private @Nullable Component header;
	@Shadow private @Nullable Component footer;
	@Unique private @Nullable Component newgen$header, newgen$footer;

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void newgen$begin(GuiGraphics g, int width, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
		if (HudMods.TAB.begin(g)) {
			ci.cancel();
			return;
		}
		// Hidden for this draw only, so switching the option off brings them back.
		newgen$header = header;
		newgen$footer = footer;
		if (HudMods.TAB.enabled && HudMods.TAB.hideHeader.value) header = null;
		if (HudMods.TAB.enabled && HudMods.TAB.hideFooter.value) footer = null;
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void newgen$end(GuiGraphics g, int width, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
		header = newgen$header;
		footer = newgen$footer;
		HudMods.TAB.end(g);
	}

	// The player limit replaces vanilla's 80; the list is cut in getPlayerInfos' return below.
	@Redirect(method = "getPlayerInfos", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;limit(J)Ljava/util/stream/Stream;"))
	private Stream<PlayerInfo> newgen$noLimit(Stream<PlayerInfo> stream, long max) {
		return HudMods.TAB.enabled ? stream : stream.limit(max);
	}

	@Inject(method = "getPlayerInfos", at = @At("RETURN"), cancellable = true)
	private void newgen$arrange(CallbackInfoReturnable<List<PlayerInfo>> cir) {
		if (HudMods.TAB.enabled) cir.setReturnValue(HudMods.TAB.arrange(cir.getReturnValue()));
	}

	// Heads are drawn when playing on a local or encrypted (online-mode) server.
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"))
	private boolean newgen$headsLocal(Minecraft mc) {
		return HudMods.TAB.showHeads(mc.isLocalServer());
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;isEncrypted()Z"))
	private boolean newgen$headsEncrypted(Connection connection) {
		return HudMods.TAB.showHeads(connection.isEncrypted());
	}

	// Compact rows: 8 pixel row pitch instead of 9 (rows touch, no gap).
	@ModifyConstant(method = "render", constant = @Constant(intValue = 9))
	private int newgen$rowPitch(int pitch) {
		return HudMods.TAB.enabled && HudMods.TAB.compact.value ? 8 : pitch;
	}

	// Room for the ping number at the end of each row (vanilla keeps 13 pixels for the icon).
	@ModifyConstant(method = "render", constant = @Constant(intValue = 13))
	private int newgen$pingRoom(int room) {
		return HudMods.TAB.enabled && HudMods.TAB.pingNumbers.value ? room + 12 : room;
	}

	@Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
	private void newgen$pingNumber(GuiGraphics g, int width, int x, int y, PlayerInfo info, CallbackInfo ci) {
		if (!HudMods.TAB.enabled || !HudMods.TAB.pingNumbers.value) return;
		ci.cancel();
		int ms = info.getLatency();
		String text = ms < 0 ? "?" : String.valueOf(ms);
		var font = Minecraft.getInstance().font;
		g.drawString(font, text, x + width - 1 - font.width(text), y, TabLogic.pingColor(ms), true);
	}
}

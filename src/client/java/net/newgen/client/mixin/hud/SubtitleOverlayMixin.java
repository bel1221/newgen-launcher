package net.newgen.client.mixin.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.network.chat.Component;
import net.newgen.client.mods.hud.StyledSubtitlesMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Styled Subtitles: moves, scales and recolours the vanilla subtitles while the mod is on. */
@Mixin(SubtitleOverlay.class)
public class SubtitleOverlayMixin {
	@Unique
	private boolean newgen$styled;

	@Inject(method = "render", at = @At("HEAD"))
	private void newgen$begin(GuiGraphics g, CallbackInfo ci) {
		newgen$styled = StyledSubtitlesMod.active();
		if (newgen$styled) StyledSubtitlesMod.INSTANCE.begin(g);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void newgen$end(GuiGraphics g, CallbackInfo ci) {
		if (newgen$styled) StyledSubtitlesMod.INSTANCE.end(g);
		newgen$styled = false;
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void newgen$row(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y2, newgen$styled ? StyledSubtitlesMod.INSTANCE.row(x1, x2) : color);
	}

	@ModifyArg(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 4)
	private int newgen$arrowColor(Font font, String text, int x, int y, int color) {
		return newgen$styled ? StyledSubtitlesMod.INSTANCE.text(color) : color;
	}

	@ModifyArg(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 4)
	private int newgen$textColor(Font font, Component text, int x, int y, int color) {
		return newgen$styled ? StyledSubtitlesMod.INSTANCE.text(color) : color;
	}
}

package net.newgen.client.mixin.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.newgen.client.mods.hud.F3DisplayMod;
import net.newgen.client.mods.hud.HudMods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets the F3 Display mod restyle the debug text. */
@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
	@Redirect(method = "renderLines", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void newgen$background(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		F3DisplayMod mod = HudMods.F3_DISPLAY;
		if (!mod.enabled || mod.background.value) g.fill(x1, y1, x2, y2, color);
	}

	@Redirect(method = "renderLines", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"))
	private void newgen$text(GuiGraphics g, Font font, String text, int x, int y, int color, boolean shadow) {
		F3DisplayMod mod = HudMods.F3_DISPLAY;
		if (mod.enabled) g.drawString(font, text, x, y, mod.textColor.value, mod.textShadow.value);
		else g.drawString(font, text, x, y, color, shadow);
	}
}

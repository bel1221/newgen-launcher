package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.newgen.client.mods.visual.CrosshairMod;
import net.newgen.client.mods.visual.OverlayMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Crosshair and the pumpkin / powder snow part of Overlays. */
@Mixin(Gui.class)
public class GuiMixin {
	// The first sprite drawn in renderCrosshair is the crosshair itself; the attack indicator comes after and stays.
	@WrapWithCondition(method = "renderCrosshair", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private boolean newgen$crosshair(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
		if (!CrosshairMod.I.enabled) return true;
		CrosshairMod.I.draw(graphics);
		return false;
	}

	@Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
	private void newgen$hideOverlay(GuiGraphics graphics, Identifier texture, float alpha, CallbackInfo ci) {
		if (!OverlayMod.I.enabled) return;
		String path = texture.getPath();
		if (path.contains("pumpkin") && OverlayMod.I.hidePumpkin.value) ci.cancel();
		if (path.contains("powder_snow") && OverlayMod.I.hidePowderSnow.value) ci.cancel();
	}
}

package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.network.chat.Component;
import net.newgen.client.mods.visual.NametagMod;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Nametag: text shadow and a background with rounded corners (the corner pixels left out). */
@Mixin(NameTagFeatureRenderer.class)
public class NameTagFeatureRendererMixin {
	@WrapOperation(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"))
	private void newgen$draw(Font font, Component text, float x, float y, int color, boolean shadow, Matrix4f pose,
			MultiBufferSource buffers, Font.DisplayMode mode, int background, int light, Operation<Void> original) {
		NametagMod mod = NametagMod.I;
		if (!mod.enabled) {
			original.call(font, text, x, y, color, shadow, pose, buffers, mode, background, light);
			return;
		}
		if (mod.rounded.value && background >>> 24 != 0) {
			float l = x - 1, t = y - 1, r = x + font.width(text), b = y + 9;
			EffectGlyph rect = ((FontAccessor) font).newgen$provider().effect();
			Font.GlyphVisitor out = Font.GlyphVisitor.forMultiBufferSource(buffers, pose, mode, light);
			out.acceptEffect(rect.createEffect(l + 1, t, r - 1, b, -0.01f, background, 0, 0f));
			out.acceptEffect(rect.createEffect(l, t + 1, l + 1, b - 1, -0.01f, background, 0, 0f));
			out.acceptEffect(rect.createEffect(r - 1, t + 1, r, b - 1, -0.01f, background, 0, 0f));
			background = 0;
		}
		boolean withShadow = shadow || mod.shadow.value.equals("Shadow") && mode == Font.DisplayMode.NORMAL;
		original.call(font, text, x, y, color, withShadow, pose, buffers, mode, background, light);
	}
}

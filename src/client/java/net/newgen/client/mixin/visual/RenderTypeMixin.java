package net.newgen.client.mixin.visual;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.newgen.client.mods.visual.ShinyPotsMod;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Shiny Pots: tints the enchantment glint through the colour multiplier of glint draws. */
@Mixin(RenderType.class)
public class RenderTypeMixin {
	@ModifyArg(method = "draw", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"),
			index = 1)
	private Vector4fc newgen$glintColor(Vector4fc color) {
		ShinyPotsMod mod = ShinyPotsMod.I;
		if (!mod.enabled || !mod.customGlint.value || ((RenderType) (Object) this).pipeline() != RenderPipelines.GLINT) return color;
		int c = mod.glintColor.current();
		return new Vector4f((c >> 16 & 0xFF) / 255f, (c >> 8 & 0xFF) / 255f, (c & 0xFF) / 255f, 1f);
	}
}

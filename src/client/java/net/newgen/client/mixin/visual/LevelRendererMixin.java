package net.newgen.client.mixin.visual;

import net.minecraft.client.renderer.LevelRenderer;
import net.newgen.client.mods.visual.BlockOutlineMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Block Outline: renderHitOutline has exactly one int (colour) and one float (line width) argument. */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true)
	private int newgen$outlineColor(int color) {
		return BlockOutlineMod.I.enabled ? BlockOutlineMod.I.color.current() : color;
	}

	@ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), argsOnly = true)
	private float newgen$outlineWidth(float width) {
		return BlockOutlineMod.I.enabled ? width * (float) BlockOutlineMod.I.width.get() : width;
	}
}

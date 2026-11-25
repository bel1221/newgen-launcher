package net.newgen.client.mixin.visual;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.newgen.client.mods.visual.VisualMods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chunk Borders, Height Limit, Hitboxes, Light Overlay and TNT Countdown draw with the vanilla "gizmo" shapes.
 * This is the one place per frame where vanilla collects them, so our shapes join the vanilla debug shapes.
 */
@Mixin(DebugRenderer.class)
public class DebugRendererMixin {
	@Inject(method = "emitGizmos", at = @At("TAIL"))
	private void newgen$emit(Frustum frustum, double x, double y, double z, float partialTick, CallbackInfo ci) {
		VisualMods.emitGizmos(partialTick);
	}
}

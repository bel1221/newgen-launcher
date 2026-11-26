package net.newgen.client.mixin.hud;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The field of view the world is drawn with (FOV effects included): the Elytra Crosshair projects with it. */
@Mixin(GameRenderer.class)
public interface GameRendererFovInvoker {
	@Invoker("getFov")
	float newgen$getFov(Camera camera, float partialTick, boolean useFovSetting);
}

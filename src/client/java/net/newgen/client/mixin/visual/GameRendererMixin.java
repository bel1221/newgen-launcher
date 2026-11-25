package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.newgen.client.mods.visual.FovMod;
import net.newgen.client.mods.visual.HurtCamMod;
import net.newgen.client.mods.visual.MenuBlurMod;
import net.newgen.client.mods.visual.ZoomMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** FOV, Zoom, Hurt Cam and Menu Blur. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	// The only Integer read in getFov is the FOV option.
	@ModifyExpressionValue(method = "getFov", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", remap = false))
	private int newgen$customFov(int original) {
		return FovMod.I.enabled ? (int) FovMod.I.fov.get() : original;
	}

	// useFovSetting is false for the hand, which must not zoom.
	@ModifyReturnValue(method = "getFov", at = @At("RETURN"))
	private float newgen$zoom(float original, Camera camera, float partialTick, boolean useFovSetting) {
		return useFovSetting && ZoomMod.I.enabled ? original / ZoomMod.I.divisor(partialTick) : original;
	}

	// The only Double read in bobHurt is the "damage tilt" option.
	@ModifyExpressionValue(method = "bobHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Double;doubleValue()D", remap = false))
	private double newgen$hurtCam(double original) {
		return HurtCamMod.I.enabled ? original * HurtCamMod.I.strength.get() : original;
	}

	@ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getMenuBackgroundBlurriness()I"))
	private int newgen$menuBlur(int original) {
		return MenuBlurMod.I.enabled ? (int) MenuBlurMod.I.strength.get() : original;
	}
}

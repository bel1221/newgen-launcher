package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.newgen.client.mods.visual.FovMod;
import net.newgen.client.mods.visual.HurtCamMod;
import net.newgen.client.mods.visual.MenuBlurMod;
import net.newgen.client.mods.visual.OverlayMod;
import net.newgen.client.mods.visual.ZoomMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** FOV, Zoom, Hurt Cam, Menu Blur and the nausea / portal distortion of Overlays. */
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

	// The only Double.floatValue in getFov is the FOV effects scale, used only for the underwater FOV change.
	@ModifyExpressionValue(method = "getFov", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", remap = false))
	private float newgen$underwaterFov(float original) {
		if (!FovMod.I.enabled) return original;
		return FovMod.I.lock.value ? 0f : original * (float) FovMod.I.underwater.get();
	}

	// Zoom: "Show hand while zoomed" off hides the hand the way third person does.
	@ModifyExpressionValue(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
	private boolean newgen$zoomHand(boolean original) {
		return original && ZoomMod.I.showHand();
	}

	// The only Math.max(float, float) in renderLevel is the nausea / portal distortion strength.
	@ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", remap = false))
	private float newgen$distortion(float original) {
		return OverlayMod.I.enabled ? original * (float) OverlayMod.I.distortion.get() / 100f : original;
	}
}

package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.newgen.client.mods.visual.NametagMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Nametag: background, height, scale, colours, and hiding the tags while F1 is on. */
@Mixin(NameTagFeatureRenderer.Storage.class)
public class NameTagStorageMixin {

	@ModifyExpressionValue(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getBackgroundOpacity(F)F"))
	private float newgen$background(float original) {
		return NametagMod.I.enabled ? (float) NametagMod.I.background.get() : original;
	}

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void newgen$hideF1(CallbackInfo ci) {
		if (NametagMod.I.hidden()) ci.cancel();
	}

	@ModifyArg(method = "add", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"), index = 1)
	private double newgen$height(double y) {
		return NametagMod.I.enabled ? y + NametagMod.I.height.get() : y;
	}

	@WrapOperation(method = "add", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
	private void newgen$scale(PoseStack pose, float x, float y, float z, Operation<Void> original) {
		float s = NametagMod.I.enabled ? (float) NametagMod.I.scale.get() : 1f;
		original.call(pose, x * s, y * s, z * s);
	}

	// Two (or three) tags are made per name: each gets the same colours. One injector, because the remapper
	// left the second copy of this target string unmapped and the game crashed (Scanned 0 targets).
	@ModifyArgs(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeStorage$NameTagSubmit;<init>(Lorg/joml/Matrix4f;FFLnet/minecraft/network/chat/Component;IIID)V"), require = 1, allow = 3)
	private void newgen$colors(Args args) {
		args.set(5, NametagMod.I.text(args.<Integer>get(5)));
		args.set(6, NametagMod.I.background(args.<Integer>get(6)));
	}
}

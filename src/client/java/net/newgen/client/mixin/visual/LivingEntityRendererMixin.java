package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.newgen.client.mods.visual.MobSizeMod;
import net.newgen.client.mods.visual.NametagMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nametag (own name tag) and Mob Size. Full method descriptors keep the compiler's bridge methods out. */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	// Vanilla hides the name of the entity the camera is in; saying "no camera entity" shows it.
	@ModifyExpressionValue(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;"))
	private Entity newgen$ownNametag(Entity camera) {
		boolean show = NametagMod.I.enabled && NametagMod.I.own.value && !Minecraft.getInstance().options.getCameraType().isFirstPerson();
		return show ? null : camera;
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL"))
	private void newgen$mobSize(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
		state.scale *= MobSizeMod.I.factor(entity);
	}
}

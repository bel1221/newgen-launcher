package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.newgen.client.emotes.EmotesMod;
import net.newgen.client.mods.visual.NametagMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nametag (own name tag) and the emote body yaw. Mob Size was removed: scaling other entities is an unfair advantage. Full method descriptors keep the compiler's bridge methods out. */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	// Vanilla hides the name of the entity the camera is in; saying "no camera entity" shows it.
	@ModifyExpressionValue(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;"))
	private Entity newgen$ownNametag(Entity camera) {
		boolean show = NametagMod.I.enabled && NametagMod.I.own.value && !Minecraft.getInstance().options.getCameraType().isFirstPerson();
		return show ? null : camera;
	}

	/**
	 * Emotes: while the local player stands still the drawn body (and the emote with it) keeps its yaw while the mouse
	 * turns the real one, so the camera orbits the emote; the head follows the emote instead of the mouse. Only the
	 * render state changes: the player's rotation, aim and what is sent to the server are untouched.
	 */
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL"))
	private void newgen$emoteBodyYaw(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
		if (!(entity instanceof LocalPlayer player) || !EmotesMod.emoting(player)) return;
		float body = EmotesMod.renderBodyYaw(player, state.bodyRot);
		if (Float.isNaN(body)) return;
		float headYaw = state.bodyRot + state.yRot;
		state.bodyRot = body;
		if (EmotesMod.bodyLocked()) {
			state.yRot = 0;
			state.xRot = 0;
		} else {
			state.yRot = Mth.wrapDegrees(headYaw - body);
		}
	}
}

package net.newgen.client.mixin.utility;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.newgen.client.mods.utility.PingNametagsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Ping on Nametags: the player's latency added to the name tag text. */
@Mixin(EntityRenderer.class)
public class EntityRendererPingMixin {
	@ModifyReturnValue(method = "getNameTag(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"))
	private Component newgen$ping(Component original, Entity entity) {
		return PingNametagsMod.decorate(original, entity);
	}
}

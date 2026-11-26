package net.newgen.client.mixin.visual2;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.newgen.client.mods.visual.CleanViewMod;
import net.newgen.client.mods.visual.EntityDistanceMod;
import net.newgen.client.mods.visual.MountViewMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Entity Distance, Clean View (lightning) and Mount View: entities the game would draw that we leave out. */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
	@ModifyReturnValue(method = "shouldRender", at = @At("RETURN"))
	private boolean newgen$cull(boolean original, Entity entity, Frustum frustum, double camX, double camY, double camZ) {
		if (!original) return false;
		if (EntityDistanceMod.I.enabled && EntityDistanceMod.I.tooFar(entity, camX, camY, camZ)) return false;
		if (CleanViewMod.I.enabled && CleanViewMod.I.lightning.value && entity instanceof LightningBolt) return false;
		if (MountViewMod.I.enabled && MountViewMod.I.hide.value) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && entity == mc.player.getVehicle() && mc.options.getCameraType().isFirstPerson()) return false;
		}
		return true;
	}
}

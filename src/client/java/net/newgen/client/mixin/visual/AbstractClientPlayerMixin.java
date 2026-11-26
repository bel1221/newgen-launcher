package net.newgen.client.mixin.visual;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.newgen.client.mods.visual.FovMod;
import net.newgen.client.mods.visual.VisualMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * FOV: switches off the sprint, speed and bow FOV changes (the spyglass keeps working), or scales each of them by its
 * own factor; the static lock switches off everything, the spyglass too.
 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
	@ModifyReturnValue(method = "getFieldOfViewModifier(ZF)F", at = @At("RETURN"))
	private float newgen$staticFov(float original, boolean firstPerson, float effectScale) {
		FovMod fov = FovMod.I;
		if (!fov.enabled) return original;
		if (fov.lock.value) return 1f;
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		if (self.isScoping()) return original;
		if (!fov.dynamic.value) return 1f;
		float walk = self.getAbilities().getWalkingSpeed();
		float ratio = walk == 0 ? 1 : (float) self.getAttributeValue(Attributes.MOVEMENT_SPEED) / walk;
		float pull = self.isUsingItem() && self.getUseItem().is(Items.BOW) ? Math.min(self.getTicksUsingItem() / 20f, 1f) : 0;
		float g = VisualMath.fovModifier(self.getAbilities().flying, ratio, self.isSprinting() ? 1.3f : 1f, pull,
				fov.flying.get(), fov.sprint.get(), fov.speed.get(), fov.slowness.get(), fov.bow.get());
		return Mth.lerp(effectScale, 1f, g);
	}
}

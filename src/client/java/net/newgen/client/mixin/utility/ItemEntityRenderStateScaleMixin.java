package net.newgen.client.mixin.utility;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.newgen.client.mods.utility.UhcOverlayMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** UHC Overlay: carries the extra scale of a dropped item from extract to submit. */
@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateScaleMixin implements UhcOverlayMod.Scaled {
	@Unique
	private float newgen$scale = 1f;

	@Override
	public float newgen$uhcScale() {
		return newgen$scale;
	}

	@Override
	public void newgen$setUhcScale(float scale) {
		newgen$scale = scale;
	}
}

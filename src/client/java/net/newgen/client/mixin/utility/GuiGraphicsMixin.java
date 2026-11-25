package net.newgen.client.mixin.utility;

import net.minecraft.client.gui.GuiGraphics;
import net.newgen.client.mods.utility.ScrollableTooltipsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
	/** Shifts the tooltip after vanilla placed it; vanilla pops the matrix itself when it is done. */
	@Inject(
			method = "renderTooltip",
			at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;pushMatrix()Lorg/joml/Matrix3x2fStack;", shift = At.Shift.AFTER, remap = false))
	private void newgen$scrollTooltip(CallbackInfo ci) {
		int[] offset = ScrollableTooltipsMod.offset();
		if (offset != null) ((GuiGraphics) (Object) this).pose().translate(offset[0], offset[1]);
	}
}

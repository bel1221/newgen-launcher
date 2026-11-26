package net.newgen.client.mixin.utility;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.newgen.client.mods.utility.InventoryTweaksMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Wave 3 utility 1
/** Inventory Tweaks: the dark layer behind container screens, at the chosen opacity. */
@Mixin(Screen.class)
public class ContainerDimMixin {
	@Inject(method = "renderTransparentBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
	private void newgen$containerDim(GuiGraphics g, CallbackInfo ci) {
		if (!((Object) this instanceof AbstractContainerScreen<?>)) return;
		int[] dim = InventoryTweaksMod.dim();
		if (dim == null) return;
		g.fillGradient(0, 0, g.guiWidth(), g.guiHeight(), dim[0], dim[1]);
		ci.cancel();
	}
}

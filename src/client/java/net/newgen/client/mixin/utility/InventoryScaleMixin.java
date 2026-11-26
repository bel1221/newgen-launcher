package net.newgen.client.mixin.utility;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.newgen.client.mods.utility.InventoryTweaksMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Wave 3 utility 1
/** Inventory Tweaks: container screens get their own GUI scale; the normal one comes back when they close. */
@Mixin(Minecraft.class)
public class InventoryScaleMixin {
	@ModifyExpressionValue(method = "resizeDisplay()V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;calculateScale(IZ)I"))
	private int newgen$inventoryScale(int scale) {
		return InventoryTweaksMod.guiScale((Minecraft) (Object) this, scale);
	}

	@Inject(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("TAIL"))
	private void newgen$afterSetScreen(Screen screen, CallbackInfo ci) {
		InventoryTweaksMod.afterSetScreen((Minecraft) (Object) this);
	}
}

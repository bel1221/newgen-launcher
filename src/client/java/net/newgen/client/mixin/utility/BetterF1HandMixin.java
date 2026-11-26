package net.newgen.client.mixin.utility;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.GameRenderer;
import net.newgen.client.mods.utility.BetterF1Mod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Better F1: whether the first-person hand is drawn (vanilla hides it while hideGui is on). */
@Mixin(GameRenderer.class)
public class BetterF1HandMixin {
	@ModifyExpressionValue(method = "renderItemInHand", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;hideGui:Z"))
	private boolean newgen$hand(boolean hideGui) {
		return BetterF1Mod.hideHand(hideGui);
	}
}
